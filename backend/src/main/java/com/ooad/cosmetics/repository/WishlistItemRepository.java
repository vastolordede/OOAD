package com.ooad.cosmetics.repository;

import com.ooad.cosmetics.entity.WishlistItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WishlistItemRepository extends JpaRepository<WishlistItem, Long> {

    Page<WishlistItem> findByWishlistId(Long wishlistId, Pageable pageable);

    Optional<WishlistItem> findByWishlistIdAndProductId(Long wishlistId, Long productId);

    boolean existsByWishlistIdAndProductId(Long wishlistId, Long productId);

    @Query("""
            SELECT i.product.id
            FROM WishlistItem i
            WHERE i.wishlist.id = :wishlistId
            ORDER BY i.id DESC
            """)
    List<Long> findProductIdsByWishlistId(@Param("wishlistId") Long wishlistId);
}
