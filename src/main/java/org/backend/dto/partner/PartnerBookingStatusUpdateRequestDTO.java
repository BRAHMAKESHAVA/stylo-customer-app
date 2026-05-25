package org.backend.dto.partner;

import lombok.Data;

@Data
public class PartnerBookingStatusUpdateRequestDTO {

    private Long salonId;

    private String status;   // CONFIRMED / REJECTED

    private String reason;   // mandatory for REJECTED
}