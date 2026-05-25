package org.backend.dto.partner;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)

public class PartnerBookingStatusResponseDTO {

    private Long bookingId;

    private String bookingStatus;

    private String rejectionReason;

    private String paymentStatus;

    private BigDecimal refundAmount;

    private String message;
}