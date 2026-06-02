package com.hbcstore.hbcstore_api.notification;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminNotificationRepository extends JpaRepository<AdminNotification, Long> {
    List<AdminNotification> findTop10ByOrderByCreatedAtDesc();

    List<AdminNotification> findByReadAtIsNull();

    long countByReadAtIsNull();
}
