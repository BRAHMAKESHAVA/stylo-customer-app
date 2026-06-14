package org.backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.backend.dto.request.SaveFcmTokenRequest;
import org.backend.service.NotificationDeviceService;
import org.backend.service.PushNotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationDeviceService notificationDeviceService;
    private final PushNotificationService pushNotificationService;


    @PostMapping("/fcm-token")
    public ResponseEntity<String> saveFcmToken(@Valid @RequestBody SaveFcmTokenRequest request) {
        notificationDeviceService.saveToken(request);
        return ResponseEntity.ok("FCM token saved successfully");
    }

    /**
     * Test API to send push notification directly to a token
     */
    @PostMapping("/send")
    public ResponseEntity<String> sendNotification(
            @RequestParam String token,
            @RequestParam String title,
            @RequestParam String body) throws Exception {

        String response = pushNotificationService.sendNotification(
                token,
                title,
                body
        );

        return ResponseEntity.ok(
                "Notification sent successfully."
                        + "\nMessageId: " + response
        );
    }
}
