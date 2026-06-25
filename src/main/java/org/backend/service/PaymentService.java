package org.backend.service;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Refund;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.backend.dto.RefundResultDTO;
import org.backend.dto.booking.BookingResponseDTO;
import org.backend.dto.request.RazorpayVerifyPaymentRequestDTO;
import org.backend.dto.response.RazorpayOrderResponseDTO;
import org.backend.enums.BookingStatus;
import org.backend.enums.PaymentMode;
import org.backend.enums.PaymentStatus;
import org.backend.exception.BadRequestException;
import org.backend.exception.PaymentGatewayException;
import org.backend.exception.ResourceNotFoundException;
import org.backend.exception.SignatureGenerationException;
import org.backend.model.Booking;
import org.backend.model.Payment;
import org.backend.model.PaymentRefund;
import org.backend.repository.BookingRepository;
import org.backend.repository.PaymentRefundRepository;
import org.backend.repository.PaymentRepository;
import org.backend.repository.SalonResourceRepository;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final BookingRepository bookingRepo;
    private final PaymentRepository paymentRepo;
    private final PaymentRefundRepository refundRepo;
    private final BookingService bookingService;
    private final SalonResourceRepository salonResourceRepo;

    @Value("${app.booking.payment-hold-minutes}")
    private int paymentHoldMinutes;

    @Value("${razorpay.key-id}")
    private String keyId;

    @Value("${razorpay.key-secret}")
    private String keySecret;

    /**
     * CONFIRM PAY AT SALON
     * Called when user clicks Proceed with Pay at Salon selected.
     * Creates payment record and moves booking to PENDING_PARTNER_CONFIRMATION.
     */
    @Transactional
    public void confirmPayAtSalon(UUID bookingId) {

        Booking booking = bookingRepo.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        // Lock the salon resource to prevent concurrent bookings for the same slot
        salonResourceRepo.lockSalonResource(booking.getSalonId());

        // Final availability check
//        if (!bookingService.checkSlotAvailable(
//                booking.getSalonId(),
//                booking.getStartTime(),
//                booking.getEndTime())) {
//
//            throw new BadRequestException(
//                    "This slot was booked by another customer. Please select a different slot.");
//        }

        if (booking.getCreatedDate()
                .isBefore(LocalDateTime.now().minusMinutes(paymentHoldMinutes))) {
            throw new BadRequestException("Booking hold has expired. Please rebook.");
        }

        Payment payment = paymentRepo.findByBookingId(bookingId)
                .orElseGet(() -> Payment.builder()
                        .bookingId(bookingId)
                        .amount(booking.getFinalAmount())
                        .currency("INR")
                        .build());

        payment.setStatus(PaymentStatus.PENDING.name());
        payment.setProvider("OFFLINE");

        paymentRepo.save(payment);

        //booking.setStatus(BookingStatus.PENDING_PARTNER_CONFIRMATION.name());
        booking.setStatus(BookingStatus.CONFIRMED.name());
        //booking.setUpdatedDate(LocalDateTime.now());
        bookingRepo.save(booking);

        // Notify customer about booking confirmation and next steps
        BookingResponseDTO bookingResponse = bookingService.buildResponse(booking);
    }

    //============START===============
    @Transactional
    public RazorpayOrderResponseDTO createOrder(UUID bookingId) {

        Booking booking = bookingRepo.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId));

        // Lock the salon resource to prevent concurrent bookings for the same slot
        salonResourceRepo.lockSalonResource(booking.getSalonId());

        // Final availability check
//        if (!bookingService.checkSlotAvailable(
//                booking.getSalonId(),
//                booking.getStartTime(),
//                booking.getEndTime())) {
//
//            throw new BadRequestException(
//                    "This slot was booked by another customer. Please select a different slot.");
//        }

        if (!BookingStatus.PAYMENT_PENDING.name().equals(booking.getStatus())) {
            throw new BadRequestException("Booking has expired. Please select a slot again.");
        }

        Payment payment = paymentRepo.findByBookingId(bookingId).orElse(null);

        if (payment != null && PaymentStatus.SUCCESS.name().equals(payment.getStatus())) {
            throw new BadRequestException("Payment already completed for booking: " + bookingId);
        }

        LocalDateTime now = LocalDateTime.now();

        try {
            RazorpayClient client = new RazorpayClient(keyId, keySecret);

            // Reuse existing INITIATED order if still valid
            if (payment != null && PaymentStatus.INITIATED.name().equals(payment.getStatus())
                    && payment.getProviderOrderId() != null) {

                List<com.razorpay.Payment> existingRazorpayPayments =
                        client.orders.fetchPayments(payment.getProviderOrderId());

                for (com.razorpay.Payment rpPayment : existingRazorpayPayments) {
                    String status = rpPayment.get("status").toString();

                    if ("captured".equalsIgnoreCase(status)) { //|| "authorized".equalsIgnoreCase(status)
                        payment.setProviderPaymentId(rpPayment.get("id").toString());
                        payment.setStatus(PaymentStatus.SUCCESS.name());
                        payment.setPaymentMethod(rpPayment.has("method") ? rpPayment.get("method").toString() : null);
                        payment.setUpdatedDate(now);
                        paymentRepo.save(payment);

                        if (BookingStatus.PAYMENT_PENDING.name().equals(booking.getStatus())) {
                            //booking.setStatus(BookingStatus.PENDING_PARTNER_CONFIRMATION.name());
                            booking.setStatus(BookingStatus.CONFIRMED.name());
                            //booking.setUpdatedDate(now);
                            bookingRepo.save(booking);
                        }

                        throw new BadRequestException("Payment already completed for booking: " + bookingId);
                    }
                }

                return new RazorpayOrderResponseDTO(
                        keyId,
                        payment.getProviderOrderId(),
                        booking.getFinalAmount().multiply(BigDecimal.valueOf(100)).longValue(),
                        "INR"
                );
            }

            // Check FAILED payments against Razorpay before creating new order
            if (payment != null && PaymentStatus.FAILED.name().equals(payment.getStatus())
                    && payment.getProviderOrderId() != null) {

                List<com.razorpay.Payment> razorpayPayments = client.orders.fetchPayments(payment.getProviderOrderId());

                for (com.razorpay.Payment rpPayment : razorpayPayments) {
                    String status = rpPayment.get("status").toString();

                    if ("captured".equalsIgnoreCase(status)) { //|| "authorized".equalsIgnoreCase(status)

                        payment.setProviderPaymentId(rpPayment.get("id").toString());
                        payment.setStatus(PaymentStatus.SUCCESS.name());
                        payment.setPaymentMethod(rpPayment.has("method") ? rpPayment.get("method").toString() : null);
                        payment.setUpdatedDate(now);
                        paymentRepo.save(payment);

                        if (BookingStatus.PAYMENT_PENDING.name().equals(booking.getStatus())) {
                            //booking.setStatus(BookingStatus.PENDING_PARTNER_CONFIRMATION.name());
                            booking.setStatus(BookingStatus.CONFIRMED.name());
                            //booking.setUpdatedDate(now);
                            bookingRepo.save(booking);
                        }

                        throw new BadRequestException("Payment already completed for booking: " + bookingId);
                    }
                }
                // If still failed → proceed to create new order
            }

            // Create new order (first time or after confirmed failure)
            long amountInPaise = booking.getFinalAmount().multiply(BigDecimal.valueOf(100)).longValue();

            JSONObject options = new JSONObject();
            options.put("amount", amountInPaise);
            options.put("currency", "INR");
            options.put("receipt", "BK-" + bookingId);

            JSONObject notes = new JSONObject();
            notes.put("bookingId", bookingId);
            options.put("notes", notes);

            Order order = client.orders.create(options);

            if (payment == null) {
                payment = Payment.builder()
                        .bookingId(bookingId)
                        .amount(booking.getFinalAmount())
                        .currency("INR")
                        .provider("RAZORPAY")
                        .status(PaymentStatus.INITIATED.name())
                        .providerOrderId(order.get("id").toString())
                        .build();
            } else {
                payment.setProvider("RAZORPAY");
                payment.setProviderOrderId(order.get("id").toString());
                payment.setProviderPaymentId(null);
                payment.setProviderSignature(null);
                payment.setStatus(PaymentStatus.INITIATED.name());
                payment.setUpdatedDate(now);
            }

            paymentRepo.save(payment);

            return new RazorpayOrderResponseDTO(
                    keyId,
                    order.get("id").toString(),
                    ((Number) order.get("amount")).longValue(),
                    order.get("currency").toString()
            );

        } catch (RazorpayException e) {
            log.error("Error creating Razorpay order | bookingId={}", bookingId, e);
            throw new PaymentGatewayException("Error creating Razorpay order for booking: " + bookingId, e);
        }
    }

    //===========END================

    /**
     * Verifies a Razorpay payment for a booking.
     * - Ensures idempotency, provider validation, signature, and amount checks.
     * - Updates booking and payment status accordingly.
     *
     * @param bookingId the booking identifier
     * @param dto the Razorpay verification request payload
     * @throws RazorpayException if Razorpay API call fails
     */
    @Transactional
    public void verifyPayment(UUID bookingId, RazorpayVerifyPaymentRequestDTO dto) throws RazorpayException {
        if (paymentRepo.existsByProviderPaymentId(dto.getRazorpayPaymentId())) {
            log.info("Payment already verified | bookingId={}, paymentId={}", bookingId, dto.getRazorpayPaymentId());
            return;
        }

        Booking booking = bookingRepo.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        Payment payment = paymentRepo.findByBookingId(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));

        if (!BookingStatus.PAYMENT_PENDING.name().equals(booking.getStatus())) {
            throw new BadRequestException("Booking has expired. Please select a slot again.");
        }

        if (!"RAZORPAY".equalsIgnoreCase(payment.getProvider())) {
            throw new BadRequestException("Invalid payment provider");
        }

        RazorpayClient razorpay = new RazorpayClient(keyId, keySecret);
        com.razorpay.Payment razorpayPayment = razorpay.payments.fetch(dto.getRazorpayPaymentId());

        String orderId = razorpayPayment.get("order_id").toString();
        String paymentStatus = razorpayPayment.get("status");

        if (!orderId.equals(dto.getRazorpayOrderId())) {
            throw new BadRequestException("Payment does not belong to provided order");
        }

        if (!"captured".equalsIgnoreCase(paymentStatus)) {
            log.warn("Payment failed | bookingId={}, status={}", bookingId, paymentStatus);
            payment.setStatus(PaymentStatus.FAILED.name());
            payment.setUpdatedDate(LocalDateTime.now());
            paymentRepo.save(payment);

            booking.setStatus(BookingStatus.PAYMENT_FAILED.name());
            booking.setUpdatedDate(LocalDateTime.now());
            bookingRepo.save(booking);

            throw new BadRequestException("Payment not captured");
        }

        // Signature verification
        String payload = dto.getRazorpayOrderId() + "|" + dto.getRazorpayPaymentId();
        String expectedSignature = hmacSHA256(payload, keySecret);
        if (!MessageDigest.isEqual(
                expectedSignature.getBytes(StandardCharsets.UTF_8),
                dto.getRazorpaySignature().getBytes(StandardCharsets.UTF_8))) {
            log.error("Signature verification failed | bookingId={}", bookingId);
            throw new BadRequestException("Signature verification failed");
        }

        // Amount verification
        long expectedAmount = booking.getFinalAmount().multiply(BigDecimal.valueOf(100)).longValue();
        long actualAmount = Long.parseLong(razorpayPayment.get("amount").toString());
        if (expectedAmount != actualAmount) {
            throw new BadRequestException("Amount mismatch");
        }

        // Mark success
        payment.setProviderPaymentId(dto.getRazorpayPaymentId());
        payment.setProviderSignature(dto.getRazorpaySignature());
        payment.setStatus(PaymentStatus.SUCCESS.name());
        payment.setPaymentMethod(razorpayPayment.get("method"));
        payment.setUpdatedDate(LocalDateTime.now());
        paymentRepo.save(payment);

        //booking.setStatus(BookingStatus.PENDING_PARTNER_CONFIRMATION.name());
        booking.setStatus(BookingStatus.CONFIRMED.name());
        //booking.setUpdatedDate(LocalDateTime.now());
        bookingRepo.save(booking);

        log.info("Payment verified successfully | bookingId={}, paymentId={}", bookingId, dto.getRazorpayPaymentId());
    }

    /**
     * Marks a booking and its payment as FAILED.
     * - Skips already successful or failed payments.
     * - Updates both Payment and Booking status atomically.
     *
     * @param bookingId the booking identifier
     */
    @Transactional
    public void markPaymentFailed(UUID bookingId) {
        Booking booking = bookingRepo.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        Payment payment = paymentRepo.findByBookingId(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));

        // Ignore already successful or failed payments
        if (PaymentStatus.SUCCESS.name().equals(payment.getStatus()) ||
                PaymentStatus.FAILED.name().equals(payment.getStatus())) {
            log.info("Payment already marked as {} | bookingId={}", payment.getStatus(), bookingId);
            return;
        }

        log.warn("Marking payment as FAILED | bookingId={}", bookingId);

        payment.setStatus(PaymentStatus.FAILED.name());
        payment.setUpdatedDate(LocalDateTime.now());
        paymentRepo.save(payment);

        booking.setStatus(BookingStatus.PAYMENT_FAILED.name());
        //booking.setUpdatedDate(LocalDateTime.now());
        bookingRepo.save(booking);
    }

    /**
     * Initiates a refund for a successful payment.
     * - Validates booking/payment and refund eligibility.
     * - Calls Razorpay API to process refund.
     * - Persists refund record and updates payment status.
     *
     * @param bookingId the booking identifier
     * @param reason reason for refund
     * @return RefundResultDTO containing refund details
     */
    @Transactional
    public RefundResultDTO refundPayment(UUID bookingId, String reason) {
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

        try {
            RazorpayClient client = new RazorpayClient(keyId, keySecret);

            long amountInPaise = refundAmount.multiply(BigDecimal.valueOf(100))
                    .setScale(0, RoundingMode.DOWN)
                    .longValue();

            JSONObject options = new JSONObject();
            options.put("payment_id", payment.getProviderPaymentId());
            options.put("amount", amountInPaise);

            Refund refund = client.payments.refund(options);

            String razorpayStatus = refund.get("status").toString();
            String providerRefundId = refund.get("id").toString();
            BigDecimal actualRefund = new BigDecimal(refund.get("amount").toString())
                    .divide(BigDecimal.valueOf(100));

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

            log.info("Refund initiated | bookingId={}, refundId={}, status={}", bookingId, providerRefundId, finalStatus);

            return RefundResultDTO.builder()
                    .refundAmount(actualRefund)
                    .paymentStatus(payment.getStatus())
                    .providerRefundId(providerRefundId)
                    .build();

        } catch (RazorpayException e) {
            log.error("Refund failed | bookingId={}", bookingId, e);
            throw new PaymentGatewayException("Refund failed", e);
        }
    }

    /**
     * Handles Razorpay webhook events.
     * - Verifies signature
     * - Updates Payment and Booking status based on event type
     *
     * @param payload   the webhook payload JSON
     * @param signature the Razorpay signature header
     */
    @Transactional
    public void handleWebhook(String payload, String signature) {
        log.info("Received Razorpay webhook");

        // Verify signature
        String expectedSignature = hmacSHA256(payload, keySecret);
        if (!MessageDigest.isEqual(
                expectedSignature.getBytes(StandardCharsets.UTF_8),
                signature.getBytes(StandardCharsets.UTF_8))) {
            log.error("Webhook signature verification failed");
            throw new BadRequestException("Invalid webhook signature");
        }

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

        if (PaymentStatus.SUCCESS.name().equals(payment.getStatus())) {
            log.info("Payment already marked SUCCESS | bookingId={}", booking.getBookingId());
            return;
        }

        switch (eventType) {
            case "payment.captured" -> {
                payment.setProviderPaymentId(paymentId);
                payment.setStatus(PaymentStatus.SUCCESS.name());
                payment.setUpdatedDate(LocalDateTime.now());
                paymentRepo.save(payment);

                booking.setStatus(BookingStatus.CONFIRMED.name());
                booking.setUpdatedDate(LocalDateTime.now());
                bookingRepo.save(booking);

                log.info("Payment captured | bookingId={} | paymentId={}", booking.getBookingId(), paymentId);
            }
            case "payment.failed" -> {
                payment.setProviderPaymentId(paymentId);
                payment.setStatus(PaymentStatus.FAILED.name());
                payment.setUpdatedDate(LocalDateTime.now());
                paymentRepo.save(payment);

                booking.setStatus(BookingStatus.PAYMENT_FAILED.name());
                booking.setUpdatedDate(LocalDateTime.now());
                bookingRepo.save(booking);

                log.warn("Payment failed | bookingId={} | paymentId={}", booking.getBookingId(), paymentId);
            }
            default -> log.info("Unhandled webhook event={} | bookingId={}", eventType, booking.getBookingId());
        }
    }

    /*
     * REFUND CALCULATION
     */
    private BigDecimal calculateRefundAmount(Payment payment) {
        if (payment.getCreatedDate() == null) {
            return BigDecimal.ZERO;
        }

        long minutes = Duration.between(payment.getCreatedDate(), LocalDateTime.now()).toMinutes();

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
    public Map<String, String> getRefundPreview(Payment payment) {
        BigDecimal refund = calculateRefundAmount(payment);

        String tier = refund.compareTo(payment.getAmount()) == 0
                ? "FULL"
                : refund.compareTo(BigDecimal.ZERO) > 0
                ? "PARTIAL"
                : "NONE";

        Map<String, String> result = new HashMap<>();
        result.put("refundAmount", refund.toPlainString());
        result.put("refundTier", tier);

        return result;
    }

    /*
     * SIGNATURE HASH
     */
    private String hmacSHA256(String data, String key) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    key.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"
            );
            mac.init(secretKeySpec);

            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));

            // Convert bytes to hex string
            StringBuilder hexString = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hexString.append(String.format("%02x", b));
            }

            return hexString.toString();
        } catch (GeneralSecurityException e) {
            throw new SignatureGenerationException("Signature generation failed", e);
        }
    }

}