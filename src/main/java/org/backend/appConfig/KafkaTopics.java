package org.backend.appConfig;

/**
 * Central place to keep all Kafka topic names.
 * Helps avoid typos and makes refactoring easier.
 */
public final class KafkaTopics {

    // Prevent instantiation
    private KafkaTopics() {}

    public static final String NOTIFICATION_EVENTS = "notification-events";
    public static final String NOTIFICATION_DEAD_LETTER = "notification-dlt";
}
