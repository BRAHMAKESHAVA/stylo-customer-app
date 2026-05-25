package org.backend.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class CategoryServiceDTO {
    private String categoryName;
    private String serviceName;
}