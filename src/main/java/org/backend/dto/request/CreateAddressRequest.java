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
import org.backend.validation.annotation.ValidPinCode;

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

    @Schema(description = "Customer full name", example = "Brahma", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Customer name is required")
    @Size(max = 40, message = "Customer name cannot exceed 40 characters")
    @Pattern(
            regexp = "^[A-Za-z]+(?: [A-Za-z]+)*$",
            message = "Customer name must contain only letters with single spaces between words"
    )
    private String customerName;

    @Schema(description = "House or flat number", example = "A-203", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "House number is required")
    @Size(max = 20, message = "House number cannot exceed 20 characters")
    @Pattern(
            regexp = "^[A-Za-z0-9]+(?:[-/ ][A-Za-z0-9]+)*$",
            message = "House number can contain letters, numbers, hyphen, slash, and single spaces only"
    )
    private String houseNumber;

    @Schema(description = "Apartment or building name", example = "Sri Sai Residency")
    @Size(max = 40, message = "Building name cannot exceed 40 characters")
    @Pattern(
            regexp = "^[A-Za-z]+(?: [A-Za-z]+)*$",
            message = "Building name must contain only letters with single spaces between words"
    )
    private String buildingName;

    @Schema(description = "Area or locality", example = "Junnasandra")
    @Size(max = 70, message = "Area cannot exceed 70 characters")
    @Pattern(
            regexp = "^[A-Za-z0-9]+(?:[-/ ][A-Za-z0-9]+)*$",
            message = "Area can contain letters, numbers, hyphen, slash, and single spaces only"
    )
    private String area;

    @Schema(description = "Nearby landmark", example = "Near Axiotech Solutions")
    @Size(max = 40, message = "Landmark cannot exceed 40 characters")
    @Pattern(
            regexp = "^[A-Za-z0-9]+(?: [A-Za-z0-9]+)*$",
            message = "Landmark can contain letters and numbers with single spaces between words"
    )
    private String landmark;

    @Schema(description = "City name", example = "Bangalore", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "City is required")
    @Size(max = 30, message = "City cannot exceed 30 characters")
    @Pattern(
            regexp = "^[A-Za-z]+(?: [A-Za-z]+)*$",
            message = "City must contain only letters with single spaces between words"
    )
    private String city;

    @Schema(description = "State name", example = "Karnataka", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "State is required")
    @Size(max = 40, message = "State cannot exceed 40 characters")
    @Pattern(
            regexp = "^[A-Za-z]+(?: [A-Za-z]+)*$",
            message = "State must contain only letters with single spaces between words"
    )
    private String state;

    @Schema(description = "Country code", example = "IN")
    @NotBlank(message = "Country code is required")
    @Size(max = 10, message = "Country code cannot exceed 10 characters")
    @Pattern(
            regexp = "^[A-Z]{2}$",
            message = "Country code must contain exactly 2 uppercase letters"
    )
    private String countryCode;

    @Schema(description = "6-digit postal PIN code", example = "560035", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Pin code is required")
    @ValidPinCode
    private String pinCode;

    @Schema(description = "Latitude coordinate", example = "12.912345", requiredMode = Schema.RequiredMode.REQUIRED)
    @ValidLatitude
    private BigDecimal latitude;

    @Schema(description = "Longitude coordinate", example = "77.684567", requiredMode = Schema.RequiredMode.REQUIRED)
    @ValidLongitude
    private BigDecimal longitude;

    @Schema(
            description = "Type of address",
            example = "HOME",
            allowableValues = {"HOME", "WORK", "OTHER"},
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "Address type is required")
    @Enumerated(EnumType.STRING)
    private AddressType addressType;

    @Schema(description = "Custom address label", example = "Stylo Home Address", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Label name is required")
    @Size(max = 100, message = "Label name cannot exceed 100 characters")
    @Pattern(
            regexp = "^[A-Za-z]+(?: [A-Za-z]+)*$",
            message = "Label name must contain only letters with single spaces between words"
    )
    private String labelName;

    @Schema(description = "Marks whether this address is default", example = "false")
    @Builder.Default
    private Boolean isDefault = false;

    @Schema(description = "Marks whether this address is selected", example = "true")
    @Builder.Default
    private Boolean isSelected = true;
}