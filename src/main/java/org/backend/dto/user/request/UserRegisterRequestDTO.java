package org.backend.dto.user.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.backend.enums.Role;
import org.backend.validation.annotation.ValidAge;
import org.backend.validation.annotation.ValidEmail;
import org.backend.validation.annotation.ValidGender;
import org.backend.validation.annotation.ValidName;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(
        name = "User Register Request",
        description = "Request payload used to register a new user in the system"
)
public class UserRegisterRequestDTO {

    @Schema(
            description = "User first name. Only alphabetic characters are allowed",
            example = "Brahma",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @ValidName(field = "First name")
    private String firstName;

    @Schema(
            description = "User last name. Only alphabetic characters are allowed",
            example = "Kumar",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @ValidName(field = "Last name")
    private String lastName;

    @Schema(
            description = "Gender of the user",
            example = "Male",
            allowableValues = {"Male", "Female", "Other"},
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @ValidGender
    private String gender;

    @Schema(
            description = "Age of the user. Allowed range is between 18 and 100",
            example = "25",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @ValidAge
    private String age;

    @Schema(
            description = "Total years of professional experience",
            example = "2",
            minimum = "0"
    )
    @Min(value = 0, message = "Experience cannot be negative")
    private Integer experience;

    @Schema(
            description = "Role assigned to the user",
            example = "CUSTOMER",
            allowableValues = {"CUSTOMER", "ADMIN", "CAPTAIN", "PARTNER"},
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "Role is required")
    private Role role;

    @Schema(
            description = "Valid 10-digit Indian mobile number",
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
            description = "Valid email address of the user",
            example = "stylo@gmail.com"
    )
    @ValidEmail(field = "Email")
    private String email;

    @Schema(
            description = """
                    Strong password with:
                    - Minimum 8 characters
                    - At least 1 uppercase letter
                    - At least 1 lowercase letter
                    - At least 1 number
                    - At least 1 special character
                    """,
            example = "AxioTechX@123",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&]).{8,}$",
            message = "Password must contain uppercase, lowercase, number and special character"
    )
    private String password;
}