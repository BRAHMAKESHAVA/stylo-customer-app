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
@Schema(name = "User Register Request")
public class UserRegisterRequestDTO {

    @Schema(description = "First name (only letters)", example = "Brahma")
    @ValidName(field = "First name")
    private String firstName;

    @Schema(description = "Last name (only letters)", example = "Kumar")
    @ValidName(field = "Last name")
    private String lastName;

    @Schema(description = "Gender", example = "Male", allowableValues = {"Male","Female","Other"})
    @ValidGender
    private String gender;

    @Schema(description = "Age (18–100)", example = "25")
    @ValidAge
    private String age;

    @Schema(description = "Years of experience", example = "2")
    @Min(value = 0, message = "Experience cannot be negative")
    private Integer experience;

    @Schema(description = "User role", example = "CUSTOMER")
    @NotNull(message = "Role is required")
    private Role role;

    @Schema(description = "Indian mobile number", example = "9876543210")
    @NotBlank(message = "Mobile number is required")
    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Invalid Indian mobile number")
    private String mobile;

    @Schema(description = "Email address", example = "brahma@gmail.com")
    @ValidEmail(field = "Email")
    private String email;

    @Schema(description = "Strong password", example = "Brahma@123")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&]).{8,}$",
            message = "Password must contain uppercase, lowercase, number and special character"
    )
    private String password;
}