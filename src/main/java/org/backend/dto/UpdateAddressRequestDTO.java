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
public class UpdateAddressRequestDTO {

    @Size(max = 40)
    private String customerName;

    @Size(max = 20)
    private String houseNumber;

    @Size(max = 40)
    private String buildingName;

    @Size(max = 70)
    private String area;

    @Size(max = 40)
    private String landmark;

    @Size(max = 30)
    private String city;

    @Size(max = 40)
    private String state;

    @Size(max = 10)
    private String countryCode;

    @Pattern(regexp = "^[1-9][0-9]{5}$", message = "Invalid PIN code")
    private String pinCode;

    @DecimalMin(value = "-90.0")
    @DecimalMax(value = "90.0")
    private BigDecimal latitude;

    @DecimalMin(value = "-180.0")
    @DecimalMax(value = "180.0")
    private BigDecimal longitude;

    private AddressType addressType;

    @Size(max = 100)
    private String labelName;

    private Boolean isDefault;
}