package org.backend.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.backend.dto.LogoutRequest;
import org.backend.dto.response.AuthResponse;
import org.backend.dto.response.RefreshTokenResponse;
import org.backend.enums.Role;
import org.backend.exception.ResourceNotFoundException;
import org.backend.model.Customer;
import org.backend.model.PartnerDetails;
import org.backend.model.SalonDetails;
import org.backend.model.Users;
import org.backend.repository.CustomerRepository;
import org.backend.repository.PartnerRepository;
import org.backend.repository.SalonRepository;
import org.backend.repository.UserRepository;
import org.backend.utill.JwtUtill;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Service class for handling authentication-related operations.
 * Provides functionality for token refresh and request source extraction.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final JwtUtill jwtUtill;
    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final PartnerRepository partnerRepository;
    private final SalonRepository salonRepository;
    private final NotificationDeviceService notificationDeviceService;

    /**
     * Extracts the source (origin) of the request from the HttpServletRequest.
     * The method checks for the "Origin" header in the request and extracts the domain name by removing the "http://" or "https://" prefix.
     * If the "Origin" header is not present, it falls back to using the server name and port from the request.
     *
     * @param request The HttpServletRequest object containing details of the incoming request.
     * @return A string representing the source (origin) of the request.
     */
    // EXTRACT SOURCE
    String extractSource(HttpServletRequest request) {
        String origin = request.getHeader("Origin");

        if (origin != null) {
            return origin.replace("http://", "").replace("https://", "");
        }

        return request.getServerName() + ":" + request.getServerPort();
    }

    /**
     * Refreshes the access token and refresh token for a user based on the provided refresh token.
     * The method performs the following steps:
     * 1. Extracts the mobile number from the provided refresh token using the JwtUtill.
     * 2. Extracts the source (origin) of the request to ensure that tokens are generated for the correct client.
     * 3. Retrieves the user associated with the extracted mobile number from the database. If no user is found, a ResourceNotFoundException is thrown.
     * 4. Generates a new access token and refresh token for the user using the JwtUtill, passing in the user details and source.
     * 5. Returns a TokenRefreshResponseDTO containing the newly generated access token and refresh token.
     *
     * @param refreshToken The refresh token provided by the client for refreshing tokens.
     * @param request The HttpServletRequest object containing details of the incoming request.
     * @return A TokenRefreshResponseDTO containing the new access token and refresh token.
     * @throws ResourceNotFoundException If no user is found with the extracted mobile number from the refresh token.
     */
    // REFRESH TOKEN
    public RefreshTokenResponse refreshToken(String refreshToken, HttpServletRequest request) {
        boolean isGuest = jwtUtill.isGuestToken(refreshToken);
        String source = extractSource(request);
        String subject = jwtUtill.getSubject(refreshToken);

        if (!isGuest) {
            Long userId = Long.valueOf(subject);

            Users user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            return RefreshTokenResponse.builder()
                    .accessToken(jwtUtill.generateAccessToken(user, source))
                    .refreshToken(refreshToken)
                    .build();
        }

        return RefreshTokenResponse.builder()
                .accessToken(jwtUtill.generateAccessToken(null, source))
                .refreshToken(refreshToken)
                .build();
    }

    // GENERATE TOKEN
    public AuthResponse generateToken(String mobile, HttpServletRequest httpRequest) {
        Users user = userRepository.findByMobile(mobile)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Customer customer = customerRepository.findByUsers(user).orElse(null);
        String source = extractSource(httpRequest);

        String accessToken = jwtUtill.generateAccessToken(user, source);
        String refreshToken = jwtUtill.generateRefreshToken(user, source);

        return AuthResponse.builder()
                .userId(user.getId())
                .customerId(customer != null ? customer.getCustomerId() : null)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    // VALIDATE SALON ACCESS
    public void validateSalonAccess(Long salonId) {
        // Ensure the salon exists
        salonRepository.findById(salonId)
                .orElseThrow(() -> new ResourceNotFoundException("Salon not found with id: " + salonId));

        Users currentUser = getCurrentUser();
        if (currentUser == null) {
            throw new AccessDeniedException("No logged-in user found.");
        }

        // Admins can access any salon
        if (currentUser.getRole() == Role.ADMIN) {
            return;
        }

        // Partners can only access salons they are linked to
        if (currentUser.getRole() == Role.PARTNER) {
            PartnerDetails partner = partnerRepository.findByMobile(currentUser.getMobile())
                    .orElseThrow(() -> new ResourceNotFoundException("Partner not found with mobile: " + currentUser.getMobile()));

            boolean hasAccess = salonRepository.existsBySalonIdAndPartnerPartnerId(salonId, partner.getPartnerId());
            if (!hasAccess) {
                throw new AccessDeniedException(
                        String.format("Partner %d does not have permission to access salon %d", partner.getPartnerId(), salonId)
                );
            }
            return;
        }

        // Other roles are denied
        throw new AccessDeniedException("Access denied for role: " + currentUser.getRole());
    }

    // VALIDATE USER ACCESS
    public Users validateUserAccess(Long requestedUserId) {
        Users currentUser = getCurrentUser();

        if (currentUser == null) {
            throw new AccessDeniedException("No logged-in user found.");
        }

        // Admins can access any user's information
        if (currentUser.getRole() == Role.ADMIN) {
            return currentUser;
        }

        // Non-admins can only access their own information
        if (!currentUser.getId().equals(requestedUserId)) {
            throw new AccessDeniedException(
                    String.format("User %d is not authorized to access information for user %d",
                            currentUser.getId(), requestedUserId)
            );
        }

        return currentUser;
    }

    // VALIDATE CUSTOMER ACCESS
    public Customer validateCustomerAccess(Long requestedCustomerId) {
        Users currentUser = getCurrentUser();
        if (currentUser == null) {
            throw new AccessDeniedException("No logged-in user found.");
        }

        // Admins can access any customer by ID
        if (currentUser.getRole() == Role.ADMIN) {
            return customerRepository.findByUsers(currentUser)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            String.format("Customer not found with id: %d", requestedCustomerId)
                    ));
        }

        // For non-admins, ensure they have a customer profile
        Customer customer = customerRepository.findByUsers(currentUser)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format("Customer not found with id: %d", requestedCustomerId)
                ));

        // Check if the requested customer matches the logged-in customer
        if (!customer.getCustomerId().equals(requestedCustomerId)) {
            throw new AccessDeniedException("You are not authorized to access this customer's information.");
        }

        return customer;
    }

    // GET CURRENT USER
    public Users getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("No authenticated user found.");
        }

        String mobile = authentication.getName();
        return userRepository.findByMobile(mobile)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format("User not found with mobile: %s", mobile)
                ));
    }
}