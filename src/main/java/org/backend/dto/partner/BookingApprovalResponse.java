package org.backend.dto.partner;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;

@Data
@Builder
public class BookingApprovalResponse {

    private Long approvalId;
    private Long customerId;
    private Long bookingId;
    private Integer serviceDuration;
    private LocalDate slotDate;
    private LocalTime slotStartTime;
    private LocalTime slotEndTime;
    private LocalTime workingEndTime;
    private String approvalStatus;
    private String remarks;
    private LocalDateTime createdAt;
}