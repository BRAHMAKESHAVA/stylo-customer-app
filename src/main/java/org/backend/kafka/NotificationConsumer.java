package org.backend.kafka;

import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.backend.appConfig.KafkaTopics;
import org.backend.dto.NotificationEvent;
import org.backend.model.NotificationDevice;
import org.backend.repository.NotificationDeviceRepository;
import org.backend.service.PushNotificationService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Kafka consumer that listens for NotificationEvent messages
 * and dispatches them to user devices via Firebase Cloud Messaging.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationConsumer {

    private final NotificationDeviceRepository repository;
    private final PushNotificationService pushNotificationService;

    @KafkaListener(topics = KafkaTopics.NOTIFICATION_EVENTS, groupId = "notification-group")
    public void consume(NotificationEvent event) {
        log.info("Received notification event of type: {}",  event.getData().get("type"));

        List<NotificationDevice> devices = repository.findByUserIdAndIsActiveTrue(event.getUserId());
        log.info("Found {} active devices for userId={}", devices.size(), event.getUserId());

        for (NotificationDevice device : devices) {
            try {
                pushNotificationService.send(device.getFcmToken(), event);
                log.debug("Notification sent to device token={}", device.getFcmToken());

            } catch (FirebaseMessagingException ex) {
                if (ex.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED) {
                    device.setIsActive(false);
                    repository.save(device);
                    log.warn("Inactive FCM token removed: {}", device.getFcmToken());
                } else {
                    log.error("Firebase messaging error for token={}", device.getFcmToken(), ex);
                }

            } catch (Exception ex) {
                log.error("Notification delivery failed for token={}", device.getFcmToken(), ex);
            }
        }
    }
}
