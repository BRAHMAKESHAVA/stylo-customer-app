package org.backend.dto.auth.request;

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
        name = "Verify OTP Request",
        description = "DTO used for verifying OTP during login or authentication"
)
public class VerifyOtpRequest {

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
            description = "Role of the user verifying OTP",
            example = "CUSTOMER",
            allowableValues = {"CUSTOMER", "PARTNER", "ADMIN","CAPTAIN"},
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "Role is required")
    private Role role;

    @Schema(
            description = "4-digit OTP sent to mobile number",
            example = "1234",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "OTP is required")
    @Pattern(
            regexp = "^\\d{4}$",
            message = "OTP must be a 4-digit number"
    )
    private String otp;
}