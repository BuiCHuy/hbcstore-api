package com.hbcstore.hbcstore_api.notification;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findTop10ByTargetTypeOrderByCreatedAtDesc(Notification.TargetType targetType);

    List<Notification> findByTargetTypeAndReadAtIsNull(Notification.TargetType targetType);

    long countByTargetTypeAndReadAtIsNull(Notification.TargetType targetType);

    List<Notification> findTop10ByTargetTypeAndRecipient_IdOrderByCreatedAtDesc(
            Notification.TargetType targetType,
            Long recipientId
    );

    List<Notification> findByTargetTypeAndRecipient_IdAndReadAtIsNull(
            Notification.TargetType targetType,
            Long recipientId
    );

    long countByTargetTypeAndRecipient_IdAndReadAtIsNull(
            Notification.TargetType targetType,
            Long recipientId
    );
}
