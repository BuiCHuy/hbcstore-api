package com.hbcstore.hbcstore_api.cart;

import com.hbcstore.hbcstore_api.catalog.Product;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    Optional<CartItem> findByCartAndProduct(Cart cart, Product product);
    List<CartItem> findAllByCart(Cart cart);
    void deleteAllByCart(Cart cart);
}
