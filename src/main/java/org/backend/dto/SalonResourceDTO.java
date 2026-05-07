package org.backend.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalonResourceDTO {

    @NotNull(message = "Salon ID is required")
    private Long salonId;

    @NotNull(message = "Resource count is required")
    @Min(value = 1, message = "Resource count must be at least 1")
    private Integer resourceCount;
}