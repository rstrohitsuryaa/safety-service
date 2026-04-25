package com.buildsmart.safety.domain.repository;

import com.buildsmart.safety.domain.model.SafetyNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<SafetyNotification, String> {

    List<SafetyNotification> findByUserIdOrderByCreatedAtDesc(String userId);

    long countByUserIdAndIsReadFalse(String userId);

    Optional<SafetyNotification> findByNotificationId(String notificationId);

    @Modifying
    @Query("UPDATE SafetyNotification n SET n.isRead = true WHERE n.userId = :userId AND n.isRead = false")
    int markAllAsReadForUser(String userId);
}
