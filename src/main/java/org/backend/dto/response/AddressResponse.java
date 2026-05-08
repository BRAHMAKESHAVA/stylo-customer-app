package org.backend.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.backend.enums.AddressType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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

    private Long addressId;

    private Long customerId;

    private String customerName;

    private String houseNumber;

    private String buildingName;

    private String area;

    private String landmark;

    private String city;

    private String state;

    private String countryCode;

    private String pinCode;

    private BigDecimal latitude;

    private BigDecimal longitude;

    private AddressType addressType;

    private String labelName;

    private Boolean isDefault;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}