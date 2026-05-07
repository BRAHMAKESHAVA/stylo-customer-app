package org.backend.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceCategoryDTO {

    @NotNull(message = "Salon ID is required")
    private Long salonId;

    @NotBlank(message = "Category name cannot be empty")
    @Size(max = 100, message = "Category name must be less than 100 characters")
    private String categoryName;

    // Optional (default = true)
    private Boolean isActive = true;
}