package com.hbcstore.hbcstore_api.order;

import com.hbcstore.hbcstore_api.catalog.Product;
import com.hbcstore.hbcstore_api.catalog.ProductRepository;
import com.hbcstore.hbcstore_api.coupon.CouponRepository;
import com.hbcstore.hbcstore_api.coupon.Coupon;
import com.hbcstore.hbcstore_api.notification.NotificationService;
import com.hbcstore.hbcstore_api.pricing.ProductPriceSnapshot;
import com.hbcstore.hbcstore_api.pricing.PricingService;
import com.hbcstore.hbcstore_api.promotion.PromotionProduct;
import com.hbcstore.hbcstore_api.promotion.PromotionProductRepository;
import com.hbcstore.hbcstore_api.shipping.ShippingService;
import com.hbcstore.hbcstore_api.order.dto.CreateOrderItemRequest;
import com.hbcstore.hbcstore_api.order.dto.CreateOrderRequest;
import com.hbcstore.hbcstore_api.order.dto.OrderQuoteItemResponse;
import com.hbcstore.hbcstore_api.order.dto.OrderQuoteResponse;
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
    private final NotificationService notificationService;
    private final PricingService pricingService;

    public OrderService(
            StoreOrderRepository orderRepository,
            OrderDetailRepository orderDetailRepository,
            ProductRepository productRepository,
            UserRepository userRepository,
            CouponRepository couponRepository,
            PromotionProductRepository promotionProductRepository,
            ShippingService shippingService,
            NotificationService notificationService,
            PricingService pricingService
    ) {
        this.orderRepository = orderRepository;
        this.orderDetailRepository = orderDetailRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.couponRepository = couponRepository;
        this.promotionProductRepository = promotionProductRepository;
        this.shippingService = shippingService;
        this.notificationService = notificationService;
        this.pricingService = pricingService;
    }

    public List<OrderResponse> getAll(String principalEmail) {
        requireAdmin(principalEmail);
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

    public OrderResponse getById(Long id, String principalEmail) {
        StoreOrder order = findOrder(id);
        validateOrderAccess(order, principalEmail);
        return toResponse(order);
    }

    @Transactional
    public OrderResponse create(CreateOrderRequest request, String userEmail) {
        User user = requireCustomer(userEmail);
        ResolvedOrderQuote quote = resolveQuote(request, user);
        StoreOrder order = new StoreOrder();
        order.setUser(user);
        order.setGuestName(request.customerName());
        order.setGuestPhone(request.customerPhone());
        order.setGuestEmail(request.customerEmail());
        order.setShippingAddress(request.shippingAddress());
        order.setPaymentMethod(request.paymentMethod());
        if (quote.coupon() != null) {
            validateAndConsumeCoupon(quote.coupon());
            order.setCoupon(quote.coupon());
        }
        order.setPaymentStatus(StoreOrder.PaymentStatus.UNPAID);
        order.setStatus(StoreOrder.OrderStatus.PENDING);
        if (request.paymentMethod() == StoreOrder.PaymentMethod.BANK_TRANSFER) {
            order.setPaymentExpiredAt(LocalDateTime.now().plusMinutes(BANK_TRANSFER_PAYMENT_EXPIRE_MINUTES));
        } else {
            order.setPaymentExpiredAt(null);
        }
        order.setDiscountAmount(quote.discountAmount());
        order.setShippingFee(quote.shippingFee());
        order.setSubtotalAmount(quote.subtotalAmount());
        order.setTotalAmount(quote.totalAmount());

        StoreOrder savedOrder = orderRepository.save(order);
        quote.items().forEach(item -> saveOrderDetail(savedOrder, item));
        notificationService.createNewOrderNotification(savedOrder);
        return toResponse(savedOrder);
    }

    @Transactional(readOnly = true)
    public OrderQuoteResponse quote(CreateOrderRequest request, String userEmail) {
        User user = requireCustomer(userEmail);
        ResolvedOrderQuote quote = resolveQuote(request, user);
        return new OrderQuoteResponse(
                quote.items().stream()
                        .map(item -> new OrderQuoteItemResponse(
                                item.product().getId(),
                                item.product().getName(),
                                item.quantity(),
                                item.unitPrice(),
                                item.totalPrice()
                        ))
                        .toList(),
                quote.coupon() == null ? null : quote.coupon().getId(),
                quote.coupon() == null ? null : quote.coupon().getCode(),
                quote.subtotalAmount(),
                quote.shippingFee(),
                quote.discountAmount(),
                quote.totalAmount()
        );
    }

    private void validateAndConsumeCoupon(com.hbcstore.hbcstore_api.coupon.Coupon coupon) {
        LocalDateTime now = LocalDateTime.now();
        if (coupon.getStatus() != com.hbcstore.hbcstore_api.coupon.Coupon.CouponStatus.ACTIVE) {
            throw new IllegalArgumentException("Mã giảm giá hiện không hoạt động");
        }
        if (coupon.getStartDate() != null && coupon.getStartDate().isAfter(now)) {
            throw new IllegalArgumentException("Mã giảm giá chưa đến thời gian áp dụng");
        }
        if (coupon.getEndDate() != null && coupon.getEndDate().isBefore(now)) {
            throw new IllegalArgumentException("Mã giảm giá đã hết hạn");
        }
        int usedCount = coupon.getUsedCount() == null ? 0 : coupon.getUsedCount();
        Integer usageLimit = coupon.getUsageLimit();
        if (usageLimit != null && usageLimit > 0 && usedCount >= usageLimit) {
            throw new IllegalArgumentException("Mã giảm giá đã hết lượt sử dụng");
        }
        coupon.setUsedCount(usedCount + 1);
    }

    @Transactional
    public OrderResponse updateStatus(Long id, OrderStatusRequest request, String principalEmail) {
        requireAdmin(principalEmail);
        StoreOrder order = findOrder(id);
        validateStatusTransition(order.getStatus(), request.status());
        validatePaymentBeforeStatusTransition(order, request.status());

        if (request.status() == StoreOrder.OrderStatus.CANCELLED
                && order.getPaymentStatus() == StoreOrder.PaymentStatus.PAID) {
            throw new IllegalArgumentException("Không thể hủy đơn hàng đã thanh toán");
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
        notificationService.createOrderStatusUpdatedNotification(saved);
        return toResponse(saved);
    }

    @Transactional
    public OrderResponse cancelMine(Long id, String principalEmail) {
        User user = requireCustomer(principalEmail);
        StoreOrder order = findOrder(id);
        if (order.getUser() == null || !order.getUser().getId().equals(user.getId())) {
            throw new SecurityException("Bạn không có quyền hủy đơn hàng này");
        }
        if (order.getStatus() != StoreOrder.OrderStatus.PENDING) {
            throw new IllegalArgumentException("Chỉ có thể hủy đơn ở trạng thái chờ xử lý");
        }
        if (order.getPaymentStatus() == StoreOrder.PaymentStatus.PAID) {
            throw new IllegalArgumentException("Không thể hủy đơn đã thanh toán");
        }
        rollbackConsumptionOnCancel(order);
        order.setStatus(StoreOrder.OrderStatus.CANCELLED);
        StoreOrder saved = orderRepository.save(order);
        notificationService.createOrderStatusUpdatedNotification(saved);
        return toResponse(saved);
    }

    private void rollbackConsumptionOnCancel(StoreOrder order) {
        rollbackCouponUsage(order.getCoupon());
        rollbackPromotionSoldCount(order);
        rollbackProductStock(order);
    }

    private void rollbackProductStock(StoreOrder order) {
        List<OrderDetail> details = orderDetailRepository.findByOrderId(order.getId());
        for (OrderDetail detail : details) {
            Product product = detail.getProduct();
            int currentStock = product.getStockQuantity() == null ? 0 : product.getStockQuantity();
            product.setStockQuantity(currentStock + detail.getQuantity());
            productRepository.save(product);
        }
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
            throw new IllegalArgumentException("Trạng thái đơn hàng không hợp lệ");
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
                    "Chuyển trạng thái đơn hàng không hợp lệ: " + current + " -> " + next
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
            throw new IllegalArgumentException("Đơn chuyển khoản phải được thanh toán trước khi xử lý");
        }
        if (order.getPaymentExpiredAt() != null && order.getPaymentExpiredAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Thanh toán đã hết hạn");
        }
    }

    private StoreOrder findOrder(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng"));
    }

    private void saveOrderDetail(StoreOrder order, ResolvedOrderItem item) {
        Product product = item.product();
        
        int currentStock = product.getStockQuantity() == null ? 0 : product.getStockQuantity();
        product.setStockQuantity(currentStock - item.quantity());
        productRepository.save(product);

        if (item.priceSnapshot().usesPromotionStock()) {
            consumePromotionStock(product.getId(), item.quantity());
        }

        OrderDetail detail = new OrderDetail();
        detail.setOrder(order);
        detail.setProduct(product);
        detail.setProductName(product.getName());
        detail.setProductImage(product.getThumbnailUrl());
        detail.setQuantity(item.quantity());
        detail.setUnitPrice(item.unitPrice());
        detail.setTotalPrice(item.totalPrice());
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
                throw new IllegalArgumentException("Số lượng khuyến mãi còn lại không đủ");
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

    private ResolvedOrderQuote resolveQuote(CreateOrderRequest request, User user) {
        List<ResolvedOrderItem> items = request.items().stream()
                .map(this::resolveOrderItem)
                .toList();

        BigDecimal subtotal = items.stream()
                .map(ResolvedOrderItem::totalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Coupon coupon = resolveCoupon(request);
        if (coupon != null) {
            validateCouponPerUserLimit(user, coupon);
            validateCouponForCheckout(coupon, subtotal);
        }

        BigDecimal shippingFee = shippingService.calculateFee(subtotal, request.shippingAddress());
        BigDecimal discountAmount = pricingService.calculateCouponDiscount(coupon, subtotal);
        BigDecimal totalAmount = subtotal.add(shippingFee).subtract(discountAmount);

        return new ResolvedOrderQuote(items, coupon, subtotal, shippingFee, discountAmount, totalAmount);
    }

    private ResolvedOrderItem resolveOrderItem(CreateOrderItemRequest item) {
        Product product = productRepository.findById(item.productId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm"));
        if (product.getStatus() == Product.ProductStatus.INACTIVE) {
            throw new IllegalArgumentException("Sản phẩm hiện đang bị ẩn");
        }
        int stock = product.getStockQuantity() == null ? 0 : product.getStockQuantity();
        if (stock < item.quantity()) {
            throw new IllegalArgumentException("Sản phẩm không đủ tồn kho: " + product.getName());
        }
        ProductPriceSnapshot snapshot = pricingService.resolveProductPrice(product, item.quantity());
        BigDecimal unitPrice = snapshot.unitPrice() == null ? product.getPrice() : snapshot.unitPrice();
        BigDecimal totalPrice = unitPrice.multiply(BigDecimal.valueOf(item.quantity()));
        return new ResolvedOrderItem(product, item.quantity(), unitPrice, totalPrice, snapshot);
    }

    private Coupon resolveCoupon(CreateOrderRequest request) {
        if (request.couponCode() != null && !request.couponCode().isBlank()) {
            return couponRepository.findByCodeIgnoreCase(request.couponCode().trim())
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy mã giảm giá"));
        }
        if (request.couponId() != null) {
            return couponRepository.findById(request.couponId())
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy mã giảm giá"));
        }
        return null;
    }

    private void validateCouponForCheckout(Coupon coupon, BigDecimal subtotal) {
        LocalDateTime now = LocalDateTime.now();
        if (coupon.getStatus() != Coupon.CouponStatus.ACTIVE) {
            throw new IllegalArgumentException("Mã giảm giá hiện không hoạt động");
        }
        if (coupon.getStartDate() != null && coupon.getStartDate().isAfter(now)) {
            throw new IllegalArgumentException("Mã giảm giá chưa đến thời gian áp dụng");
        }
        if (coupon.getEndDate() != null && coupon.getEndDate().isBefore(now)) {
            throw new IllegalArgumentException("Mã giảm giá đã hết hạn");
        }
        int usedCount = coupon.getUsedCount() == null ? 0 : coupon.getUsedCount();
        Integer usageLimit = coupon.getUsageLimit();
        if (usageLimit != null && usageLimit > 0 && usedCount >= usageLimit) {
            throw new IllegalArgumentException("Mã giảm giá đã hết lượt sử dụng");
        }
        if (coupon.getMinOrderValue() != null && subtotal.compareTo(coupon.getMinOrderValue()) < 0) {
            throw new IllegalArgumentException("Đơn hàng chưa đạt giá trị tối thiểu để dùng mã giảm giá");
        }
    }

    private void validateCouponPerUserLimit(User user, Coupon coupon) {
        if (user == null || user.getId() == null || coupon == null || coupon.getId() == null) {
            return;
        }

        boolean alreadyUsed = orderRepository.existsCouponUsageByUserExcludingCancelled(
                user.getId(),
                coupon.getId(),
                StoreOrder.OrderStatus.CANCELLED
        );
        if (alreadyUsed) {
            throw new IllegalArgumentException("Mỗi người dùng chỉ được sử dụng mã giảm giá này một lần");
        }
    }

    private User requireCustomer(String principalEmail) {
        if (principalEmail == null || principalEmail.isBlank()) {
            throw new SecurityException("Bạn cần đăng nhập để thực hiện thao tác này");
        }
        User user = userRepository.findByEmailIgnoreCase(principalEmail.trim())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản đăng nhập"));
        if (user.getRole() == User.Role.ADMIN) {
            throw new SecurityException("Tài khoản admin không thể thực hiện thao tác này");
        }
        if (user.getStatus() != User.UserStatus.ACTIVE) {
            throw new SecurityException("Tài khoản của bạn không hoạt động");
        }
        return user;
    }

    private void requireAdmin(String principalEmail) {
        if (principalEmail == null || principalEmail.isBlank()) {
            throw new SecurityException("Bạn chưa đăng nhập");
        }
        User user = userRepository.findByEmailIgnoreCase(principalEmail.trim())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng"));
        if (user.getRole() != User.Role.ADMIN) {
            throw new SecurityException("Bạn không có quyền thực hiện thao tác này");
        }
    }

    private void validateOrderAccess(StoreOrder order, String principalEmail) {
        if (principalEmail == null || principalEmail.isBlank()) {
            throw new SecurityException("Bạn chưa đăng nhập");
        }
        User user = userRepository.findByEmailIgnoreCase(principalEmail.trim())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng"));
        if (user.getRole() == User.Role.ADMIN) {
            return;
        }
        if (order.getUser() == null || !order.getUser().getId().equals(user.getId())) {
            throw new SecurityException("Bạn không có quyền xem đơn hàng này");
        }
    }

    private record ResolvedOrderItem(
            Product product,
            int quantity,
            BigDecimal unitPrice,
            BigDecimal totalPrice,
            ProductPriceSnapshot priceSnapshot
    ) {
    }

    private record ResolvedOrderQuote(
            List<ResolvedOrderItem> items,
            Coupon coupon,
            BigDecimal subtotalAmount,
            BigDecimal shippingFee,
            BigDecimal discountAmount,
            BigDecimal totalAmount
    ) {
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
