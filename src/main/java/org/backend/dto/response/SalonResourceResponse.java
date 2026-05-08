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
        name = "Salon Resource Response",
        description = "DTO used for returning salon resource details"
)
public class SalonResourceResponse {

    @Schema(description = "Unique resource ID", example = "1")
    private Long id;

    @Schema(description = "Unique salon ID", example = "1")
    private Long salonId;

    @Schema(description = "Available salon resources or seats", example = "10")
    private Integer resourceCount;
}