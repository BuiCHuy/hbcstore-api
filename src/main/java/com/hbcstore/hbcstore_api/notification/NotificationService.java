package com.hbcstore.hbcstore_api.notification;

import com.hbcstore.hbcstore_api.notification.dto.NotificationListResponse;
import com.hbcstore.hbcstore_api.notification.dto.NotificationResponse;
import com.hbcstore.hbcstore_api.order.StoreOrder;
import com.hbcstore.hbcstore_api.refund.RefundRequest;
import com.hbcstore.hbcstore_api.user.User;
import com.hbcstore.hbcstore_api.user.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationService(
            NotificationRepository notificationRepository,
            UserRepository userRepository
    ) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void createNewOrderNotification(StoreOrder order) {
        Notification notification = new Notification();
        notification.setType(Notification.NotificationType.NEW_ORDER);
        notification.setTargetType(Notification.TargetType.ADMIN);
        notification.setTitle("Có đơn hàng mới");
        notification.setMessage("%s vừa đặt %s.".formatted(
                order.getGuestName() == null || order.getGuestName().isBlank() ? "Khách hàng" : order.getGuestName(),
                formatOrderCode(order.getId())
        ));
        notification.setLink("/admin/orders");
        notification.setOrder(order);
        notificationRepository.save(notification);
    }

    @Transactional
    public void createRefundRequestNotification(RefundRequest refundRequest) {
        Notification notification = new Notification();
        notification.setType(Notification.NotificationType.REFUND_REQUEST_CREATED);
        notification.setTargetType(Notification.TargetType.ADMIN);
        notification.setTitle("Có yêu cầu hoàn tiền mới");
        notification.setMessage("%s vừa gửi yêu cầu hoàn tiền cho %s.".formatted(
                refundRequest.getUser() != null && refundRequest.getUser().getFullName() != null
                        ? refundRequest.getUser().getFullName()
                        : "Khách hàng",
                formatOrderCode(refundRequest.getOrder().getId())
        ));
        notification.setLink("/admin/orders");
        notification.setOrder(refundRequest.getOrder());
        notification.setRefundRequest(refundRequest);
        notificationRepository.save(notification);
    }

    @Transactional
    public void createOrderStatusUpdatedNotification(StoreOrder order) {
        if (order.getUser() == null) {
            return;
        }
        Notification notification = new Notification();
        notification.setType(Notification.NotificationType.ORDER_STATUS_UPDATED);
        notification.setTargetType(Notification.TargetType.USER);
        notification.setRecipient(order.getUser());
        notification.setTitle("Đơn hàng được cập nhật");
        notification.setMessage("%s hiện ở trạng thái %s.".formatted(
                formatOrderCode(order.getId()),
                getOrderStatusLabel(order.getStatus())
        ));
        notification.setLink("/orders/" + order.getId());
        notification.setOrder(order);
        notificationRepository.save(notification);
    }

    @Transactional
    public void createRefundStatusUpdatedNotification(RefundRequest refundRequest) {
        if (refundRequest.getUser() == null || refundRequest.getStatus() == RefundRequest.RefundStatus.PENDING) {
            return;
        }
        Notification notification = new Notification();
        notification.setType(Notification.NotificationType.REFUND_STATUS_UPDATED);
        notification.setTargetType(Notification.TargetType.USER);
        notification.setRecipient(refundRequest.getUser());
        notification.setTitle(getRefundStatusTitle(refundRequest.getStatus()));
        notification.setMessage(getRefundStatusMessage(refundRequest));
        notification.setLink("/orders/" + refundRequest.getOrder().getId());
        notification.setOrder(refundRequest.getOrder());
        notification.setRefundRequest(refundRequest);
        notificationRepository.save(notification);
    }

    @Transactional(readOnly = true)
    public NotificationListResponse getLatestAdmin(String principalEmail) {
        requireAdmin(principalEmail);
        List<NotificationResponse> notifications = notificationRepository
                .findTop10ByTargetTypeOrderByCreatedAtDesc(Notification.TargetType.ADMIN)
                .stream()
                .map(NotificationResponse::from)
                .toList();
        return new NotificationListResponse(
                notificationRepository.countByTargetTypeAndReadAtIsNull(Notification.TargetType.ADMIN),
                notifications
        );
    }

    @Transactional(readOnly = true)
    public NotificationListResponse getLatestMine(String principalEmail) {
        User user = requireCustomer(principalEmail);
        List<NotificationResponse> notifications = notificationRepository
                .findTop10ByTargetTypeAndRecipient_IdOrderByCreatedAtDesc(Notification.TargetType.USER, user.getId())
                .stream()
                .map(NotificationResponse::from)
                .toList();
        return new NotificationListResponse(
                notificationRepository.countByTargetTypeAndRecipient_IdAndReadAtIsNull(Notification.TargetType.USER, user.getId()),
                notifications
        );
    }

    @Transactional
    public NotificationListResponse markAllAdminAsRead(String principalEmail) {
        requireAdmin(principalEmail);
        LocalDateTime now = LocalDateTime.now();
        notificationRepository.findByTargetTypeAndReadAtIsNull(Notification.TargetType.ADMIN)
                .forEach(notification -> notification.setReadAt(now));
        return getLatestAdmin(principalEmail);
    }

    @Transactional
    public NotificationListResponse markAllMineAsRead(String principalEmail) {
        User user = requireCustomer(principalEmail);
        LocalDateTime now = LocalDateTime.now();
        notificationRepository.findByTargetTypeAndRecipient_IdAndReadAtIsNull(Notification.TargetType.USER, user.getId())
                .forEach(notification -> notification.setReadAt(now));
        return getLatestMine(principalEmail);
    }

    private User requireAdmin(String principalEmail) {
        User user = findUser(principalEmail);
        if (user.getRole() != User.Role.ADMIN) {
            throw new IllegalArgumentException("Cần quyền quản trị để thực hiện thao tác này");
        }
        return user;
    }

    private User requireCustomer(String principalEmail) {
        User user = findUser(principalEmail);
        if (user.getRole() == User.Role.ADMIN) {
            throw new IllegalArgumentException("Tài khoản admin không dùng luồng thông báo khách hàng");
        }
        return user;
    }

    private User findUser(String principalEmail) {
        if (principalEmail == null || principalEmail.isBlank()) {
            throw new IllegalArgumentException("Yêu cầu chưa đăng nhập");
        }
        return userRepository.findByEmailIgnoreCase(principalEmail.trim())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng"));
    }

    private String formatOrderCode(Long orderId) {
        return "#HBC" + String.format("%06d", orderId);
    }

    private String getOrderStatusLabel(StoreOrder.OrderStatus status) {
        return switch (status) {
            case PENDING -> "đang xử lý";
            case CONFIRMED -> "đã xác nhận";
            case SHIPPING -> "đang giao";
            case DELIVERED -> "đã giao";
            case CANCELLED -> "đã hủy";
        };
    }

    private String getRefundStatusTitle(RefundRequest.RefundStatus status) {
        return switch (status) {
            case APPROVED -> "Yêu cầu hoàn tiền đã được duyệt";
            case REJECTED -> "Yêu cầu hoàn tiền bị từ chối";
            case COMPLETED -> "Hoàn tiền đã hoàn tất";
            case PENDING -> "Yêu cầu hoàn tiền được cập nhật";
        };
    }

    private String getRefundStatusMessage(RefundRequest refundRequest) {
        String orderCode = formatOrderCode(refundRequest.getOrder().getId());
        return switch (refundRequest.getStatus()) {
            case APPROVED -> "%s đã được duyệt yêu cầu hoàn tiền.".formatted(orderCode);
            case REJECTED -> "%s đã bị từ chối yêu cầu hoàn tiền.".formatted(orderCode);
            case COMPLETED -> "%s đã được hoàn tiền thành công.".formatted(orderCode);
            case PENDING -> "%s có trạng thái hoàn tiền: chờ duyệt.".formatted(orderCode);
        };
    }
}
