package com.hbcstore.hbcstore_api.refund;

import com.hbcstore.hbcstore_api.notification.NotificationService;
import com.hbcstore.hbcstore_api.order.StoreOrder;
import com.hbcstore.hbcstore_api.order.StoreOrderRepository;
import com.hbcstore.hbcstore_api.refund.dto.CreateRefundRequest;
import com.hbcstore.hbcstore_api.refund.dto.RefundResponse;
import com.hbcstore.hbcstore_api.refund.dto.RefundStatusUpdateRequest;
import com.hbcstore.hbcstore_api.user.User;
import com.hbcstore.hbcstore_api.user.UserRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefundService {
    private final RefundRequestRepository refundRequestRepository;
    private final StoreOrderRepository orderRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public RefundService(
            RefundRequestRepository refundRequestRepository,
            StoreOrderRepository orderRepository,
            UserRepository userRepository,
            NotificationService notificationService
    ) {
        this.refundRequestRepository = refundRequestRepository;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public RefundResponse create(String principalEmail, CreateRefundRequest request) {
        User user = findUser(principalEmail);
        StoreOrder order = orderRepository.findById(request.orderId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng"));

        if (order.getUser() == null || !order.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Bạn chỉ có thể yêu cầu hoàn tiền cho đơn hàng của chính mình");
        }
        if (order.getStatus() != StoreOrder.OrderStatus.DELIVERED) {
            throw new IllegalArgumentException("Chỉ đơn hàng đã giao mới được yêu cầu hoàn tiền");
        }
        if (order.getPaymentStatus() != StoreOrder.PaymentStatus.PAID) {
            throw new IllegalArgumentException("Chỉ đơn hàng đã thanh toán mới được yêu cầu hoàn tiền");
        }

        boolean hasOpenRefund = refundRequestRepository.existsByOrderIdAndStatusIn(
                order.getId(),
                List.of(RefundRequest.RefundStatus.PENDING, RefundRequest.RefundStatus.APPROVED)
        );
        if (hasOpenRefund) {
            throw new IllegalArgumentException("Đơn hàng này đã có yêu cầu hoàn tiền đang được xử lý");
        }

        RefundRequest refund = new RefundRequest();
        refund.setOrder(order);
        refund.setUser(user);
        refund.setReason(request.reason().trim());
        refund.setRefundAmount(order.getTotalAmount());
        refund.setStatus(RefundRequest.RefundStatus.PENDING);

        RefundRequest saved = refundRequestRepository.save(refund);
        notificationService.createRefundRequestNotification(saved);
        return RefundResponse.from(saved);
    }

    public List<RefundResponse> getMine(String principalEmail) {
        if (principalEmail == null || principalEmail.isBlank()) return List.of();
        return refundRequestRepository.findByUser_EmailIgnoreCaseOrderByCreatedAtDesc(principalEmail.trim())
                .stream()
                .map(RefundResponse::from)
                .toList();
    }

    public List<RefundResponse> getAllAdmin(String principalEmail) {
        User admin = findUser(principalEmail);
        if (admin.getRole() != User.Role.ADMIN) {
            throw new IllegalArgumentException("Chỉ quản trị viên mới có thể quản lý hoàn tiền");
        }
        return refundRequestRepository.findAll().stream()
                .map(RefundResponse::from)
                .toList();
    }

    @Transactional
    public RefundResponse updateStatus(Long id, String principalEmail, RefundStatusUpdateRequest request) {
        User admin = findUser(principalEmail);
        if (admin.getRole() != User.Role.ADMIN) {
            throw new IllegalArgumentException("Chỉ quản trị viên mới có thể cập nhật trạng thái hoàn tiền");
        }

        RefundRequest refund = refundRequestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy yêu cầu hoàn tiền"));

        validateTransition(refund.getStatus(), request.status());
        refund.setStatus(request.status());
        refund.setAdminNote(request.adminNote());
        refund.setProcessedBy(admin);
        refund.setProcessedAt(java.time.LocalDateTime.now());

        if (request.status() == RefundRequest.RefundStatus.COMPLETED) {
            StoreOrder order = refund.getOrder();
            order.setPaymentStatus(StoreOrder.PaymentStatus.REFUNDED);
            orderRepository.save(order);
        }

        RefundRequest saved = refundRequestRepository.save(refund);
        notificationService.createRefundStatusUpdatedNotification(saved);
        return RefundResponse.from(saved);
    }

    private User findUser(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Yêu cầu chưa đăng nhập");
        }
        return userRepository.findByEmail(email.trim())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng"));
    }

    private void validateTransition(RefundRequest.RefundStatus current, RefundRequest.RefundStatus next) {
        if (current == next) return;
        boolean valid = switch (current) {
            case PENDING -> next == RefundRequest.RefundStatus.APPROVED || next == RefundRequest.RefundStatus.REJECTED;
            case APPROVED -> next == RefundRequest.RefundStatus.COMPLETED || next == RefundRequest.RefundStatus.REJECTED;
            case REJECTED, COMPLETED -> false;
        };
        if (!valid) {
            throw new IllegalArgumentException("Chuyển trạng thái hoàn tiền không hợp lệ: " + current + " -> " + next);
        }
    }
}
