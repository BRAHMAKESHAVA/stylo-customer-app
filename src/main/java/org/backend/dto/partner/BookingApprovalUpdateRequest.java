package org.backend.dto.partner;

import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingApprovalUpdateRequest {

    @Positive(message = "Customer ID must be greater than 0")
    private Long customerId;

    @Positive(message = "Booking ID must be greater than 0")
    private Long bookingId;

    @Positive(message = "Service duration must be greater than 0")
    private Integer serviceDuration;

    private LocalDate slotDate;

    private LocalTime slotStartTime;

    private LocalTime slotEndTime;

    private LocalTime workingEndTime;

    @Size(max = 50, message = "Approval status cannot exceed 50 characters")
    private String approvalStatus;

    @Size(max = 500, message = "Remarks cannot exceed 500 characters")
    private String remarks;
}