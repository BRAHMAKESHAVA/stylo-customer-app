package org.backend.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalonServiceDTO {

    @NotNull(message = "Salon ID is required")
    private Long salonId;

    @NotNull(message = "Category ID is required")
    private Long categoryId;

    @NotBlank(message = "Service name cannot be empty")
    @Size(max = 100, message = "Service name must be less than 100 characters")
    private String serviceName;

    @NotNull(message = "Duration is required")
    @Positive(message = "Duration must be greater than 0")
    private Integer durationMinutes;

    private Integer bufferMinutes = 0;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    @Digits(integer = 8, fraction = 2, message = "Price must be valid (max 10 digits, 2 decimals)")
    private BigDecimal price;

    // By default, a new service is active. This can be changed when updating the service.
    private Boolean isActive = true;
}
