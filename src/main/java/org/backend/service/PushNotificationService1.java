//package org.backend.service;
//
//import com.google.firebase.messaging.FirebaseMessaging;
//import com.google.firebase.messaging.Message;
//import com.google.firebase.messaging.Notification;
//import org.backend.dto.booking.BookingResponseDTO;
//import org.backend.dto.partner.BookingApprovalResponse;
//import org.springframework.stereotype.Service;
//
//import java.time.Duration;
//
//@Service
//public class PushNotificationService {
//
//    public String sendNotification(String token, String title, String body) throws Exception {
//
//        if (token == null || token.isBlank()) {
//            throw new IllegalArgumentException("FCM token must not be null or empty");
//        }
//
//        Message message = Message.builder()
//                .setToken(token)
//                .setNotification(
//                        Notification.builder()
//                                .setTitle(title)
//                                .setBody(body)
//                                .build()
//                )
//                .build();
//
//        System.out.println("Sending notification to token: " + token);
//        return FirebaseMessaging.getInstance().send(message);
//    }
//
//    // New method to send booking approval notifications
//    public String sendBookingApproval(String token, BookingApprovalResponse approval) throws Exception {
//
//        long exceededMinutes = Duration
//                .between(approval.getWorkingEndTime(), approval.getSlotEndTime())
//                .toMinutes();
//
//        String body = """
//                ⚠️ Partner Approval Required
//
//                This booking exceeds salon closing time by %d minutes
//
//                Start: %s
//                End: %s
//                Closing: %s
//                """.formatted(
//                exceededMinutes,
//                approval.getSlotStartTime(),
//                approval.getSlotEndTime(),
//                approval.getWorkingEndTime()
//        );
//
//        // Build the notification message
//        Message message = Message.builder()
//                .setToken(token)
//                .setNotification(
//                        Notification.builder()
//                                .setTitle("Booking Approval Required")
//                                .setBody(body)
//                                .build()
//                )
//                .putData("type", "BOOKING_APPROVAL")
//                .putData("approvalId", approval.getApprovalId().toString())
//                .build();
//
//        // Send the message through Firebase
//        return FirebaseMessaging.getInstance().send(message);
//    }
//
//    // New method to send booking created notifications
//    public String sendBookingCreated(String token, BookingResponseDTO booking) throws Exception {
//        String body = """
//                Your booking has been confirmed.
//
//                Booking ID: %s
//                Total Amount: ₹%s
//                """.formatted(
//                booking.getBookingId(),
//                booking.getFinalAmount()
//        );
//
//        Message message = Message.builder()
//                .setToken(token)
//                .setNotification(
//                        Notification.builder()
//                                .setTitle("Booking Confirmed 🎉")
//                                .setBody(body)
//                                .build()
//                )
//                .putData("type", "BOOKING_CREATED")
//                .putData("bookingId", booking.getBookingId().toString())
//                .putData("status", booking.getStatus())
//                .build();
//
//        return FirebaseMessaging.getInstance().send(message);
//    }
//}
