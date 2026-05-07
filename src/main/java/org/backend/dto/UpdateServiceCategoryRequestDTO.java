package org.backend.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateServiceCategoryRequestDTO {

    private Long salonId;

    @Size(max = 100, message = "Category name must be less than 100 characters")
    @Pattern(regexp = "^[A-Za-z ]*$", message = "Category name must contain only letters and spaces")
    private String categoryName;

    private Boolean isActive;
}