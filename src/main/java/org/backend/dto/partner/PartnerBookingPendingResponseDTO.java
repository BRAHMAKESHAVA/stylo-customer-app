package org.backend.dto.partner;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(
        name = "Partner Booking Pending Response",
        description = "DTO representing a pending booking for a partner, including customer, salon, services, timings, payment, and status details."
)
public class PartnerBookingPendingResponseDTO {

    @Schema(description = "Unique identifier of the booking", example = "1001")
    private Long bookingId;

    @Schema(description = "Unique identifier of the customer", example = "2001")
    private Long customerId;

    @Schema(description = "Unique identifier of the salon", example = "101")
    private Long salonId;

    @Schema(description = "List of services included in the booking", example = "[\"Haircut\", \"Facial\", \"Massage\"]")
    private List<String> services;

    @Schema(description = "Name of the package booked", example = "Luxury Spa Package")
    private String packageName;

    @Schema(description = "Booking start time", example = "2026-06-04T10:00:00")
    private LocalDateTime startTime;

    @Schema(description = "Booking end time", example = "2026-06-04T11:30:00")
    private LocalDateTime endTime;

    @Schema(description = "Payment mode selected by the customer", example = "ONLINE")
    private String paymentMode;

    @Schema(description = "Payment status of the booking", example = "PENDING")
    private String paymentStatus;

    @Schema(description = "Final payable amount after deductions", example = "2499.99")
    private BigDecimal finalAmount;

    @Schema(description = "Current booking status", example = "PENDING_CONFIRMATION")
    private String bookingStatus;
}
