package org.backend.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class PackageResponseDTO {

    private Long packageId;
    private Long salonId;
    private String packageName;
    private String description;
    private BigDecimal packagePrice;
    private Boolean isActive;
    private List<ServiceInfoDTO> services;
}