package com.hbcstore.hbcstore_api.refund;

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

    public RefundService(
            RefundRequestRepository refundRequestRepository,
            StoreOrderRepository orderRepository,
            UserRepository userRepository
    ) {
        this.refundRequestRepository = refundRequestRepository;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public RefundResponse create(String principalEmail, CreateRefundRequest request) {
        User user = findUser(principalEmail);
        StoreOrder order = orderRepository.findById(request.orderId())
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        if (order.getUser() == null || !order.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("You can only request refund for your own order");
        }
        if (order.getStatus() != StoreOrder.OrderStatus.DELIVERED) {
            throw new IllegalArgumentException("Only delivered orders can be refunded");
        }
        if (order.getPaymentStatus() != StoreOrder.PaymentStatus.PAID) {
            throw new IllegalArgumentException("Only paid orders can be refunded");
        }

        boolean hasOpenRefund = refundRequestRepository.existsByOrderIdAndStatusIn(
                order.getId(),
                List.of(RefundRequest.RefundStatus.PENDING, RefundRequest.RefundStatus.APPROVED)
        );
        if (hasOpenRefund) {
            throw new IllegalArgumentException("A refund request for this order is already being processed");
        }

        RefundRequest refund = new RefundRequest();
        refund.setOrder(order);
        refund.setUser(user);
        refund.setReason(request.reason().trim());
        refund.setRefundAmount(order.getTotalAmount());
        refund.setStatus(RefundRequest.RefundStatus.PENDING);

        return RefundResponse.from(refundRequestRepository.save(refund));
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
            throw new IllegalArgumentException("Only admin can access refund management");
        }
        return refundRequestRepository.findAll().stream()
                .map(RefundResponse::from)
                .toList();
    }

    @Transactional
    public RefundResponse updateStatus(Long id, String principalEmail, RefundStatusUpdateRequest request) {
        User admin = findUser(principalEmail);
        if (admin.getRole() != User.Role.ADMIN) {
            throw new IllegalArgumentException("Only admin can update refund status");
        }

        RefundRequest refund = refundRequestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Refund request not found"));

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

        return RefundResponse.from(refundRequestRepository.save(refund));
    }

    private User findUser(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Unauthenticated request");
        }
        return userRepository.findByEmail(email.trim())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    private void validateTransition(RefundRequest.RefundStatus current, RefundRequest.RefundStatus next) {
        if (current == next) return;
        boolean valid = switch (current) {
            case PENDING -> next == RefundRequest.RefundStatus.APPROVED || next == RefundRequest.RefundStatus.REJECTED;
            case APPROVED -> next == RefundRequest.RefundStatus.COMPLETED || next == RefundRequest.RefundStatus.REJECTED;
            case REJECTED, COMPLETED -> false;
        };
        if (!valid) {
            throw new IllegalArgumentException("Invalid refund status transition: " + current + " -> " + next);
        }
    }
}

