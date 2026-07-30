package org.backend.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.extern.slf4j.Slf4j;
import org.backend.dto.NotificationEvent;
import org.springframework.stereotype.Service;

/**
 * Service for sending push notifications via Firebase Cloud Messaging (FCM).
 */
@Service
@Slf4j
public class PushNotificationService {

    /**
     * Sends a notification event to a specific device token.
     *
     * @param token the FCM device token
     * @param event the notification event to send
     * @return the Firebase message ID
     * @throws Exception if sending fails
     */
    public String send(String token, NotificationEvent event) throws Exception {

        Message.Builder builder = Message.builder()
                .setToken(token)
                .setNotification(
                        Notification.builder()
                                .setTitle(event.getTitle())
                                .setBody(event.getBody())
                                .build()
                );

        if (event.getData() != null) {
            event.getData().forEach(builder::putData);
        }

        Message message = builder.build();
        log.info("Sending notification of type={} to token={}",  event.getData().get("type"), token);

        return FirebaseMessaging.getInstance().send(message);
    }
}
