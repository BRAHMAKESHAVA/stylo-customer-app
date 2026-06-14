package org.backend.repository;

import org.backend.model.NotificationDevice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationDeviceRepository extends JpaRepository<NotificationDevice, Long> {

    Optional<NotificationDevice> findByUserIdAndFcmToken(Long userId, String fcmToken);

    List<NotificationDevice> findByUserIdAndActiveTrue(Long userId);

    Optional<NotificationDevice> findByFcmToken(String fcmToken);
}
