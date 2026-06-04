package org.backend.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.backend.enums.SlotStatus;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(
        name = "Slot Response",
        description = "DTO representing an available or unavailable booking slot for a salon."
)
public class SlotResponseDTO {

    @Schema(description = "Time of the slot in HH:mm format", example = "10:30")
    private String slotTime;

    @Schema(description = "Status of the slot (e.g., AVAILABLE, BOOKED, UNAVAILABLE)", example = "AVAILABLE")
    private SlotStatus status;
}
