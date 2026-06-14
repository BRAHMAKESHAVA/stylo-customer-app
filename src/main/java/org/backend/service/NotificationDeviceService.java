package org.backend.service;

import lombok.RequiredArgsConstructor;
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
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String mobile = authentication.getName();

        Users currentUser =  userRepository.findByMobile(mobile)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format("User not found with mobile: %s", mobile)
                ));

        Customer customer = customerRepository.findByUsers(currentUser)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format("Customer not found")
                ));


        NotificationDevice device = repository
                .findByUserIdAndFcmToken(customer.getCustomerId(), request.getFcmToken())
                .orElse(null);

        if (device == null) {
            device = NotificationDevice.builder()
                    .userId(customer.getCustomerId())
                    .userType(String.valueOf(currentUser.getRole())) // consider using an enum for consistency
                    .fcmToken(request.getFcmToken())
                    .deviceType(request.getDeviceType())
                    .active(true)
                    .lastSeenAt(LocalDateTime.now())
                    .build();
        } else {
            device.setLastSeenAt(LocalDateTime.now());
            device.setActive(true);
        }

        repository.save(device);
    }
}
