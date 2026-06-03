package com.hbcstore.hbcstore_api.cart;

import com.hbcstore.hbcstore_api.cart.dto.CartItemRequest;
import com.hbcstore.hbcstore_api.cart.dto.CartItemResponse;
import com.hbcstore.hbcstore_api.cart.dto.CartResponse;
import com.hbcstore.hbcstore_api.catalog.Product;
import com.hbcstore.hbcstore_api.catalog.ProductRepository;
import com.hbcstore.hbcstore_api.user.User;
import com.hbcstore.hbcstore_api.user.UserRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CartService {
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    public CartService(
            CartRepository cartRepository,
            CartItemRepository cartItemRepository,
            UserRepository userRepository,
            ProductRepository productRepository
    ) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public CartResponse getMyCart(String email) {
        User user = getActiveUser(email);
        Cart cart = cartRepository.findByUser(user).orElseGet(() -> createEmptyCart(user));
        return toResponse(cart);
    }

    @Transactional
    public CartResponse upsertItem(String email, CartItemRequest request) {
        User user = getActiveUser(email);
        Cart cart = cartRepository.findByUser(user).orElseGet(() -> createEmptyCart(user));
        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm"));

        CartItem item = cartItemRepository.findByCartAndProduct(cart, product)
                .orElseGet(() -> {
                    CartItem next = new CartItem();
                    next.setCart(cart);
                    next.setProduct(product);
                    return next;
                });

        item.setQuantity(Math.max(1, Math.min(10, request.quantity())));
        item.setUnitPriceSnapshot(product.getPrice());
        cartItemRepository.save(item);
        return toResponse(cartRepository.findById(cart.getId()).orElse(cart));
    }

    @Transactional
    public CartResponse removeItem(String email, Long productId) {
        User user = getActiveUser(email);
        Cart cart = cartRepository.findByUser(user).orElseGet(() -> createEmptyCart(user));
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm"));
        cartItemRepository.findByCartAndProduct(cart, product).ifPresent(cartItemRepository::delete);
        return toResponse(cartRepository.findById(cart.getId()).orElse(cart));
    }

    @Transactional
    public CartResponse clear(String email) {
        User user = getActiveUser(email);
        Cart cart = cartRepository.findByUser(user).orElseGet(() -> createEmptyCart(user));
        cartItemRepository.deleteAllByCart(cart);
        return toResponse(cart);
    }

    private User getActiveUser(String email) {
        return userRepository.findByEmail(email)
                .filter(u -> u.getStatus() == User.UserStatus.ACTIVE)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng"));
    }

    private Cart createEmptyCart(User user) {
        Cart cart = new Cart();
        cart.setUser(user);
        return cartRepository.save(cart);
    }

    private CartResponse toResponse(Cart cart) {
        List<CartItemResponse> items = cartItemRepository.findAllByCart(cart).stream().map(item -> new CartItemResponse(
                item.getId(),
                item.getProduct().getId(),
                item.getProduct().getName(),
                item.getProduct().getThumbnailUrl(),
                item.getProduct().getBrand() != null ? item.getProduct().getBrand().getName() : "",
                item.getProduct().getCategory() != null ? item.getProduct().getCategory().getName() : "",
                item.getUnitPriceSnapshot(),
                item.getQuantity()
        )).toList();
        return new CartResponse(cart.getId(), cart.getUser().getId(), items);
    }
}
