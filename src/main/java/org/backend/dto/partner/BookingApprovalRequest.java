package org.backend.dto.partner;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class BookingApprovalRequest {

    @NotNull
    private Long customerId;

    @NotNull
    private Integer serviceDuration;

    @NotNull
    private LocalDate slotDate;

    @NotNull
    private LocalTime slotStartTime;

    @NotNull
    private LocalTime slotEndTime;

    @NotNull
    private LocalTime workingEndTime;
}