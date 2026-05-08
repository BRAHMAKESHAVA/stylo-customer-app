package org.backend.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.backend.enums.AddressType;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(
        name = "Address Response",
        description = "DTO used for returning customer address details"
)
public class AddressResponse {

    @Schema(description = "Unique address ID", example = "1")
    private Long addressId;

    @Schema(description = "Unique customer ID", example = "101")
    private Long customerId;

    @Schema(description = "Customer full name", example = "Brahma")
    private String customerName;

    @Schema(description = "House or flat number", example = "A-203")
    private String houseNumber;

    @Schema(description = "Apartment or building name", example = "Sri Sai Residency")
    private String buildingName;

    @Schema(description = "Area or locality", example = "Junnasandra")
    private String area;

    @Schema(description = "Nearby landmark", example = "Near Axiotech Solutions")
    private String landmark;

    @Schema(description = "City name", example = "Bangalore")
    private String city;

    @Schema(description = "State name", example = "Karnataka")
    private String state;

    @Schema(description = "Country dialing code", example = "+91")
    private String countryCode;

    @Schema(description = "6-digit postal PIN code", example = "560035")
    private String pinCode;

    @Schema(description = "Latitude coordinate", example = "12.912345")
    private BigDecimal latitude;

    @Schema(description = "Longitude coordinate", example = "77.684567")
    private BigDecimal longitude;

    @Schema(description = "Type of address", example = "HOME", allowableValues = {"HOME", "WORK", "OTHER"})
    private AddressType addressType;

    @Schema(description = "Custom address label", example = "Stylo Home Address")
    private String labelName;

    @Schema(description = "Marks whether this address is default", example = "true")
    private Boolean isDefault;
}