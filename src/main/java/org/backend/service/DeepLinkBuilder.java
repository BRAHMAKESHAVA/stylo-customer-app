package org.backend.service;

import org.springframework.stereotype.Component;

/**
 * Utility component for building deep links used in the Stylo app.
 * Each method generates a URI pointing to a specific feature or resource.
 */
@Component
public class DeepLinkBuilder {

    private static final String BASE_URI = "stylo://";

    public String booking(Long bookingId) {
        return BASE_URI + "booking/" + bookingId;
    }

    public String payment(Long paymentId) {
        return BASE_URI + "payment/" + paymentId;
    }

    public String offer(Long offerId) {
        return BASE_URI + "offer/" + offerId;
    }

    public String wallet() {
        return BASE_URI + "wallet";
    }

    public String festival() {
        return BASE_URI + "festival";
    }

    public String profile() {
        return BASE_URI + "profile";
    }

    public String chat(Long chatId) {
        return BASE_URI + "chat/" + chatId;
    }
}
