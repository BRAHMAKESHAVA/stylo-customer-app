package org.backend.service;

import lombok.RequiredArgsConstructor;
import org.backend.dto.LogoutRequest;
import org.backend.dto.request.SaveFcmTokenRequest;
import org.backend.exception.ResourceNotFoundException;
import org.backend.model.Customer;
import org.backend.model.NotificationDevice;
import org.backend.model.Users;
import org.backend.repository.CustomerRepository;
import org.backend.repository.NotificationDeviceRepository;
import org.backend.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationDeviceService {

    private final NotificationDeviceRepository repository;
    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;

    public void saveToken(SaveFcmTokenRequest request) {
        Customer customer = getAuthenticatedCustomer();
        Users currentUser = customer.getUsers();
        NotificationDevice device = repository
                .findByUserIdAndFcmToken(customer.getCustomerId(), request.getFcmToken())
                .orElse(null);

        if (device == null) {
            device = NotificationDevice.builder()
                    .userId(customer.getCustomerId())
                    .userType(String.valueOf(currentUser.getRole())) // consider using an enum for consistency
                    .fcmToken(request.getFcmToken())
                    .deviceType(request.getDeviceType())
                    .deviceId(request.getDeviceId())
                    .isActive(true)
                    .lastSeenAt(LocalDateTime.now())
                    .build();
        } else {
            device.setLastSeenAt(LocalDateTime.now());
            device.setIsActive(true);
        }

        repository.save(device);
    }

    public void logoutDevice(LogoutRequest request) {
        Customer customer = getAuthenticatedCustomer();
        NotificationDevice device = repository.findByUserIdAndDeviceId(customer.getCustomerId(), request.getDeviceId())
                .orElseThrow(() -> new ResourceNotFoundException("Device not found"));
        device.setIsActive(false);
        device.setLastSeenAt(LocalDateTime.now());

        repository.save(device);
    }

    // HELPER METHOD
    private Customer getAuthenticatedCustomer() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String mobile = auth.getName();

        Users user = userRepository.findByMobile(mobile)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with mobile: " + mobile));

        return customerRepository.findByUsers(user)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
    }
}
