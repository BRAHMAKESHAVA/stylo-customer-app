//package org.backend.service;
//
//import com.google.firebase.messaging.FirebaseMessagingException;
//import com.google.firebase.messaging.MessagingErrorCode;
//import lombok.RequiredArgsConstructor;
//import org.backend.dto.booking.BookingResponseDTO;
//import org.backend.dto.partner.BookingApprovalResponse;
//import org.backend.model.NotificationDevice;
//import org.backend.repository.NotificationDeviceRepository;
//import org.springframework.stereotype.Service;
//
//import java.util.List;
//
//@Service
//@RequiredArgsConstructor
//public class NotificationService {
//
//    private final NotificationDeviceRepository repository;
//    private final PushNotificationService pushNotificationService;
//
//    public void sendToPartner(Long partnerId, String title, String body) {
//        System.out.println("Sending notification to userId = " + partnerId);
//        List<NotificationDevice> devices = repository.findByUserIdAndActiveTrue(partnerId);
//        System.out.println("Found devices = " + devices.size());
//
//        for (NotificationDevice device : devices) {
//            System.out.println("FCM Token = " + device.getFcmToken());
//
//            try {
//                pushNotificationService.sendNotification(device.getFcmToken(), title, body);
//
//                System.out.println("FCM Sent Successfully");
//
//            } catch (FirebaseMessagingException ex) {
//                if (ex.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED) {
//                    device.setActive(false);
//                    repository.save(device);
//                }
//                // Optionally log other Firebase errors here
//
//            } catch (Exception e) {
//                // Replace with proper logging in production
//                System.err.println("Failed to send notification to device " + device.getId() + ": " + e.getMessage());
//                e.printStackTrace();
//            }
//        }
//    }
//
//    // New method to send booking approval notifications
//    public void sendBooingApprovalToPartner(Long customerId, BookingApprovalResponse approval) {
//
//        System.out.println("Partner Id = " + customerId);
//        // Get all active devices for the partner
//        List<NotificationDevice> devices = repository.findByUserIdAndActiveTrue(customerId);
//        System.out.println("Devices Found = " + devices.size());
//
//        // Try sending the approval notification to each device
//        for (NotificationDevice device : devices) {
//            try {
//                pushNotificationService.sendBookingApproval(device.getFcmToken(), approval);
//            } catch (Exception e) {
//                // Log the error so we don’t lose track of failures
//                e.printStackTrace();
//            }
//        }
//    }
//
//    public void sendBookingCreatedToCustomer(Long customerId, BookingResponseDTO booking) {
//        System.out.println("Sending booking notification to customer: " + customerId);
//
//        List<NotificationDevice> devices = repository.findByUserIdAndActiveTrue(customerId);
//
//        for (NotificationDevice device : devices) {
//            try {
//                pushNotificationService.sendBookingCreated(device.getFcmToken(), booking);
//            } catch (Exception ex) {
//                System.err.println("Failed to send notification to device " + device.getId() +
//                        " for customer " + customerId);
//                ex.printStackTrace();
//            }
//        }
//    }
//
//
//}
