package com.ooad.cosmetics.repository;

import com.ooad.cosmetics.entity.Review;
import com.ooad.cosmetics.entity.ReviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ReviewRepository
        extends JpaRepository<Review, Long>, JpaSpecificationExecutor<Review> {

    boolean existsByOrderItemId(Long orderItemId);

    Optional<Review> findByIdAndUserId(Long id, Long userId);

    Page<Review> findByUserId(Long userId, Pageable pageable);

    Page<Review> findByProductIdAndStatus(Long productId, ReviewStatus status, Pageable pageable);

    long countByProductIdAndStatus(Long productId, ReviewStatus status);

    @Query("""
            SELECT AVG(r.rating)
            FROM Review r
            WHERE r.product.id = :productId
              AND r.status = :status
            """)
    Double averageRating(
            @Param("productId") Long productId,
            @Param("status") ReviewStatus status
    );
}
