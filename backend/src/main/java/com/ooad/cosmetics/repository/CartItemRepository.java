package com.ooad.cosmetics.repository;

import com.ooad.cosmetics.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    List<CartItem> findByCartIdOrderByIdAsc(Long cartId);

    Optional<CartItem> findByCartIdAndVariantId(Long cartId, Long variantId);

    Optional<CartItem> findByIdAndCartId(Long id, Long cartId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM CartItem i WHERE i.cart.id = :cartId")
    int deleteAllByCartId(@Param("cartId") Long cartId);
}
