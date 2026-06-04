package org.backend.dto.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(
        name = "Update Package Request",
        description = "DTO used for updating an existing salon package. All fields are optional; only provided values will be updated."
)
public class UpdatePackageRequestDTO {

    @Schema(description = "Updated package name", example = "Luxury Spa Package")
    @Size(max = 100, message = "Package name cannot exceed 100 characters")
    private String packageName;

    @Schema(description = "Updated package description", example = "Includes full body massage and facial")
    @Size(max = 255, message = "Description cannot exceed 255 characters")
    private String description;

    @Schema(description = "Updated package price", example = "2499.99")
    @Digits(integer = 8, fraction = 2,
            message = "Package price can have up to 8 digits before decimal and 2 digits after decimal")
    @Positive(message = "Package price must be greater than 0")
    private BigDecimal packagePrice;

    @Schema(description = "Marks whether the package is active", example = "true")
    private Boolean isActive;

    @Schema(description = "Updated list of service IDs included in the package", example = "[301, 302, 303]")
    private List<
            @Positive(message = "Service ID must be greater than 0")
                    Long
            > serviceIds;
}