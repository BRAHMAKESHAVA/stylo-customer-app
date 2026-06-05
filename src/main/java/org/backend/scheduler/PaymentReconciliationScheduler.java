package org.backend.scheduler;

import com.razorpay.RazorpayClient;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.backend.enums.BookingStatus;
import org.backend.enums.PaymentStatus;
import org.backend.model.Booking;
import org.backend.model.Payment;
import org.backend.repository.BookingRepository;
import org.backend.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentReconciliationScheduler {

    private final PaymentRepository paymentRepo;
    private final BookingRepository bookingRepo;

    @Value("${razorpay.key.id}")
    private String keyId;

    @Value("${razorpay.key.secret}")
    private String keySecret;

    @Value("${app.booking.payment-hold-minutes}")
    private int paymentHoldMinutes;

    @Scheduled(fixedRate = 30000) // every 30 seconds
    @Transactional
    public void reconcileInitiatedPayments() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cutoffTime = now.minusMinutes(paymentHoldMinutes); // configurable timeout

        System.out.println("Running payment reconciliation at " + now + ", checking for payments before " + cutoffTime);
        List<Payment> payments = paymentRepo.findByStatusAndCreatedDateBefore(
                PaymentStatus.INITIATED.name(),
                cutoffTime);

        if (payments.isEmpty()) return;

        try {
            RazorpayClient client = new RazorpayClient(keyId, keySecret);

            for (Payment payment : payments) {
                try {
                    // Skip if status already changed
                    if (!PaymentStatus.INITIATED.name().equals(payment.getStatus())) {
                        continue;
                    }

                    // Protect against null orderId
                    if (payment.getProviderOrderId() == null) {
                        payment.setStatus(PaymentStatus.FAILED.name());
                        payment.setUpdatedDate(now);
                        paymentRepo.save(payment);

                        Booking booking = bookingRepo.findById(payment.getBookingId()).orElse(null);
                        if (booking != null &&
                                BookingStatus.PAYMENT_PENDING.name().equals(booking.getStatus())) {
                            booking.setStatus(BookingStatus.PAYMENT_FAILED.name());
                            booking.setUpdatedDate(now);
                            bookingRepo.save(booking);
                        }

                        log.warn("Payment had no providerOrderId, marked as FAILED. bookingId={}", payment.getBookingId());
                        continue;
                    }

                    List<com.razorpay.Payment> razorpayPayments = client.orders.fetchPayments(payment.getProviderOrderId());

                    boolean success = false;
                    for (com.razorpay.Payment rpPayment : razorpayPayments) {
                        String status = rpPayment.get("status");

                        if ("captured".equalsIgnoreCase(status)) { // || "authorized".equalsIgnoreCase(status)
                            success = true;

                            String method = rpPayment.has("method") ? rpPayment.get("method").toString() : null;

                            payment.setStatus(PaymentStatus.SUCCESS.name());
                            payment.setProviderPaymentId(rpPayment.get("id").toString());
                            payment.setPaymentMethod(method);
                            payment.setUpdatedDate(now);
                            paymentRepo.save(payment);

                            Booking booking = bookingRepo.findById(payment.getBookingId()).orElse(null);
                            if (booking != null &&
                                    BookingStatus.PAYMENT_PENDING.name().equals(booking.getStatus())) {
                                booking.setStatus(BookingStatus.PENDING_PARTNER_CONFIRMATION.name());
                                booking.setUpdatedDate(now);
                                bookingRepo.save(booking);
                            }

                            log.info("Payment reconciled successfully. bookingId={}", payment.getBookingId());
                            break;
                        }
                    }

                    if (!success) {
                        payment.setStatus(PaymentStatus.FAILED.name());
                        payment.setUpdatedDate(now);
                        paymentRepo.save(payment);

                        Booking booking = bookingRepo.findById(payment.getBookingId()).orElse(null);
                        if (booking != null &&
                                BookingStatus.PAYMENT_PENDING.name().equals(booking.getStatus())) {
                            booking.setStatus(BookingStatus.PAYMENT_FAILED.name());
                            booking.setUpdatedDate(now);
                            bookingRepo.save(booking);
                        }

                        log.info("Payment expired. bookingId={}", payment.getBookingId());
                    }

                } catch (Exception ex) {
                    log.error("Error reconciling paymentId={}", payment.getPaymentId(), ex);
                }
            }

        } catch (Exception e) {
            log.error("Unable to initialize Razorpay client", e);
        }
    }
}
