package org.backend.dto.response;

import lombok.*;
import org.backend.enums.SlotStatus;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SlotResponseDTO {

    private String slotTime;
    private SlotStatus status;

}