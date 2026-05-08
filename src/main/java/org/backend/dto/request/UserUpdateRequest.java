package org.backend.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;
import org.backend.validation.annotation.ValidGender;

@Data
@Schema(
        name = "User Update Request",
        description = "Request payload used to update user profile details"
)
public class UserUpdateRequest {

    @Schema(
            description = "User's first name. Only alphabets and spaces are allowed",
            example = "Axiotechx"
    )
    @Pattern(
            regexp = "$|^[A-Za-z]+( [A-Za-z]+)*$",
            message = "First name must contain only letters."
    )
    private String firstName;

    @Schema(
            description = "User's last name. Only alphabets and spaces are allowed",
            example = "Stylo"
    )
    @Pattern(
            regexp = "$|^[A-Za-z]+( [A-Za-z]+)*$",
            message = "Last name must contain only letters."
    )
    private String lastName;

    @Schema(
            description = "Gender of the user. Allowed values: Male, Female, Other (case-insensitive)",
            example = "Male",
            allowableValues = {"Male", "Female", "Other"}
    )
    @Pattern(
            regexp = "(?i)^$|^(male|female|other)$",
            message = "Gender must be Male, Female, or Other"
    )
    @ValidGender
    private String gender;

    @Schema(
            description = "User age. Allowed range is between 18 and 100",
            example = "25",
            minimum = "18",
            maximum = "100"
    )
    @Min(value = 18, message = "Age must be >= 18")
    @Max(value = 100, message = "Age must be <= 100")
    private Integer age;

    @Schema(
            description = "Valid email address of the user",
            example = "support@axiotechx.com"
    )
    @Email(message = "Invalid email format")
    @Pattern(
            regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$",
            message = "Please enter a valid email address"
    )
    private String email;
}