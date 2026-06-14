package org.backend.service;

import lombok.RequiredArgsConstructor;
import org.backend.dto.booking.BookingResponseDTO;
import org.backend.dto.partner.BookingApprovalResponse;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * Service responsible for sending booking approval requests
 * to partner clients via WebSocket/STOMP messaging.
 */
@Service
@RequiredArgsConstructor
public class PartnerWebSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Sends a booking approval response to all subscribed partners.
     *
     * @param response the booking approval payload to be delivered
     */
    public void notifyPartner(BookingApprovalResponse response) {
        // create a topic to only specific partner only the topic url contain partner id and send the response to that topic
        // example /topic/partner/booking-approval/1
        messagingTemplate.convertAndSend("/topic/partner/booking-approval", response);
    }

    /**
     * Sends a booking approval response to a specific customer
     * over their dedicated WebSocket topic.
     *
     * @param customerId the unique identifier of the customer
     * @param response   the booking approval payload
     */
    public void notifyCustomer(Long customerId, BookingApprovalResponse response) {
        String destination = "/topic/customer/booking-approval/" + customerId;
        messagingTemplate.convertAndSend(destination, response);
    }

    /**
     * Notifies a specific customer about their booking event
     * by sending a response payload to their dedicated WebSocket topic.
     *
     * @param customerId the unique identifier of the customer
     * @param response   the booking response payload
     */
    public void notifyCustomer(Long customerId, BookingResponseDTO response) {
        String destination = "/topic/customer/bookings/" + customerId;
        messagingTemplate.convertAndSend(destination, response);
    }


}
