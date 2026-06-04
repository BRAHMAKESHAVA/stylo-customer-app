package org.backend.service;

import lombok.RequiredArgsConstructor;
import org.backend.dto.common.PageResponse;
import org.backend.dto.request.UserRegisterRequest;
import org.backend.dto.request.UserUpdateRequest;
import org.backend.dto.response.UserResponse;
import org.backend.enums.Role;
import org.backend.exception.BadRequestException;
import org.backend.exception.DuplicateResourceException;
import org.backend.exception.ResourceNotFoundException;
import org.backend.model.Customer;
import org.backend.model.Users;
import org.backend.repository.CustomerRepository;
import org.backend.repository.PartnerRepository;
import org.backend.repository.UserRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service class for managing user and customer operations.
 * Handles user registration, updates, and retrieval, including role-based validations.
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final PartnerRepository partnerRepository;
    private final AuthService authService;

    /**
     * Registers a new user based on the provided registration details.
     */
    // USER REGISTER
    public UserResponse userRegister(UserRegisterRequest registerUser) {
        if (userRepository.existsByMobile(registerUser.getMobile())) {
            throw new DuplicateResourceException(
                    "This mobile number is already registered. Please log in instead."
            );
        }

        if (registerUser.getRole() == Role.PARTNER) {
            if (!partnerRepository.existsByMobile(registerUser.getMobile())) {
                throw new BadRequestException(
                        "Only our onboard partners can register from this platform."
                );
            }
        }

        Users users = new Users();
        BeanUtils.copyProperties(registerUser, users);

        if (registerUser.getPassword() != null && !registerUser.getPassword().isEmpty()) {
            users.setPassword(passwordEncoder.encode(registerUser.getPassword()));
        }

        users.setAge(Integer.parseInt(registerUser.getAge()));
        users.setGender(registerUser.getGender().trim().toUpperCase());
        users = userRepository.save(users);

        if (registerUser.getRole() == Role.CUSTOMER) {
            customerRepository.save(Customer.builder().users(users).build());
        }

        UserResponse response = new UserResponse();
        BeanUtils.copyProperties(users, response);

        return response;
    }

    /**
     * Updates an existing user's information based on the provided user ID and update details.
     */
    // UPDATE USER
    public UserResponse updateUser(Long id, UserUpdateRequest user) {
        Users existing = authService.validateUserAccess(id);

        if (user.getFirstName() != null &&
                !user.getFirstName().isBlank()) {

            existing.setFirstName(user.getFirstName());
        }

        if (user.getLastName() != null &&
                !user.getLastName().isBlank()) {

            existing.setLastName(user.getLastName());
        }

        if (user.getGender() != null &&
                !user.getGender().isBlank()) {
            existing.setGender(user.getGender().trim().toUpperCase());
        }

        if (user.getAge() != null) {
            existing.setAge(user.getAge());
        }

        if (user.getEmail() != null) {
            existing.setEmail(user.getEmail());
        }

        Users updated = userRepository.save(existing);

        UserResponse response = new UserResponse();
        BeanUtils.copyProperties(updated, response);

        return response;
    }

    /**
     * Retrieves a user's information based on the provided user ID.
     */
    // GET USER BY ID
    public UserResponse getUserById(Long id) {
        Users user = authService.validateUserAccess(id);
        UserResponse response = new UserResponse();
        BeanUtils.copyProperties(user, response);

        return response;
    }

    /**
     * Retrieves a list of all users in the system.
     */
    // GET ALL USERS
    public PageResponse<UserResponse> getAllUsers(int page, int size) {

        if (page < 1) {
            throw new BadRequestException("Page number must be >= 1");
        }

        if (size < 1 || size >= 100) {
            throw new BadRequestException("Page size must be between 1 and 100");
        }

        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("id").descending());

        Page<Users> userPage = userRepository.findAll(pageable);

        List<UserResponse> content = userPage.getContent()
                .stream()
                .map(user -> {
                    UserResponse dto = new UserResponse();
                    BeanUtils.copyProperties(user, dto);
                    return dto;
                })
                .toList();

        return PageResponse.<UserResponse>builder()
                .page(page)
                .size(size)
                .totalElements(userPage.getTotalElements())
                .totalPages(userPage.getTotalPages())
                .last(userPage.isLast())
                .content(content)
                .build();
    }

    /**
     * Retrieves a customer's information based on the provided customer ID.
     */
    // GET CUSTOMER BY ID
    public Customer getCustomerById(Long id) {
        return authService.validateCustomerAccess(id);
    }

    //fetch all customers
    /**
     * Retrieves a list of all customers in the system.
     *
     * @return a list of DTOs containing information about all customers
     */
    // GET ALL CUSTOMERS
    public List<UserResponse> getAllCustomers(int page, int size) {

        // Business validation
        validatePagination(page, size);

        Pageable pageable = PageRequest.of(page - 1, size);

        Page<Customer> customerPage = customerRepository.findAll(pageable);

        return customerPage.getContent().stream().map(customer -> {
            UserResponse dto = new UserResponse();
            BeanUtils.copyProperties(customer.getUsers(), dto);
            return dto;
        }).toList();
    }

    private void validatePagination(int page, int size) {

        if (page < 1) {
            throw new BadRequestException("Page number must be >= 1");
        }

        if (size < 1 || size >= 100) {
            throw new BadRequestException("Page size must be between 1 and 100");
        }
    }
}