package org.backend.dto.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(
        name = "Update Service Request",
        description = "DTO used for partially updating salon service details"
)
public class UpdateSalonServiceRequest {

    @Schema(
            description = "Unique salon ID",
            example = "1"
    )
    private Long salonId;

    @Schema(
            description = "Unique service category ID",
            example = "2"
    )
    private Long categoryId;

    @Schema(
            description = "Salon service name",
            example = "Advanced Hair Styling"
    )
    @Size(
            max = 100,
            message = "Service name must be less than 100 characters"
    )
    private String serviceName;

    @Schema(
            description = "Updated service duration in minutes",
            example = "60"
    )
    @Positive(message = "Duration must be greater than 0")
    private Integer durationMinutes;

    @Schema(
            description = "Updated buffer time after service completion in minutes",
            example = "15"
    )
    @PositiveOrZero(message = "Buffer minutes cannot be negative")
    private Integer bufferMinutes;

    @Schema(
            description = "Updated service price",
            example = "799.99"
    )
    @DecimalMin(
            value = "0.0",
            inclusive = false,
            message = "Price must be greater than 0"
    )
    @Digits(
            integer = 8,
            fraction = 2,
            message = "Price must be valid (max 8 digits and 2 decimal places)"
    )
    private BigDecimal price;

    @Schema(
            description = "Status of salon service",
            example = "true"
    )
    private Boolean isActive;
}