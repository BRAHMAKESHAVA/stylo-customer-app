package org.backend.dto.partner;

import com.fasterxml.jackson.annotation.JsonInclude;
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

public class PartnerBookingPendingResponseDTO {

    private Long bookingId;

    private Long customerId;

    private Long salonId;

    private List<String> services;

    private String packageName;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private String paymentMode;

    private String paymentStatus;

    private BigDecimal finalAmount;

    private String bookingStatus;
}