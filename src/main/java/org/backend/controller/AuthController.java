package org.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.backend.dto.common.ApiResponseDTO;
import org.backend.dto.request.GenerateTokenRequest;
import org.backend.dto.request.SendOtpRequest;
import org.backend.dto.request.VerifyOtpRequest;
import org.backend.dto.response.AuthResponse;
import org.backend.dto.response.RefreshTokenResponse;
import org.backend.dto.response.SendOtpResponse;
import org.backend.service.AuthService;
import org.backend.service.OtpService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for authentication and authorization operations.
 *
 * This controller handles OTP-based authentication flows including OTP generation,
 * verification, and JWT token refresh operations. All endpoints are accessible at
 * the base path "/auth/login".
 *
 * The authentication flow typically follows:
 * 1. Client sends phone number to receive OTP via {@link #sendOtp(SendOtpRequest)}
 * 2. Client verifies OTP to receive JWT tokens via {@link #verifyOtp(VerifyOtpRequest, HttpServletRequest)}
 * 3. Client refreshes expired access tokens via {@link #refreshToken(String, HttpServletRequest)}
 *
 * @author Stylo User Management Service
 * @version 1.0
 */
@RestController
@RequestMapping("/auth/login")
@RequiredArgsConstructor
@Slf4j
@Tag(
        name = "Authentication Management",
        description = "Endpoints for user authentication including OTP-based login, token verification, and JWT refresh operations"
)
public class AuthController {

    /** Service for OTP generation and validation operations */
    private final OtpService otpService;

    /** Service for JWT token generation and refresh operations */
    private final AuthService authService;

    /**
     * Generates and sends an OTP to the provided phone number.
     *
     * This endpoint initiates the authentication process by sending a One-Time Password
     * to the user's registered phone number via SMS or other communication channel.
     *
     * @param request the OTP send request containing the phone number
     * @return ResponseEntity containing the OTP response with status and message
     * @throws jakarta.validation.ConstraintViolationException if the request body validation fails
     */
    @PostMapping(
            value = "/sendOTP",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            summary = "Send OTP to phone number",
            description = "Initiates the authentication process by sending a One-Time Password (OTP) to the user's registered phone number via SMS. " +
                    "This is the first step in the OTP-based authentication flow.",
            operationId = "sendOTP"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "OTP sent successfully",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid phone number format or validation error",
                    content = @Content

            ),
            @ApiResponse(
                    responseCode = "429",
                    description = "Too many OTP requests - rate limit exceeded",
                    content = @Content

            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error while sending OTP",
                    content = @Content

            )
    })
    public ResponseEntity<SendOtpResponse> sendOtp(@Valid @RequestBody SendOtpRequest request) {
        log.info(String.valueOf(request));
        return ResponseEntity.ok(otpService.generateOtp(request));
    }

    /**
     * Verifies the OTP provided by the user and issues JWT tokens upon successful verification.
     *
     * This endpoint validates the OTP that was previously sent to the user's phone number.
     * Upon successful verification, it generates and returns JWT access and refresh tokens
     * for authenticated session management.
     *
     * @param request the OTP verification request containing the phone number and OTP code
     * @param httpRequest the HTTP servlet request containing request metadata and headers
     * @return ResponseEntity containing the OTP verification response with JWT tokens
     * @throws jakarta.validation.ConstraintViolationException if the request body validation fails
     */
    @PostMapping(
            value = "/verifyOTP",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            summary = "Verify OTP and obtain JWT tokens",
            description = "Validates the OTP code sent to the user's phone number. On successful verification, " +
                    "returns JWT access token and refresh token for authenticated API access.",
            operationId = "verifyOTP"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "OTP verified successfully - JWT tokens returned",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid OTP or phone number format",
                    content = @Content

            ),
            @ApiResponse(
                    responseCode = "410",
                    description = "OTP expired or invalid",
                    content = @Content

            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "User not found for given phone number",
                    content = @Content

            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content

            )
    })
    public ResponseEntity<ApiResponseDTO<AuthResponse>> verifyOtp(
            @Valid @RequestBody VerifyOtpRequest request, HttpServletRequest httpRequest) {
        AuthResponse response = otpService.validateOtp(request, httpRequest);
        return ResponseEntity.ok(
                ApiResponseDTO.<AuthResponse>builder()
                        .status(true)
                        .message("OTP verified successfully")
                        .data(response)
                        .build()
        );
    }

    /**
     * Refreshes an expired JWT access token using a valid refresh token.
     *
     * This endpoint allows clients to obtain a new access token without requiring
     * re-authentication. The refresh token is extracted from the Authorization header
     * (Bearer token format) and used to issue a new access token.
     *
     * @param authorizationHeader the Authorization header containing the refresh token in Bearer format
     * @param httpRequest the HTTP servlet request containing request metadata and headers
     * @return ResponseEntity containing the token refresh response with the new access token
     */
    @PostMapping(
            value = "/refreshToken",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            summary = "Refresh JWT access token",
            description = "Issues a new access token using a valid refresh token. This allows clients to maintain " +
                    "an authenticated session without re-authentication when the access token expires.",
            operationId = "refreshToken"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Token refreshed successfully",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid token format or missing Authorization header",
                    content = @Content

            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Refresh token expired or invalid",
                    content = @Content

            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content

            )
    })
    public ResponseEntity<ApiResponseDTO<RefreshTokenResponse>> refreshToken(
            @RequestHeader("Authorization") String authorizationHeader, HttpServletRequest httpRequest) {
        String refreshToken = authorizationHeader.replace("Bearer ", "").trim();
        RefreshTokenResponse response = authService.refreshToken(refreshToken, httpRequest);
        return ResponseEntity.ok(
                ApiResponseDTO.<RefreshTokenResponse>builder()
                        .status(true)
                        .message("Token refreshed successfully")
                        .data(response)
                        .build()
        );
    }

    @PostMapping(
            value = "/token",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            summary = "Generate JWT token for guest user",
            description = "Generates JWT access and refresh tokens for an guest user identified by mobile number. " +
                    "This endpoint is useful for programmatic token generation.",
            operationId = "generateToken"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Token generated successfully",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid mobile number format",
                    content = @Content

            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "User not found",
                    content = @Content

            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content

            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Authentication is required or the provided token is invalid",
                    content = @Content
            )
    })
    public ResponseEntity<ApiResponseDTO<AuthResponse>> generateToken(
            @Valid @RequestBody GenerateTokenRequest request, HttpServletRequest httpRequest) {
        String mobileNumber = request.getMobile();
        AuthResponse response = authService.generateToken(mobileNumber, httpRequest);

        return ResponseEntity.ok(
                ApiResponseDTO.<AuthResponse>builder()
                        .status(true)
                        .message("Token generated successfully")
                        .data(response)
                        .build()
        );
    }

}
