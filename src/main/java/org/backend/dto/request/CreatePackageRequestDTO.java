package org.backend.dto.packagee;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class CreatePackageRequestDTO {

    @NotNull(message = "Salon ID is required")
    private Long salonId;

    @NotBlank(message = "Package name is required")
    private String packageName;

    private String description;

    @NotNull(message = "Package price is required")
    private BigDecimal packagePrice;

    @NotEmpty(message = "At least one service must be selected")
    private List<Long> serviceIds;
}