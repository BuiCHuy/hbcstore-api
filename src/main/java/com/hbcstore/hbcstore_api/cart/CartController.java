package com.hbcstore.hbcstore_api.cart;

import com.hbcstore.hbcstore_api.cart.dto.CartItemRequest;
import com.hbcstore.hbcstore_api.cart.dto.CartResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cart")
public class CartController {
    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public CartResponse getMyCart(Authentication authentication) {
        return cartService.getMyCart(authentication.getName());
    }

    @PostMapping("/items")
    public CartResponse upsertItem(Authentication authentication, @Valid @RequestBody CartItemRequest request) {
        return cartService.upsertItem(authentication.getName(), request);
    }

    @DeleteMapping("/items/{productId}")
    public CartResponse removeItem(Authentication authentication, @PathVariable Long productId) {
        return cartService.removeItem(authentication.getName(), productId);
    }

    @DeleteMapping
    public CartResponse clear(Authentication authentication) {
        return cartService.clear(authentication.getName());
    }
}
