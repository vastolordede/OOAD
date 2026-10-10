package com.ooad.cosmetics.repository;

import com.ooad.cosmetics.entity.Cart;
import com.ooad.cosmetics.entity.CartStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {

    Optional<Cart> findByUserIdAndStatus(Long userId, CartStatus status);
}
