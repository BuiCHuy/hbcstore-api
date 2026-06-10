CREATE TABLE IF NOT EXISTS notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    type VARCHAR(40) NOT NULL,
    target_type VARCHAR(20) NOT NULL,
    title VARCHAR(200) NOT NULL,
    message TEXT NOT NULL,
    link VARCHAR(255),
    recipient_user_id BIGINT NULL,
    order_id BIGINT NULL,
    refund_request_id BIGINT NULL,
    created_at DATETIME NOT NULL,
    read_at DATETIME NULL,
    CONSTRAINT fk_notifications_recipient_user
        FOREIGN KEY (recipient_user_id) REFERENCES users(id),
    CONSTRAINT fk_notifications_order
        FOREIGN KEY (order_id) REFERENCES orders(id),
    CONSTRAINT fk_notifications_refund_request
        FOREIGN KEY (refund_request_id) REFERENCES refund_requests(id)
);

INSERT INTO notifications (
    type,
    target_type,
    title,
    message,
    link,
    recipient_user_id,
    order_id,
    refund_request_id,
    created_at,
    read_at
)
SELECT
    type,
    'ADMIN',
    title,
    message,
    '/admin/orders',
    NULL,
    order_id,
    NULL,
    created_at,
    read_at
FROM admin_notifications
WHERE NOT EXISTS (
    SELECT 1
    FROM notifications n
    WHERE n.target_type = 'ADMIN'
      AND n.type = admin_notifications.type
      AND (n.order_id <=> admin_notifications.order_id)
      AND n.created_at = admin_notifications.created_at
);

-- Sau khi kiểm tra dữ liệu đã chuyển xong, có thể xóa bảng cũ nếu muốn:
-- DROP TABLE admin_notifications;
