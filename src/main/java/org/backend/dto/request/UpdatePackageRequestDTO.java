package org.backend.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class UpdatePackageRequestDTO {

    private String packageName;

    private String description;

    private BigDecimal packagePrice;

    private Boolean isActive;

    private List<Long> serviceIds;
}