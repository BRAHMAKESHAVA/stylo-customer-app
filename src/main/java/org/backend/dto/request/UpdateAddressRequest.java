package org.backend.dto.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;
import org.backend.enums.AddressType;
import org.backend.validation.annotation.ValidLatitude;
import org.backend.validation.annotation.ValidLongitude;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(
        name = "Update Address Request",
        description = "DTO used to update customer address details"
)
public class UpdateAddressRequest {

    @Schema(
            description = "Customer full name",
            example = "Brahma"
    )
    @Size(max = 40, message = "Customer name cannot exceed 40 characters")
    private String customerName;

    @Schema(
            description = "House or flat number",
            example = "A-203"
    )
    @Size(max = 20, message = "House number cannot exceed 20 characters")
    private String houseNumber;

    @Schema(
            description = "Apartment or building name",
            example = "Sri Sai Residency"
    )
    @Size(max = 40, message = "Building name cannot exceed 40 characters")
    private String buildingName;

    @Schema(
            description = "Area or locality",
            example = "Junnasandra"
    )
    @Size(max = 70, message = "Area cannot exceed 70 characters")
    private String area;

    @Schema(
            description = "Nearby landmark",
            example = "Near Axiotech Solutions"
    )
    @Size(max = 40, message = "Landmark cannot exceed 40 characters")
    private String landmark;

    @Schema(
            description = "City name",
            example = "Bangalore"
    )
    @Size(max = 30, message = "City cannot exceed 30 characters")
    private String city;

    @Schema(
            description = "State name",
            example = "Karnataka"
    )
    @Size(max = 40, message = "State cannot exceed 40 characters")
    private String state;

    @Schema(
            description = "Country dialing code",
            example = "+91"
    )
    @Size(max = 10, message = "Country code cannot exceed 10 characters")
    private String countryCode;

    @Schema(
            description = "6-digit postal PIN code",
            example = "560035"
    )
    @Pattern(
            regexp = "^[1-9][0-9]{5}$",
            message = "Invalid PIN code. It must be a valid 6-digit number"
    )
    private String pinCode;

    @Schema(
            description = "Latitude coordinate",
            example = "12.912345"
    )
    @ValidLatitude
    private BigDecimal latitude;

    @Schema(
            description = "Longitude coordinate",
            example = "77.684567"
    )
    @ValidLongitude
    private BigDecimal longitude;

    @Schema(
            description = "Type of address",
            example = "HOME",
            allowableValues = {"HOME", "WORK", "OTHER"}
    )
    private AddressType addressType;

    @Schema(
            description = "Custom address label",
            example = "Stylo Home Address"
    )
    @Size(max = 100, message = "Label name cannot exceed 100 characters")
    private String labelName;

    @Schema(
            description = "Customer email address",
            example = "support@stylo.com"
    )
    @Email(message = "Invalid email format")
    @Size(max = 100, message = "Email cannot exceed 100 characters")
    private String email;

    @Schema(
            description = "Marks address as default",
            example = "true"
    )
    private Boolean isDefault;
}