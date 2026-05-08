package org.backend.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(
        name = "Salon Service Response DTO",
        description = "DTO used for returning salon service details"
)
public class SalonServiceResponse {

    @Schema(description = "Unique service ID", example = "1")
    private Long serviceId;

    @Schema(description = "Unique salon ID", example = "1")
    private Long salonId;

    @Schema(description = "Unique category ID", example = "2")
    private Long categoryId;

    @Schema(description = "Service category name", example = "Hair Services")
    private String categoryName;

    @Schema(description = "Salon service name", example = "Hair Cut")
    private String serviceName;

    @Schema(description = "Service duration in minutes", example = "45")
    private Integer durationMinutes;

    @Schema(description = "Buffer time in minutes", example = "10")
    private Integer bufferMinutes;

    @Schema(description = "Service price", example = "499.99")
    private BigDecimal price;

    @Schema(description = "Service active status", example = "true")
    private Boolean isActive;
}