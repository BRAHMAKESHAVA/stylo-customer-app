package org.backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.backend.dto.LogoutRequest;
import org.backend.dto.common.ApiResponseDTO;
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

    @PostMapping("/device/deactivate")
    public ResponseEntity<ApiResponseDTO<Void>> logout(
            @Valid @RequestBody LogoutRequest request) {

        notificationDeviceService.logoutDevice(request);

        return ResponseEntity.ok(
                ApiResponseDTO.<Void>builder()
                        .status(true)
                        .message("Logged out successfully.")
                        .build()
        );
    }

}
