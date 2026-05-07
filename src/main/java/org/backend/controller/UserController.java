package org.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.backend.dto.common.PageResponse;
import org.backend.dto.user.request.UserRegisterRequestDTO;
import org.backend.dto.user.request.UserUpdateRequestDTO;
import org.backend.dto.user.response.UserRegisterResponseDTO;
import org.backend.model.Customer;
import org.backend.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.backend.dto.common.ApiResponseDto;
import java.util.List;

/**
 * REST controller for managing user and customer operations.
 *
 * This controller provides endpoints for user registration, profile management,
 * and customer information retrieval. It handles HTTP requests related to user
 * and customer resources at the base path "/user".
 *
 * @author Stylo User Management Service
 * @version 1.0
 */
@RequestMapping("/user")
@RestController
@RequiredArgsConstructor
@Tag(
        name = "User Management",
        description = "Endpoints for user registration, profile management, and customer information retrieval"
)
public class UserController {

    /** Service layer for user-related business logic operations */
    private final UserService userService;

    /**
     * Registers a new user with the provided registration details.
     *
     * @param registerUser the user registration request containing user details
     * @return ResponseEntity containing the registered user's information wrapped in an ApiResponse
     * @throws jakarta.validation.ConstraintViolationException if the request body validation fails
     */
    @PostMapping(
            value = "/register",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            summary = "Register a new user",
            description = "Creates a new user account with the provided registration details. Returns the newly created user's information.",
            operationId = "userRegister"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "User registered successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid registration data or validation error"
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "User already exists with the given mobile number"
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error during registration"
            )
    })
    ResponseEntity<ApiResponseDto<UserRegisterResponseDTO>> userRegister(@Valid @RequestBody UserRegisterRequestDTO registerUser) {
       UserRegisterResponseDTO response = userService.userRegister(registerUser);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponseDto.<UserRegisterResponseDTO>builder()
                        .status(true)
                        .message("User with mobile " + response.getMobile() + " has been successfully created!")
                        .data(response)
                        .build());
    }

    /**
     * Updates an existing user's information.
     *
     * @param userId the ID of the user to be updated
     * @param updateRequestDTO the update request containing new user details
     * @return ResponseEntity containing the updated user information wrapped in an ApiResponse
     */
    @PutMapping(
            value = "/{userId}",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            summary = "Update user profile",
            description = "Updates an existing user's profile information. Only provided fields will be updated.",
            operationId = "updateUser"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "User updated successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid update data or validation error"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "User not found with the provided ID"
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error during update"
            )
    })
    public ResponseEntity<ApiResponseDto<UserRegisterResponseDTO>> updateUser(
            @Parameter(description = "Unique identifier of the user to update")
            @PathVariable Long userId,
            @Valid @RequestBody UserUpdateRequestDTO updateRequestDTO) {
        UserRegisterResponseDTO response = userService.updateUser(userId, updateRequestDTO);
        return ResponseEntity.ok(
                ApiResponseDto.<UserRegisterResponseDTO>builder()
                        .status(true)
                        .message("User updated successfully")
                        .data(response)
                        .build()
        );
    }

    /**
     * Retrieves a user by their unique identifier.
     *
     * @param userId the ID of the user to retrieve
     * @return ResponseEntity containing the user information wrapped in an ApiResponse
     */
    @GetMapping(
            value = "/{userId}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            summary = "Get user by ID",
            description = "Retrieves detailed information about a specific user by their unique identifier.",
            operationId = "getUserById"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "User retrieved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "User not found with the provided ID"
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error"
            )
    })
    public ResponseEntity<ApiResponseDto<UserRegisterResponseDTO>> getUserById(
            @Parameter(description = "Unique identifier of the user")
            @PathVariable Long userId) {
        UserRegisterResponseDTO response = userService.getUserById(userId);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponseDto.<UserRegisterResponseDTO>builder()
                        .status(true)
                        .message("User with the give userId: " + userId + " has been fetched successfully!")
                        .data(response)
                        .build());
    }

    /**
     * Retrieves all users from the system.
     *
     * Note: This endpoint is currently unrestricted but may be restricted to ADMIN role
     * in future versions (see commented @PreAuthorize annotation).
     *
     * @return ResponseEntity containing a list of all users wrapped in an ApiResponse
     */
    @GetMapping(
            value = "/all",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            summary = "Get all users with pagination",
            description = "Retrieves a paginated list of all users in the system. Supports pagination parameters.",
            operationId = "getAllUsers"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Users retrieved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid pagination parameters"
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error"
            )
    })
    public ResponseEntity<ApiResponseDto<PageResponse<UserRegisterResponseDTO>>> getAllUsers(
            @Parameter(description = "Page number (1-indexed)")
            @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "Number of items per page")
            @RequestParam(defaultValue = "20") int size
    ) {
        PageResponse<UserRegisterResponseDTO> users = userService.getAllUsers(page, size);

        return ResponseEntity.ok(
                ApiResponseDto.<PageResponse<UserRegisterResponseDTO>>builder()
                        .status(true)
                        .message("All users fetched successfully")
                        .data(users)
                        .build()
        );
    }

    /**
     * Retrieves a customer by their unique identifier.
     *
     * @param customerId the ID of the customer to retrieve
     * @return ResponseEntity containing the customer information wrapped in an ApiResponse
     */
    @GetMapping(
            value = "/customer/{customerId}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            summary = "Get customer by ID",
            description = "Retrieves detailed information about a specific customer by their unique identifier.",
            operationId = "getCustomerById"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Customer retrieved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Customer not found with the provided ID"
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error"
            )
    })
    public ResponseEntity<ApiResponseDto<Customer>> getCustomerById(
            @Parameter(description = "Unique identifier of the customer")
            @PathVariable Long customerId) {
        Customer response = userService.getCustomerById(customerId);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponseDto.<Customer>builder()
                        .status(true)
                        .message("Customer fetched successfully")
                        .data(response)
                        .build());
    }

    /**
     * Retrieves all customers from the system.
     *
     * @return ResponseEntity containing a list of all customers wrapped in an ApiResponse
     */
    @GetMapping(
            value = "/customer/all",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            summary = "Get all customers",
            description = "Retrieves a complete list of all customers registered in the system.",
            operationId = "getAllCustomers"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Customers retrieved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error"
            )
    })
    public ResponseEntity<ApiResponseDto<List<UserRegisterResponseDTO>>> getAllCustomers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {

        List<UserRegisterResponseDTO> customers = userService.getAllCustomers(page, size);

        return ResponseEntity.ok(
                ApiResponseDto.<List<UserRegisterResponseDTO>>builder()
                        .status(true)
                        .message("All customers fetched successfully")
                        .data(customers)
                        .build()
        );
    }
}
