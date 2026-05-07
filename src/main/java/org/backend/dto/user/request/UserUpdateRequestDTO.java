package org.backend.dto.user.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;
import org.backend.validation.annotation.ValidGender;

@Data
public class UserUpdateRequestDTO {

    @Schema(
            description = "User's first name (only letters allowed)",
            example = "Brahma"
    )
    @Pattern(
            regexp = "$|^[A-Za-z]+( [A-Za-z]+)*$",
            message = "First name must contain only letters."
    )
    private String firstName;

    @Schema(
            description = "User's last name (only letters allowed)",
            example = "Kumar"
    )
    @Pattern(
            regexp = "$|^[A-Za-z]+( [A-Za-z]+)*$",
            message = "Last name must contain only letters."
    )
    private String lastName;

    @Schema(
            description = "User gender (Male, Female, Other)",
            example = "Male"
    )
    @Pattern(
            regexp = "$|^[A-Za-z]+$",
            message = "Gender must contain only letters."
    )
    @ValidGender
    private String gender;

    @Schema(
            description = "User age (must be between 18 and 100)",
            example = "25"
    )
    @Min(value = 18, message = "Age must be >= 18")
    @Max(value = 100, message = "Age must be <= 100")
    private Integer age;

    @Schema(
            description = "User email address",
            example = "brahma@gmail.com"
    )
    @Email(message = "Invalid email format")
    @Pattern(
            regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$",
            message = "Please enter a valid email address"
    )
    private String email;
}