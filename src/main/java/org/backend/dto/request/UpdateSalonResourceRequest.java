package org.backend.dto.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(
        name = "Update Salon Resource Request",
        description = "DTO used for updating salon resource capacity details"
)
public class UpdateSalonResourceRequest {

    @Schema(description = "Unique salon ID", example = "1")
    private Long salonId;

    @Schema(description = "Updated available salon resources or seats", example = "15")
    @Min(value = 1, message = "Resource count must be at least 1")
    private Integer resourceCount;
}