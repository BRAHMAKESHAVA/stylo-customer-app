package org.backend.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.backend.dto.common.PageResponse;
import org.backend.dto.user.request.UserRegisterRequestDTO;
import org.backend.dto.user.request.UserUpdateRequestDTO;
import org.backend.dto.user.response.UserRegisterResponseDTO;
import org.backend.enums.Role;
import org.backend.exception.BadRequestException;
import org.backend.exception.DuplicateResourceException;
import org.backend.exception.ResourceNotFoundException;
import org.backend.model.Customer;
import org.backend.model.Users;
import org.backend.repository.CustomerRepository;
import org.backend.repository.PartnerRepository;
import org.backend.repository.UserRepository;
import org.backend.utill.JwtUtill;
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
    private final JwtUtill jwtUtill;
    private final HttpServletRequest httpRequest;
    private final AuthService authService;

    /**
     * Registers a new user based on the provided registration details.
     */
    // USER REGISTER
    public UserRegisterResponseDTO userRegister(UserRegisterRequestDTO registerUser) {
        if (userRepository.existsByMobile(registerUser.getMobile())) {
            throw new DuplicateResourceException(
                    "This mobile number is already registered. Please log in instead."
            );
        }

        if (registerUser.getRole() == Role.PARTNER) {
            boolean partnerExists =
                    partnerRepository.existsByMobile(registerUser.getMobile());

            if (!partnerExists) {
                throw new BadRequestException(
                        "Only our onboard partners can register from this platform."
                );
            }
        }

        Users users = new Users();
        BeanUtils.copyProperties(registerUser, users);

        if (registerUser.getPassword() != null &&
                !registerUser.getPassword().isEmpty()) {

            users.setPassword(
                    passwordEncoder.encode(registerUser.getPassword())
            );
        }

        users.setAge(Integer.parseInt(registerUser.getAge()));
        users = userRepository.save(users);

        if (registerUser.getRole() == Role.CUSTOMER) {
            customerRepository.save(
                    Customer.builder().users(users).build()
            );
        }

        UserRegisterResponseDTO response = new UserRegisterResponseDTO();
        BeanUtils.copyProperties(users, response);

        return response;
    }

    /**
     * Updates an existing user's information based on the provided user ID and update details.
     */
    // UPDATE USER
    public UserRegisterResponseDTO updateUser(Long id, UserUpdateRequestDTO user) {
        Users existing = userRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found with id: " + id
                        )
                );

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

            existing.setGender(user.getGender());
        }

        if (user.getAge() != null) {
            existing.setAge(user.getAge());
        }

        if (user.getEmail() != null) {
            existing.setEmail(user.getEmail());
        }

        Users updated = userRepository.save(existing);

        UserRegisterResponseDTO response = new UserRegisterResponseDTO();
        BeanUtils.copyProperties(updated, response);

        return response;
    }

    /**
     * Retrieves a user's information based on the provided user ID.
     */
    // GET USER BY ID
    public UserRegisterResponseDTO getUserById(Long id) {
        Users user = userRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found with id: " + id
                        )
                );

        UserRegisterResponseDTO response = new UserRegisterResponseDTO();
        BeanUtils.copyProperties(user, response);

        return response;
    }

    /**
     * Retrieves a list of all users in the system.
     */
    // GET ALL USERS
    public PageResponse<UserRegisterResponseDTO> getAllUsers(int page, int size) {

        if (page < 1) {
            throw new BadRequestException("Page number must be >= 1");
        }

        if (size < 1 || size >= 100) {
            throw new BadRequestException("Page size must be between 1 and 100");
        }

        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("userId").descending());

        Page<Users> userPage = userRepository.findAll(pageable);

        List<UserRegisterResponseDTO> content = userPage.getContent()
                .stream()
                .map(user -> {
                    UserRegisterResponseDTO dto = new UserRegisterResponseDTO();
                    BeanUtils.copyProperties(user, dto);
                    return dto;
                })
                .toList();

        return PageResponse.<UserRegisterResponseDTO>builder()
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
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Customer not found with id: " + id
                        )
                );

        //Users user = customer.getUsers();
        //UserRegisterResponseDTO response = new UserRegisterResponseDTO();
        //BeanUtils.copyProperties(user, response);

        return customer;
    }

    //fetch all customers
    /**
     * Retrieves a list of all customers in the system.
     *
     * @return a list of DTOs containing information about all customers
     */
    // GET ALL CUSTOMERS
    public List<UserRegisterResponseDTO> getAllCustomers(int page, int size) {

        // 🔥 Business validation
        validatePagination(page, size);

        Pageable pageable = PageRequest.of(page - 1, size);

        Page<Customer> customerPage = customerRepository.findAll(pageable);

        return customerPage.getContent().stream().map(customer -> {
            UserRegisterResponseDTO dto = new UserRegisterResponseDTO();
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