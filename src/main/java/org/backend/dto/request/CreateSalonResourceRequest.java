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
        name = "Create Salon Resource Request",
        description = "DTO used for managing salon resource capacity details"
)
public class CreateSalonResourceRequest {

    @Schema(description = "Unique salon ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Salon ID is required")
    private Long salonId;

    @Schema(description = "Total available salon resources or seats", example = "10", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Resource count is required")
    @Min(value = 1, message = "Resource count must be at least 1")
    private Integer resourceCount;
}