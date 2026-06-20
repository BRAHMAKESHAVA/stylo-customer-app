package org.backend.dto;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Data
@Builder
@Getter @Setter
public class ServiceInfoDTO {
    private Long serviceId;
    private String serviceName;
    private BigDecimal price;
    private Integer durationMinutes;
}