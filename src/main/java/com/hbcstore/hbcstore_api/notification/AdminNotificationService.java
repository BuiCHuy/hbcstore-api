package com.hbcstore.hbcstore_api.notification;

import com.hbcstore.hbcstore_api.notification.dto.AdminNotificationListResponse;
import com.hbcstore.hbcstore_api.notification.dto.AdminNotificationResponse;
import com.hbcstore.hbcstore_api.order.StoreOrder;
import com.hbcstore.hbcstore_api.user.User;
import com.hbcstore.hbcstore_api.user.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminNotificationService {
    private final AdminNotificationRepository adminNotificationRepository;
    private final UserRepository userRepository;

    public AdminNotificationService(
            AdminNotificationRepository adminNotificationRepository,
            UserRepository userRepository
    ) {
        this.adminNotificationRepository = adminNotificationRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void createNewOrderNotification(StoreOrder order) {
        AdminNotification notification = new AdminNotification();
        notification.setType(AdminNotification.NotificationType.NEW_ORDER);
        notification.setTitle("Có đơn hàng mới");
        notification.setMessage("%s vừa đặt %s.".formatted(
                order.getGuestName() == null || order.getGuestName().isBlank() ? "Khách hàng" : order.getGuestName(),
                "#HBC" + String.format("%06d", order.getId())
        ));
        notification.setOrder(order);
        adminNotificationRepository.save(notification);
    }

    @Transactional(readOnly = true)
    public AdminNotificationListResponse getLatest(String principalEmail) {
        requireAdmin(principalEmail);
        List<AdminNotificationResponse> notifications = adminNotificationRepository.findTop10ByOrderByCreatedAtDesc()
                .stream()
                .map(AdminNotificationResponse::from)
                .toList();
        return new AdminNotificationListResponse(
                adminNotificationRepository.countByReadAtIsNull(),
                notifications
        );
    }

    @Transactional
    public AdminNotificationListResponse markAllAsRead(String principalEmail) {
        requireAdmin(principalEmail);
        LocalDateTime now = LocalDateTime.now();
        adminNotificationRepository.findByReadAtIsNull().stream()
                .filter(notification -> notification.getReadAt() == null)
                .forEach(notification -> notification.setReadAt(now));
        return getLatest(principalEmail);
    }

    private void requireAdmin(String principalEmail) {
        User user = userRepository.findByEmailIgnoreCase(principalEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (user.getRole() != User.Role.ADMIN) {
            throw new IllegalArgumentException("Admin access required");
        }
    }
}
