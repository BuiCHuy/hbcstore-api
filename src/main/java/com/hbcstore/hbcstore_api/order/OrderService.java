package com.hbcstore.hbcstore_api.order;

import com.hbcstore.hbcstore_api.catalog.Product;
import com.hbcstore.hbcstore_api.catalog.ProductRepository;
import com.hbcstore.hbcstore_api.coupon.CouponRepository;
import com.hbcstore.hbcstore_api.coupon.Coupon;
import com.hbcstore.hbcstore_api.notification.AdminNotificationService;
import com.hbcstore.hbcstore_api.promotion.PromotionProduct;
import com.hbcstore.hbcstore_api.promotion.PromotionProductRepository;
import com.hbcstore.hbcstore_api.shipping.ShippingService;
import com.hbcstore.hbcstore_api.order.dto.CreateOrderItemRequest;
import com.hbcstore.hbcstore_api.order.dto.CreateOrderRequest;
import com.hbcstore.hbcstore_api.order.dto.OrderItemResponse;
import com.hbcstore.hbcstore_api.order.dto.OrderResponse;
import com.hbcstore.hbcstore_api.order.dto.OrderStatusRequest;
import com.hbcstore.hbcstore_api.user.User;
import com.hbcstore.hbcstore_api.user.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {
    private static final long BANK_TRANSFER_PAYMENT_EXPIRE_MINUTES = 15L;
    private static final DateTimeFormatter UTC_ISO_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    private final StoreOrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final CouponRepository couponRepository;
    private final PromotionProductRepository promotionProductRepository;
    private final ShippingService shippingService;
    private final AdminNotificationService adminNotificationService;

    public OrderService(
            StoreOrderRepository orderRepository,
            OrderDetailRepository orderDetailRepository,
            ProductRepository productRepository,
            UserRepository userRepository,
            CouponRepository couponRepository,
            PromotionProductRepository promotionProductRepository,
            ShippingService shippingService,
            AdminNotificationService adminNotificationService
    ) {
        this.orderRepository = orderRepository;
        this.orderDetailRepository = orderDetailRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.couponRepository = couponRepository;
        this.promotionProductRepository = promotionProductRepository;
        this.shippingService = shippingService;
        this.adminNotificationService = adminNotificationService;
    }

    public List<OrderResponse> getAll() {
        return orderRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public List<OrderResponse> getMine(String email) {
        if (email == null || email.isBlank()) {
            return List.of();
        }
        String normalizedEmail = email.trim().toLowerCase();
        return orderRepository
                .findByUserEmailIgnoreCaseOrGuestEmailIgnoreCaseOrderByOrderDateDesc(
                        normalizedEmail,
                        normalizedEmail
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public OrderResponse getById(Long id) {
        return toResponse(findOrder(id));
    }

    @Transactional
    public OrderResponse create(CreateOrderRequest request, String userEmail) {
        StoreOrder order = new StoreOrder();
        if (userEmail != null && !userEmail.isBlank()) {
            User user = userRepository.findByEmailIgnoreCase(userEmail)
                    .orElseThrow(() -> new IllegalArgumentException("Authenticated user not found"));
            if (user.getRole() == User.Role.ADMIN) {
                throw new IllegalArgumentException("Admin account cannot place orders");
            }
            order.setUser(user);
        }
        order.setGuestName(request.customerName());
        order.setGuestPhone(request.customerPhone());
        order.setGuestEmail(request.customerEmail());
        order.setShippingAddress(request.shippingAddress());
        order.setPaymentMethod(request.paymentMethod());
        if (request.couponId() != null) {
            var coupon = couponRepository.findById(request.couponId())
                    .orElseThrow(() -> new IllegalArgumentException("Coupon not found"));
            validateCouponPerUserLimit(order, coupon);
            validateAndConsumeCoupon(coupon);
            order.setCoupon(coupon);
        }
        order.setPaymentStatus(StoreOrder.PaymentStatus.UNPAID);
        order.setStatus(StoreOrder.OrderStatus.PENDING);
        if (request.paymentMethod() == StoreOrder.PaymentMethod.BANK_TRANSFER) {
            order.setPaymentExpiredAt(LocalDateTime.now().plusMinutes(BANK_TRANSFER_PAYMENT_EXPIRE_MINUTES));
        } else {
            order.setPaymentExpiredAt(null);
        }
        order.setDiscountAmount(request.discountAmount() == null ? BigDecimal.ZERO : request.discountAmount());

        BigDecimal subtotal = request.items().stream()
                .map(this::calculateItemTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        order.setShippingFee(shippingService.calculateFee(subtotal, order.getShippingAddress()));
        order.setSubtotalAmount(subtotal);
        order.setTotalAmount(subtotal.add(order.getShippingFee()).subtract(order.getDiscountAmount()));

        StoreOrder savedOrder = orderRepository.save(order);
        request.items().forEach(item -> saveOrderDetail(savedOrder, item));
        adminNotificationService.createNewOrderNotification(savedOrder);
        return toResponse(savedOrder);
    }

    private void validateAndConsumeCoupon(com.hbcstore.hbcstore_api.coupon.Coupon coupon) {
        LocalDateTime now = LocalDateTime.now();
        if (coupon.getStatus() != com.hbcstore.hbcstore_api.coupon.Coupon.CouponStatus.ACTIVE) {
            throw new IllegalArgumentException("Coupon is not active");
        }
        if (coupon.getStartDate() != null && coupon.getStartDate().isAfter(now)) {
            throw new IllegalArgumentException("Coupon is not active yet");
        }
        if (coupon.getEndDate() != null && coupon.getEndDate().isBefore(now)) {
            throw new IllegalArgumentException("Coupon has expired");
        }
        int usedCount = coupon.getUsedCount() == null ? 0 : coupon.getUsedCount();
        Integer usageLimit = coupon.getUsageLimit();
        if (usageLimit != null && usageLimit > 0 && usedCount >= usageLimit) {
            throw new IllegalArgumentException("Coupon usage limit reached");
        }
        coupon.setUsedCount(usedCount + 1);
    }

    private void validateCouponPerUserLimit(StoreOrder order, Coupon coupon) {
        if (order.getUser() == null || order.getUser().getId() == null || coupon == null || coupon.getId() == null) {
            return;
        }

        boolean alreadyUsed = orderRepository.existsCouponUsageByUserExcludingCancelled(
                order.getUser().getId(),
                coupon.getId(),
                StoreOrder.OrderStatus.CANCELLED
        );
        if (alreadyUsed) {
            throw new IllegalArgumentException("Each user can only use this coupon once");
        }
    }

    @Transactional
    public OrderResponse updateStatus(Long id, OrderStatusRequest request) {
        StoreOrder order = findOrder(id);
        validateStatusTransition(order.getStatus(), request.status());
        validatePaymentBeforeStatusTransition(order, request.status());

        if (request.status() == StoreOrder.OrderStatus.CANCELLED
                && order.getPaymentStatus() == StoreOrder.PaymentStatus.PAID) {
            throw new IllegalArgumentException("Cannot cancel a paid order");
        }

        if (request.status() == StoreOrder.OrderStatus.CANCELLED) {
            rollbackConsumptionOnCancel(order);
        }

        order.setStatus(request.status());

        if (request.status() == StoreOrder.OrderStatus.DELIVERED) {
            order.setPaymentStatus(StoreOrder.PaymentStatus.PAID);
            order.setPaymentExpiredAt(null);
        }

        StoreOrder saved = orderRepository.save(order);
        return toResponse(saved);
    }

    private void rollbackConsumptionOnCancel(StoreOrder order) {
        rollbackCouponUsage(order.getCoupon());
        rollbackPromotionSoldCount(order);
    }

    private void rollbackCouponUsage(Coupon coupon) {
        if (coupon == null) return;
        int usedCount = coupon.getUsedCount() == null ? 0 : coupon.getUsedCount();
        if (usedCount > 0) {
            coupon.setUsedCount(usedCount - 1);
        }
    }

    private void rollbackPromotionSoldCount(StoreOrder order) {
        List<OrderDetail> details = orderDetailRepository.findByOrderId(order.getId());
        for (OrderDetail detail : details) {
            BigDecimal currentBasePrice = detail.getProduct().getPrice();
            boolean hadPromotionDiscount = detail.getUnitPrice() != null
                    && currentBasePrice != null
                    && detail.getUnitPrice().compareTo(currentBasePrice) < 0;
            if (!hadPromotionDiscount) {
                continue;
            }

            List<PromotionProduct> targets = promotionProductRepository
                    .findMatchedByProductIdAtOrderDate(detail.getProduct().getId(), order.getOrderDate());
            if (targets.isEmpty()) {
                continue;
            }

            PromotionProduct target = targets.getFirst();
            int soldCount = target.getSoldCount() == null ? 0 : target.getSoldCount();
            int nextSoldCount = Math.max(0, soldCount - detail.getQuantity());
            target.setSoldCount(nextSoldCount);
            promotionProductRepository.save(target);
        }
    }

    private void validateStatusTransition(StoreOrder.OrderStatus current, StoreOrder.OrderStatus next) {
        if (current == null || next == null) {
            throw new IllegalArgumentException("Invalid order status");
        }
        if (current == next) {
            return;
        }

        EnumSet<StoreOrder.OrderStatus> allowedNextStatuses = switch (current) {
            case PENDING -> EnumSet.of(StoreOrder.OrderStatus.CONFIRMED, StoreOrder.OrderStatus.CANCELLED);
            case CONFIRMED -> EnumSet.of(StoreOrder.OrderStatus.SHIPPING);
            case SHIPPING -> EnumSet.of(StoreOrder.OrderStatus.DELIVERED);
            case DELIVERED, CANCELLED -> EnumSet.noneOf(StoreOrder.OrderStatus.class);
        };

        if (!allowedNextStatuses.contains(next)) {
            throw new IllegalArgumentException(
                    "Invalid status transition: " + current + " -> " + next
            );
        }
    }

    private void validatePaymentBeforeStatusTransition(StoreOrder order, StoreOrder.OrderStatus next) {
        if (order.getPaymentMethod() != StoreOrder.PaymentMethod.BANK_TRANSFER) {
            return;
        }
        if (next == StoreOrder.OrderStatus.CANCELLED) {
            return;
        }
        if (order.getPaymentStatus() != StoreOrder.PaymentStatus.PAID) {
            throw new IllegalArgumentException("Bank transfer order must be paid before processing");
        }
        if (order.getPaymentExpiredAt() != null && order.getPaymentExpiredAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Payment has expired");
        }
    }

    private StoreOrder findOrder(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
    }

    private BigDecimal calculateItemTotal(CreateOrderItemRequest item) {
        Product product = productRepository.findById(item.productId())
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));
        BigDecimal unitPrice = item.unitPrice() == null ? product.getPrice() : item.unitPrice();
        return unitPrice.multiply(BigDecimal.valueOf(item.quantity()));
    }

    private void saveOrderDetail(StoreOrder order, CreateOrderItemRequest item) {
        Product product = productRepository.findById(item.productId())
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));
        BigDecimal unitPrice = item.unitPrice() == null ? product.getPrice() : item.unitPrice();
        boolean hasPromotionDiscount = unitPrice.compareTo(product.getPrice()) < 0;
        if (hasPromotionDiscount) {
            consumePromotionStock(product.getId(), item.quantity());
        }

        OrderDetail detail = new OrderDetail();
        detail.setOrder(order);
        detail.setProduct(product);
        detail.setProductName(product.getName());
        detail.setProductImage(product.getThumbnailUrl());
        detail.setQuantity(item.quantity());
        detail.setUnitPrice(unitPrice);
        detail.setTotalPrice(unitPrice.multiply(BigDecimal.valueOf(item.quantity())));
        orderDetailRepository.save(detail);
    }

    private void consumePromotionStock(Long productId, int quantity) {
        List<PromotionProduct> targets = promotionProductRepository
                .findActiveByProductIdOrderByPriority(productId, LocalDateTime.now());

        if (targets.isEmpty()) {
            return;
        }

        PromotionProduct target = targets.getFirst();
        int soldCount = target.getSoldCount() == null ? 0 : target.getSoldCount();
        Integer stockLimit = target.getSaleStockLimit();

        if (stockLimit != null && stockLimit > 0) {
            int remaining = stockLimit - soldCount;
            if (remaining < quantity) {
                throw new IllegalArgumentException("Promotion sale stock limit reached");
            }
        }

        target.setSoldCount(soldCount + quantity);
        promotionProductRepository.save(target);
    }

    private OrderResponse toResponse(StoreOrder order) {
        List<OrderItemResponse> items = orderDetailRepository.findByOrderId(order.getId()).stream()
                .map(OrderItemResponse::from)
                .toList();
        int itemCount = items.stream()
                .mapToInt(OrderItemResponse::quantity)
                .sum();

        return new OrderResponse(
                order.getId(),
                order.getUser() == null ? null : order.getUser().getId(),
                order.getCoupon() == null ? null : order.getCoupon().getId(),
                order.getGuestName(),
                order.getGuestPhone(),
                order.getGuestEmail(),
                order.getShippingAddress(),
                order.getOrderDate(),
                order.getStatus(),
                order.getPaymentMethod(),
                order.getPaymentStatus(),
                toUtcIsoString(order.getPaymentExpiredAt()),
                order.getSubtotalAmount(),
                order.getShippingFee(),
                order.getDiscountAmount(),
                order.getTotalAmount(),
                itemCount,
                items
        );
    }

    private String toUtcIsoString(LocalDateTime dateTime) {
        if (dateTime == null) return null;
        return dateTime
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .atOffset(ZoneOffset.UTC)
                .format(UTC_ISO_FORMATTER);
    }
}
