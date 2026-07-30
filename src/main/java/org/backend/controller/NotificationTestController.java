package org.backend.controller;

import lombok.RequiredArgsConstructor;
import org.backend.dto.BookingNotificationRequest;
import org.backend.dto.NotificationEvent;
import org.backend.dto.PaymentNotificationRequest;
import org.backend.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Test controller for triggering notification events manually.
 * Provides endpoints to simulate booking and payment notifications.
 */
@RestController
@RequestMapping("/api/test/notification")
@RequiredArgsConstructor
public class NotificationTestController {

    private final NotificationService notificationService;

    /**
     * Endpoint to publish a booking notification.
     */
    @PostMapping("/booking")
    public ResponseEntity<Map<String, Object>> booking(
            @RequestBody BookingNotificationRequest request) {

        NotificationEvent event = notificationService.sendBookingCreated(
                request.getUserId(),
                request.getBookingId(),
                request.getStatus(),
                request.getAmount()
        );

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", "Booking notification published successfully.");
        response.put("log", String.format(
                "Booking notification published for customerId=%d, bookingId=%s",
                request.getUserId(),
                request.getBookingId()
        ));
        response.put("event", event);

        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint to publish a payment notification.
     */
    @PostMapping("/payment")
    public ResponseEntity<String> payment(@RequestBody PaymentNotificationRequest request) {
        notificationService.sendPaymentSuccess(
                request.getUserId(),
                request.getPaymentId(),
                request.getAmount()
        );

        return ResponseEntity.ok("Payment notification published successfully.");
    }
}
