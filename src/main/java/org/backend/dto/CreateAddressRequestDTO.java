package org.backend.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import org.backend.enums.AddressType;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateAddressRequestDTO {

    @NotNull(message = "Customer ID is required")
    private Long customerId;

    @NotBlank(message = "Customer name is required")
    @Size(max = 40)
    private String customerName;

    @NotBlank(message = "House number is required")
    @Size(max = 20)
    private String houseNumber;

    @Size(max = 40)
    private String buildingName;

    @Size(max = 70)
    private String area;

    @Size(max = 40)
    private String landmark;

    @NotBlank(message = "City is required")
    @Size(max = 30)
    private String city;

    @NotBlank(message = "State is required")
    @Size(max = 40)
    private String state;

    @Size(max = 10)
    private String countryCode;

    @NotBlank(message = "Pin code is required")
    @Pattern(regexp = "^[1-9][0-9]{5}$", message = "Invalid PIN code")
    private String pinCode;

    @NotNull(message = "Latitude is required")
    @DecimalMin(value = "-90.0")
    @DecimalMax(value = "90.0")
    private BigDecimal latitude;

    @NotNull(message = "Longitude is required")
    @DecimalMin(value = "-180.0")
    @DecimalMax(value = "180.0")
    private BigDecimal longitude;

    @NotNull(message = "Address type is required")
    private AddressType addressType;

    @NotBlank(message = "Label name is required")
    @Size(max = 100)
    private String labelName;

    private Boolean isDefault = false;
}