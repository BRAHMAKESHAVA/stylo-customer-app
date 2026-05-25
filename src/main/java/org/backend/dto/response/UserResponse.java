package org.backend.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.backend.enums.Role;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserResponse {
    @Schema(description = "Unique user ID", example = "1")
    private Long id;

    @Schema(description = "User first name", example = "Brahma")
    private String firstName;

    @Schema(description = "User last name", example = "Kumar")
    private String lastName;

    @Schema(description = "Gender of the user", example = "Male")
    private String gender;

    @Schema(description = "Age of the user", example = "25")
    private Integer age;

    @Schema(description = "Total years of professional experience", example = "2")
    private Integer experience;

    @Schema(description = "Role assigned to the user", example = "CUSTOMER")
    private Role role;

    @Schema(description = "Valid 10-digit Indian mobile number", example = "9876543210")
    private String mobile;

    @Schema(description = "Valid email address of the user", example = "stylo@gmail.com")
    private String email;

    @Schema(description = "Encrypted user password")
    private String password;
}
