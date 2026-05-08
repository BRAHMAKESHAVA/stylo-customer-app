package org.backend.dto.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.backend.enums.Role;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(
        name = "Send OTP Request",
        description = "DTO used for sending OTP to mobile number based on user role"
)
public class SendOtpRequest {

    @Schema(
            description = "Registered Indian mobile number",
            example = "9876543210",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "Mobile number is required")
    @Pattern(
            regexp = "^[6-9]\\d{9}$",
            message = "Invalid Indian mobile number"
    )
    private String mobile;

    @Schema(
            description = "Role of the user requesting OTP",
            example = "CUSTOMER",
            allowableValues = {"CUSTOMER", "PARTNER", "ADMIN","CAPTAIN"},
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "Role is required")
    private Role role;
}