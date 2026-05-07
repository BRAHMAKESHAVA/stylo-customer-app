package org.backend.dto;

import jakarta.validation.constraints.Min;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateSalonResourceRequestDTO {

    private Long salonId;

    @Min(value = 1, message = "Resource count must be at least 1")
    private Integer resourceCount;
}