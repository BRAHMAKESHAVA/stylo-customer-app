package org.backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

import java.util.List;

@Getter
@Setter
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(
        name = "Salon Search With Selected Services Request",
        description = "DTO used for searching nearby salons based on location and selected services"
)
public class SalonSearchWithSelectedServicesRequest {

    @Schema(
            description = "Customer latitude coordinate",
            example = "12.912345",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "Latitude is required")
    @DecimalMin(value = "-90.0", message = "Latitude must be >= -90")
    @DecimalMax(value = "90.0", message = "Latitude must be <= 90")
    private Double latitude;

    @Schema(
            description = "Customer longitude coordinate",
            example = "77.684567",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "Longitude is required")
    @DecimalMin(value = "-180.0", message = "Longitude must be >= -180")
    @DecimalMax(value = "180.0", message = "Longitude must be <= 180")
    private Double longitude;

    @Schema(
            description = "Search radius distance",
            example = "5",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "Distance is required")
    @Positive(message = "Distance must be greater than 0")
    private Double distance;

    @Schema(
            description = "Distance measurement unit",
            example = "KM",
            allowableValues = {"KM", "M"},
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "Unit is required (KM or M)")
    @Pattern(
            regexp = "KM|M",
            message = "Unit must be KM or M"
    )
    private String unit;

    @ArraySchema(
            schema = @Schema(
                    description = "Selected salon service name",
                    example = "[\"Hair Cut\", \"Beard Trim\", \"Hair Spa\"]"

            ),
            arraySchema = @Schema(
                    description = "List of selected salon services",
                    requiredMode = Schema.RequiredMode.REQUIRED
            )
    )
    @NotEmpty(message = "At least one service must be selected")
    private List<
            @NotBlank(message = "Service name cannot be empty")
                    String
            > serviceNames;
}