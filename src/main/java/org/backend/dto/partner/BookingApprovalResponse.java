package org.backend.dto.partner;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class BookingApprovalResponse {

    private Long approvalId;
    private Long customerId;
    private UUID bookingId;
    private Integer serviceDuration;
    private LocalDate slotDate;
    private LocalTime slotStartTime;
    private LocalTime slotEndTime;
    private LocalTime workingEndTime;
    private String approvalStatus;
    private String remarks;
    private LocalDateTime createdAt;
}