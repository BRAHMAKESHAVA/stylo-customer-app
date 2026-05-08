package org.backend.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(
        name = "Service Category Response",
        description = "DTO used for returning service category details"
)
public class ServiceCategoryResponse {

    @Schema(
            description = "Unique category ID",
            example = "1"
    )
    private Long categoryId;

    @Schema(
            description = "Service category name",
            example = "Hair Styling"
    )
    private String categoryName;

    @Schema(
            description = "Status of service category",
            example = "true"
    )
    private Boolean isActive;
}