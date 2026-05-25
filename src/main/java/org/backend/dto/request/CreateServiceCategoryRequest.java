package org.backend.dto.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(
        name = "Create Service Category Request",
        description = "DTO used for creating salon service categories"
)
public class CreateServiceCategoryRequest {

    @Schema(description = "Unique salon ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Salon ID is required")
    private Long salonId;

    @Schema(description = "Service category name", example = "Hair Styling", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Category name cannot be empty")
    @Size(max = 100, message = "Category name must be less than 100 characters")
    private String categoryName;

    @Schema(description = "Status of service category", example = "true", defaultValue = "true")
    @Builder.Default
    private Boolean isActive = true;
}