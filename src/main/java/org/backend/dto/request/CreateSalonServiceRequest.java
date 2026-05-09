package org.backend.dto.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(
        name = "Create Salon Service Request",
        description = "DTO used for creating salon services"
)
public class CreateSalonServiceRequest {

    @Schema(description = "Unique salon ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Salon ID is required")
    private Long salonId;

    @Schema(description = "Unique service category ID", example = "2", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Category ID is required")
    private Long categoryId;

    @Schema(description = "Salon service name", example = "Hair Cut", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Service name cannot be empty")
    @Size(max = 100, message = "Service name must be less than 100 characters")
    private String serviceName;

    @Schema(description = "Service duration in minutes", example = "45", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Duration is required")
    @Positive(message = "Duration must be greater than 0")
    private Integer durationMinutes;

    @Schema(description = "Buffer time after service completion in minutes", example = "10", defaultValue = "0")
    @PositiveOrZero(message = "Buffer minutes cannot be negative")
    private Integer bufferMinutes = 0;

    @Schema(description = "Service price", example = "499.99", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    @Digits(integer = 8, fraction = 2, message = "Price must be valid (max 8 digits and 2 decimal places)")
    private BigDecimal price;

    @Schema(description = "Status of salon service", example = "true", defaultValue = "true")
    @Builder.Default
    private Boolean isActive = true;
}