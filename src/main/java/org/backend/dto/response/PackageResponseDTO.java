package org.backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(
        name = "Package Response",
        description = "DTO representing the details of a salon package including its services."
)
public class PackageResponseDTO {

    @Schema(description = "Unique identifier of the package", example = "501")
    private Long packageId;

    @Schema(description = "Unique identifier of the salon", example = "101")
    private Long salonId;

    @Schema(description = "Name of the package", example = "Premium Hair Care")
    private String packageName;

    @Schema(description = "Description of the package", example = "Includes haircut, hair spa, and styling")
    private String description;

    @Schema(description = "Price of the package", example = "1499.99")
    private BigDecimal packagePrice;

    @Schema(description = "Indicates whether the package is active", example = "true")
    private Boolean isActive;

    @Schema(description = "List of services included in the package")
    private List<ServiceInfoDTO> services;
}
