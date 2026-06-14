package org.backend.dto.partner;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PartnerBookingStatusResponseDTO {

    @Schema(description = "Unique identifier of the booking", example = "12345")
    private UUID bookingId;

    @Schema(description = "Current status of the booking", example = "CONFIRMED")
    private String bookingStatus;

    @Schema(description = "Reason for rejection if booking was declined", example = "Payment not received")
    private String rejectionReason;

    @Schema(description = "Payment status of the booking", example = "PAID")
    private String paymentStatus;

    @Schema(description = "Refund amount if applicable", example = "250.00")
    private BigDecimal refundAmount;

    @Schema(description = "Additional message or notes for the partner", example = "Booking confirmed successfully")
    private String message;
}
