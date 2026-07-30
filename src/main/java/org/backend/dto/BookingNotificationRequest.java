package org.backend.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class BookingNotificationRequest {

    private Long userId;

    private UUID bookingId;

    private String status;

    private String amount;

}