package org.backend.dto.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(
        name = "Create Package Request",
        description = "DTO used for creating a new salon package with name, description, price, and associated services."
)
public class CreatePackageRequestDTO {

    @Schema(description = "Unique salon ID", example = "101", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Salon ID is required")
    @Positive(message = "Salon ID must be greater than 0")
    private Long salonId;

    @Schema(description = "Package name", example = "Premium Hair Care", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Package name is required")
    @Size(max = 100, message = "Package name cannot exceed 100 characters")
    private String packageName;

    @Schema(description = "Package description", example = "Includes haircut, hair spa, and styling")
    @Size(max = 255, message = "Description cannot exceed 255 characters")
    private String description;

    @Schema(description = "Package price", example = "1499.99", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Package price is required")
    @DecimalMin(value = "0.01", message = "Package price must be greater than 0")
    @Digits(integer = 8, fraction = 2,
            message = "Package price can have up to 8 digits before decimal and 2 after decimal")
    private BigDecimal packagePrice;

    @Schema(description = "List of service IDs included in the package", example = "[201, 202, 203]", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "At least one service must be selected")
    private List<
            @NotNull(message = "Service ID cannot be null")
            @Positive(message = "Service ID must be greater than 0")
                    Long
            > serviceIds;
}
