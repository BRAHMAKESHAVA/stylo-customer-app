package org.backend.dto.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
        name = "Create Address Request",
        description = "DTO used for storing customer address details"
)
public class CreateAddressRequest {

    @Schema(description = "Unique customer ID", example = "101", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Customer ID cannot be null")
    private Long customerId;

    @Schema(description = "Customer full name", example = "Brahma", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Customer name is required")
    @Size(max = 40, message = "Customer name cannot exceed 40 characters")
    private String customerName;

    @Schema(description = "House or flat number", example = "A-203", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "House number is required")
    @Size(max = 20, message = "House number cannot exceed 20 characters")
    private String houseNumber;

    @Schema(description = "Apartment or building name", example = "Sri Sai Residency")
    @Size(max = 40, message = "Building name cannot exceed 40 characters")
    private String buildingName;

    @Schema(description = "Area or locality", example = "Junnasandra")
    @Size(max = 70, message = "Area cannot exceed 70 characters")
    private String area;

    @Schema(description = "Nearby landmark", example = "Near Axiotech Solutions")
    @Size(max = 40, message = "Landmark cannot exceed 40 characters")
    private String landmark;

    @Schema(description = "City name", example = "Bangalore", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "City is required")
    @Size(max = 30, message = "City cannot exceed 30 characters")
    private String city;

    @Schema(description = "State name", example = "Karnataka", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "State is required")
    @Size(max = 40, message = "State cannot exceed 40 characters")
    private String state;

    @Schema(description = "Country dialing code", example = "+91", requiredMode = Schema.RequiredMode.REQUIRED)
    @Size(max = 10, message = "Country code cannot exceed 10 characters")
    @NotBlank(message = "Country code is required")
    private String countryCode;

    @Schema(description = "6-digit postal PIN code", example = "560035", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Pin code is required")
    @Pattern(regexp = "^[1-9][0-9]{5}$", message = "Invalid PIN code. It must be a valid 6-digit number")
    @Size(max = 10, message = "Pin code cannot exceed 10 characters")
    private String pinCode;

    @Schema(description = "Latitude coordinate", example = "12.912345")
    @ValidLatitude
    private BigDecimal latitude;

    @Schema(description = "Longitude coordinate", example = "77.684567")
    @ValidLongitude
    private BigDecimal longitude;

    @Schema(description = "Type of address", example = "HOME", allowableValues = {"HOME", "WORK", "OTHER"}, requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Address type is required")
    @Enumerated(EnumType.STRING)
    private AddressType addressType;

    @Schema(description = "Custom address label", example = "Stylo Home Address", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Label name is required")
    @Size(max = 100, message = "Label name cannot exceed 100 characters")
    private String labelName;

    @Schema(description = "Marks whether this address is default", example = "true", defaultValue = "false")
    private Boolean isDefault = false;
}