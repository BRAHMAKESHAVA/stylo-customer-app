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
        name = "Update Service Category Request",
        description = "DTO used for updating salon service category details"
)
public class UpdateServiceCategoryRequest {

    @Schema(description = "Unique salon ID", example = "1")
    private Long salonId;

    @Schema(description = "Service category name", example = "Hair Styling")
    @Size(max = 100, message = "Category name must be less than 100 characters")
    @Pattern(regexp = "^[A-Za-z ]*$", message = "Category name must contain only letters and spaces")
    private String categoryName;

    @Schema(description = "Status of service category", example = "true")
    private Boolean isActive;
}