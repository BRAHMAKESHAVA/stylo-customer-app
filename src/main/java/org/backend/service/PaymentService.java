package org.backend.service;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Refund;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.backend.dto.RazorpayOrderResponseDTO;
import org.backend.dto.RazorpayVerifyPaymentRequestDTO;
import org.backend.dto.RefundResultDTO;
import org.backend.enums.BookingStatus;
import org.backend.enums.PaymentStatus;
import org.backend.exception.BadRequestException;
import org.backend.exception.ResourceNotFoundException;
import org.backend.model.Booking;
import org.backend.model.Payment;
import org.backend.model.PaymentRefund;
import org.backend.repository.BookingRepository;
import org.backend.repository.PaymentRefundRepository;
import org.backend.repository.PaymentRepository;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final BookingRepository bookingRepo;
    private final PaymentRepository paymentRepo;
    private final PaymentRefundRepository refundRepo;

    @Value("${app.booking.payment-hold-minutes}")
    private int paymentHoldMinutes;

    @Value("${razorpay.key-id}")
    private String keyId;

    @Value("${razorpay.key-secret}")
    private String keySecret;

    /*
     * CREATE RAZORPAY ORDER
     */
    public RazorpayOrderResponseDTO createOrder(Long bookingId) {

        try {
            Booking booking = bookingRepo.findById(bookingId)
                    .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

            Payment payment = paymentRepo.findByBookingId(bookingId)
                    .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));

            // allow Razorpay only for ONLINE bookings
            if (!"RAZORPAY".equalsIgnoreCase(payment.getProvider())) {
                throw new BadRequestException("Razorpay order allowed only for ONLINE payments");
            }

            // prevent duplicate payment after success
            if (PaymentStatus.SUCCESS.name().equals(payment.getStatus())) {
                throw new BadRequestException("Payment already completed");
            }

//            // Auto-expire abandoned payment holds older than 10 minutes
//            if (BookingStatus.PAYMENT_PENDING.name().equals(booking.getStatus())
//                    && booking.getCreatedDate().isBefore(
//                    LocalDateTime.now().minusMinutes(paymentHoldMinutes)
//            )) {
//
//                booking.setStatus(BookingStatus.PAYMENT_FAILED.name());
//                booking.setUpdatedDate(LocalDateTime.now());
//                bookingRepo.save(booking);
//
//                payment.setStatus(PaymentStatus.FAILED.name());
//                payment.setUpdatedDate(LocalDateTime.now());
//                paymentRepo.save(payment);
//
//                throw new BadRequestException("Payment session expired. Please rebook.");
//            }

            RazorpayClient client = new RazorpayClient(keyId, keySecret);

            long amountInPaise = booking.getFinalAmount()
                    .multiply(BigDecimal.valueOf(100))
                    .longValue();

            JSONObject options = new JSONObject();
            options.put("amount", amountInPaise);
            options.put("currency", "INR");
            options.put("receipt", "BK-" + bookingId);

            JSONObject notes = new JSONObject();
            notes.put("bookingId", bookingId);

            options.put("notes", notes);

            Order order = client.orders.create(options);

            // Track created Razorpay order for verification/webhook mapping
            payment.setProviderOrderId(order.get("id").toString());
            payment.setStatus(PaymentStatus.INITIATED.name());
            payment.setUpdatedDate(LocalDateTime.now());

            paymentRepo.save(payment);

            return new RazorpayOrderResponseDTO(
                    keyId,
                    order.get("id").toString(),
                    ((Number) order.get("amount")).longValue(),
                    order.get("currency").toString()
            );

        } catch (Exception e) {
            throw new RuntimeException(
                    "Error creating Razorpay order",
                    e
            );
        }
    }

    /*
     * VERIFY PAYMENT
     */
    public void verifyPayment(Long bookingId, RazorpayVerifyPaymentRequestDTO dto) throws RazorpayException {

        // Idempotency protection against duplicate verify calls
        if (paymentRepo.existsByProviderPaymentId(dto.getRazorpayPaymentId())) {
            return;
        }

        Booking booking = bookingRepo.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        Payment payment = paymentRepo.findByBookingId(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));

        if (!"RAZORPAY".equalsIgnoreCase(payment.getProvider())) {
            throw new BadRequestException("Invalid payment provider");
        }

        RazorpayClient razorpay = new RazorpayClient(keyId, keySecret);

        com.razorpay.Payment razorpayPayment = razorpay.payments.fetch(dto.getRazorpayPaymentId());

        String orderId = razorpayPayment.get("order_id").toString();

        if (!orderId.equals(dto.getRazorpayOrderId())) {
            throw new BadRequestException("Payment does not belong to provided order");
        }

        String paymentStatus = razorpayPayment.get("status");

        // Mark failed payment and release slot immediately
        if (!"captured".equalsIgnoreCase(paymentStatus)) {
            payment.setStatus(PaymentStatus.FAILED.name());
            payment.setUpdatedDate(LocalDateTime.now());
            paymentRepo.save(payment);

            booking.setStatus(BookingStatus.PAYMENT_FAILED.name());
            booking.setUpdatedDate(LocalDateTime.now());
            bookingRepo.save(booking);
            throw new BadRequestException("Payment not captured");
        }

        String payload = dto.getRazorpayOrderId() + "|" + dto.getRazorpayPaymentId();

        String expectedSignature = hmacSHA256(payload, keySecret);

        if (!MessageDigest.isEqual(
                expectedSignature.getBytes(StandardCharsets.UTF_8),
                dto.getRazorpaySignature().getBytes(StandardCharsets.UTF_8)
        )) {
            throw new BadRequestException("Signature verification failed");
        }

        long expectedAmount = booking.getFinalAmount()
                .multiply(BigDecimal.valueOf(100))
                .longValue();

        long actualAmount = Long.parseLong(razorpayPayment.get("amount").toString());

        if (expectedAmount != actualAmount) {
            throw new BadRequestException("Amount mismatch");
        }

        payment.setProviderPaymentId(dto.getRazorpayPaymentId());
        payment.setProviderSignature(dto.getRazorpaySignature());
        payment.setStatus(PaymentStatus.SUCCESS.name());
        payment.setUpdatedDate(LocalDateTime.now());

        paymentRepo.save(payment);

        booking.setStatus(BookingStatus.PENDING_PARTNER_CONFIRMATION.name());
        booking.setUpdatedDate(LocalDateTime.now());

        bookingRepo.save(booking);
    }

    /*
     * PAYMENT FAILED
     */
    public void markPaymentFailed(Long bookingId) {

        Booking booking = bookingRepo.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        Payment payment = paymentRepo.findByBookingId(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));

        // ignore already successful payments
        if (PaymentStatus.SUCCESS.name().equals(payment.getStatus())) {
            return;
        }

        payment.setStatus(PaymentStatus.FAILED.name());
        payment.setUpdatedDate(LocalDateTime.now());

        paymentRepo.save(payment);

        booking.setStatus(BookingStatus.PAYMENT_FAILED.name());
        booking.setUpdatedDate(LocalDateTime.now());

        bookingRepo.save(booking);
    }

    /*
     * REFUND PAYMENT
     */
    public RefundResultDTO refundPayment(Long bookingId, String reason) {

        try {
            Booking booking = bookingRepo.findById(bookingId)
                    .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

            Payment payment = paymentRepo.findByBookingId(bookingId)
                    .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));

            if (!PaymentStatus.SUCCESS.name().equals(payment.getStatus())) {
                throw new BadRequestException("Payment is not successful");
            }

            if (PaymentStatus.REFUND_INITIATED.name().equals(payment.getStatus())) {
                throw new BadRequestException("Refund already initiated");
            }

            if (PaymentStatus.REFUNDED.name().equals(payment.getStatus())) {
                throw new BadRequestException("Payment already refunded");
            }

            BigDecimal refundAmount = calculateRefundAmount(payment);

            if (refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BadRequestException("No refund applicable");
            }

            RazorpayClient client = new RazorpayClient(keyId, keySecret);

            JSONObject options = new JSONObject();
            options.put("payment_id", payment.getProviderPaymentId());

            long amountInPaise =
                    refundAmount.multiply(
                                    BigDecimal.valueOf(100)
                            )
                            .setScale(0, RoundingMode.DOWN)
                            .longValue();

            options.put("amount", amountInPaise);

            Refund refund = client.payments.refund(options);

            String razorpayStatus = refund.get("status").toString();

            String providerRefundId = refund.get("id").toString();

            BigDecimal actualRefund = new BigDecimal(refund.get("amount").toString()).divide(BigDecimal.valueOf(100));

            String finalStatus;

            if ("processed".equalsIgnoreCase(razorpayStatus)) {
                finalStatus = PaymentStatus.SUCCESS.name();
            } else if ("pending".equalsIgnoreCase(razorpayStatus)) {
                finalStatus = PaymentStatus.PENDING.name();
            } else {
                finalStatus = PaymentStatus.FAILED.name();
            }

            PaymentRefund paymentRefund = new PaymentRefund();
            paymentRefund.setPaymentId(payment.getPaymentId());
            paymentRefund.setRefundAmount(actualRefund);
            paymentRefund.setReason(reason);
            paymentRefund.setProviderRefundId(providerRefundId);
            paymentRefund.setStatus(finalStatus);
            paymentRefund.setCreatedDate(LocalDateTime.now());

            refundRepo.save(paymentRefund);

            payment.setStatus(PaymentStatus.REFUNDED.name());
            payment.setUpdatedDate(LocalDateTime.now());

            paymentRepo.save(payment);

            // booking remains REJECTED from partner flow, not CANCELLED
            //booking.setStatus(BookingStatus.CANCELLED.name());
            //booking.setUpdatedDate(LocalDateTime.now());

            //bookingRepo.save(booking);

            return RefundResultDTO.builder()
                    .refundAmount(actualRefund)
                    .paymentStatus(payment.getStatus())
                    .providerRefundId(providerRefundId)
                    .build();

        } catch (Exception e) {
            throw new RuntimeException(
                    "Refund failed: " + e.getMessage(),
                    e
            );
        }
    }

    /*
     * WEBHOOK HANDLER
     */
    public void handleWebhook(
            String payload,
            String signature
    ) {

        log.info("Received Razorpay webhook");

        JSONObject event = new JSONObject(payload);
        String eventType = event.getString("event");

        JSONObject paymentEntity = event
                .getJSONObject("payload")
                .getJSONObject("payment")
                .getJSONObject("entity");

        String orderId = paymentEntity.optString("order_id");
        String paymentId = paymentEntity.optString("id");

        Payment payment = paymentRepo.findByProviderOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));

        Booking booking = bookingRepo.findById(payment.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        if (PaymentStatus.SUCCESS.name()
                .equals(payment.getStatus())) {
            return;
        }

        // webhook success finalizes online booking for partner approval
        if ("payment.captured".equals(eventType)) {

            payment.setProviderPaymentId(paymentId);
            payment.setStatus(PaymentStatus.SUCCESS.name());
            payment.setUpdatedDate(LocalDateTime.now());

            paymentRepo.save(payment);

            booking.setStatus(BookingStatus.PENDING_PARTNER_CONFIRMATION.name());
            booking.setUpdatedDate(LocalDateTime.now());

            bookingRepo.save(booking);
        }

        // webhook failure immediately releases slot
        else if ("payment.failed".equals(eventType)) {

            payment.setProviderPaymentId(paymentId);
            payment.setStatus(PaymentStatus.FAILED.name());
            payment.setUpdatedDate(LocalDateTime.now());

            paymentRepo.save(payment);

            booking.setStatus(BookingStatus.PAYMENT_FAILED.name());
            booking.setUpdatedDate(LocalDateTime.now());
        }
    }

    /*
     * REFUND CALCULATION
     */
    private BigDecimal calculateRefundAmount(
            Payment payment
    ) {

        if (payment.getCreatedDate() == null) {
            return BigDecimal.ZERO;
        }

        long minutes =
                java.time.Duration.between(
                        payment.getCreatedDate(),
                        LocalDateTime.now()
                ).toMinutes();

        if (minutes <= 10) {
            return payment.getAmount();
        }

        if (minutes <= 60) {
            return payment.getAmount()
                    .multiply(new BigDecimal("0.5"))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        return BigDecimal.ZERO;
    }

    /*
     * REFUND PREVIEW
     */
    public Map<String, String> getRefundPreview(
            Payment payment
    ) {

        BigDecimal refund =
                calculateRefundAmount(payment);

        String tier =
                refund.compareTo(payment.getAmount()) == 0
                        ? "FULL"
                        : refund.compareTo(BigDecimal.ZERO) > 0
                        ? "PARTIAL"
                        : "NONE";

        Map<String, String> result =
                new HashMap<>();

        result.put(
                "refundAmount",
                refund.toPlainString()
        );

        result.put(
                "refundTier",
                tier
        );

        return result;
    }

    /*
     * SIGNATURE HASH
     */
    private String hmacSHA256(
            String data,
            String key
    ) {

        try {
            Mac mac = Mac.getInstance("HmacSHA256");

            mac.init(
                    new SecretKeySpec(
                            key.getBytes(),
                            "HmacSHA256"
                    )
            );

            byte[] hash =
                    mac.doFinal(data.getBytes());

            StringBuilder hex =
                    new StringBuilder();

            for (byte b : hash) {
                hex.append(
                        String.format("%02x", b)
                );
            }

            return hex.toString();

        } catch (Exception e) {
            throw new RuntimeException(
                    "Signature generation failed",
                    e
            );
        }
    }
}