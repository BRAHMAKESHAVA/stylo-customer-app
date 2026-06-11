package org.backend.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ServiceInfoDTO {
    private Long serviceId;
    private String serviceName;
    private BigDecimal price;
    private Integer durationMinutes;
}