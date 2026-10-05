package com.ooad.cosmetics.repository;

import com.ooad.cosmetics.entity.CatalogStatus;
import com.ooad.cosmetics.entity.ProductVariant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

    boolean existsBySkuIgnoreCase(String sku);

    boolean existsBySkuIgnoreCaseAndIdNot(String sku, Long id);

    List<ProductVariant> findByProductIdOrderByIdAsc(Long productId);

    List<ProductVariant> findByProductIdAndStatusOrderByIdAsc(
            Long productId,
            CatalogStatus status
    );

    Optional<ProductVariant> findByIdAndProductId(Long id, Long productId);

    Page<ProductVariant> findByStatusAndStockLessThanEqual(
            CatalogStatus status,
            int stock,
            Pageable pageable
    );

    @Query("""
            SELECT MIN(v.price)
            FROM ProductVariant v
            WHERE v.product.id = :productId
              AND v.status = com.ooad.cosmetics.entity.CatalogStatus.ACTIVE
            """)
    BigDecimal findMinActivePrice(@Param("productId") Long productId);
}
