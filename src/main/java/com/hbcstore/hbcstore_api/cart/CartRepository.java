package com.hbcstore.hbcstore_api.cart;

import com.hbcstore.hbcstore_api.user.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartRepository extends JpaRepository<Cart, Long> {
    Optional<Cart> findByUser(User user);
}
