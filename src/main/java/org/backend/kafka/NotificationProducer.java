package org.backend.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.backend.appConfig.KafkaTopics;
import org.backend.dto.NotificationEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Producer component responsible for publishing NotificationEvent messages to Kafka.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class  NotificationProducer {

    private final KafkaTemplate<String, NotificationEvent> kafkaTemplate;


    /**
     * Publishes a notification event to the Kafka topic.
     *
     * @param event the notification event to send
     */
    public void publish(NotificationEvent event) {
        log.info("Publishing notification event of type: {}", event.toString());

        kafkaTemplate.send(
                KafkaTopics.NOTIFICATION_EVENTS,
                event.getUserId().toString(),
                event
        );
    }
}
