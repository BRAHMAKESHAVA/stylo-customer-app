package org.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.backend.dto.NotificationEvent;
import org.backend.dto.NotificationPayload;
import org.backend.enums.NotificationPriority;
import org.backend.enums.NotificationType;
import org.backend.kafka.NotificationProducer;
import org.backend.model.*;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

/**
 * Service layer for building and publishing notification events.
 * Encapsulates common notification scenarios like booking and payment.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationProducer producer;

    /**
     * Publishes a generic notification event.
     */
    public void publish(NotificationEvent event) {
        producer.publish(event);
    }

    /**
     * Sends a "Booking Created" notification to the customer.
     */
    public NotificationEvent  sendBookingCreated(Long customerId, UUID bookingId, String status, String amount) {

        NotificationEvent event = NotificationEvent.builder()
                .userId(customerId)
                .title("Booking Confirmed 🎉")
                .body("Your booking is confirmed. Please pay ₹" + amount + " at the salon.")
                .data(Map.of(
                        "type", NotificationType.BOOKING_CREATED.name(),
                        "bookingId", bookingId.toString(),
                        "status", status,
                        "amount", amount,
                        "deepLink", "stylo://order-details/"+bookingId.toString()
                ))
                .build();

        producer.publish(event);
        log.info("Booking notification published for customerId={}, bookingId={}", customerId, bookingId);
        return event;
    }

    public void sendBookingConfirmedAfterPayment(
            Booking booking,
            Payment payment) {

        NotificationEvent event = NotificationEvent.builder()
                .userId(booking.getCustomerId())
                .title("🎉 Booking Confirmed")
                .body("Your payment of ₹" + booking.getFinalAmount()
                        + " was successful. Your booking has been confirmed.")
                .data(Map.of(
                        "type", NotificationType.BOOKING_CREATED.name(),
                        "bookingId", booking.getBookingId().toString(),
                        "paymentId", payment.getPaymentId().toString(),
                        "amount", booking.getFinalAmount().toString(),
                        "status", booking.getStatus(),
                        "deepLink", "stylo://order-details/"+ booking.getBookingId().toString()
                ))
                .build();

        producer.publish(event);
    }

    /**
     * Sends a "Payment Success" notification to the customer.
     */
    public void sendPaymentSuccess(Long customerId, Long paymentId, String amount) {
        NotificationEvent event = NotificationEvent.builder()
                .userId(customerId)
                .title("Payment Successful")
                .body("Payment completed successfully.")
                .data(Map.of(
                        "type", NotificationType.PAYMENT_SUCCESS.name(),
                        "paymentId", paymentId.toString(),
                        "amount", amount,
                        "deepLink", "stylo://bookings"                ))
                .build();

        producer.publish(event);
        log.info("Payment notification published for customerId={}, paymentId={}", customerId, paymentId);
    }
}
