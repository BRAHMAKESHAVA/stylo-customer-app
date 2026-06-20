package org.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PackageRecommendationDTO {

    private Long currentPackageId;
    private String currentPackageName;
    private BigDecimal currentPackagePrice;

    private Long suggestedPackageId;
    private String suggestedPackageName;
    private BigDecimal suggestedPackagePrice;

    private BigDecimal priceDifference;

    private Integer additionalServiceCount;
    private List<String> additionalServices;
}
