package com.hbcstore.hbcstore_api.notification;

import com.hbcstore.hbcstore_api.order.StoreOrder;
import com.hbcstore.hbcstore_api.refund.RefundRequest;
import com.hbcstore.hbcstore_api.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "notifications")
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private NotificationType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TargetType targetType;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(length = 255)
    private String link;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_user_id")
    private User recipient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private StoreOrder order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "refund_request_id")
    private RefundRequest refundRequest;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime readAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public enum NotificationType {
        NEW_ORDER,
        REFUND_REQUEST_CREATED,
        ORDER_STATUS_UPDATED,
        REFUND_STATUS_UPDATED
    }

    public enum TargetType {
        ADMIN,
        USER
    }
}
