package com.ooad.cosmetics.repository;

import com.ooad.cosmetics.entity.CatalogStatus;
import com.ooad.cosmetics.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;

public interface ProductRepository extends JpaRepository<Product, Long> {

    @Query(value = """
            SELECT p.*
            FROM products p
            JOIN categories c ON c.id = p.category_id
            JOIN brands b ON b.id = p.brand_id
            LEFT JOIN product_variants v
                ON v.product_id = p.id
               AND v.status = 'ACTIVE'
            WHERE p.status = 'ACTIVE'
              AND c.status = 'ACTIVE'
              AND b.status = 'ACTIVE'
              AND (:q IS NULL OR :q = '' OR LOWER(p.name) LIKE LOWER(CONCAT('%', :q, '%')))
              AND (:categoryId IS NULL OR p.category_id = :categoryId)
              AND (:brandId IS NULL OR p.brand_id = :brandId)
              AND (
                    (:minPrice IS NULL AND :maxPrice IS NULL)
                    OR EXISTS (
                        SELECT 1
                        FROM product_variants vf
                        WHERE vf.product_id = p.id
                          AND vf.status = 'ACTIVE'
                          AND (:minPrice IS NULL OR vf.price >= :minPrice)
                          AND (:maxPrice IS NULL OR vf.price <= :maxPrice)
                    )
              )
            GROUP BY p.id
            ORDER BY
                CASE WHEN :sortMode = 'PRICE_ASC' THEN MIN(v.price) END ASC NULLS LAST,
                CASE WHEN :sortMode = 'PRICE_DESC' THEN MIN(v.price) END DESC NULLS LAST,
                CASE WHEN :sortMode = 'NEWEST' THEN p.created_at END DESC,
                p.id DESC
            """,
            countQuery = """
            SELECT COUNT(*)
            FROM products p
            JOIN categories c ON c.id = p.category_id
            JOIN brands b ON b.id = p.brand_id
            WHERE p.status = 'ACTIVE'
              AND c.status = 'ACTIVE'
              AND b.status = 'ACTIVE'
              AND (:q IS NULL OR :q = '' OR LOWER(p.name) LIKE LOWER(CONCAT('%', :q, '%')))
              AND (:categoryId IS NULL OR p.category_id = :categoryId)
              AND (:brandId IS NULL OR p.brand_id = :brandId)
              AND (
                    (:minPrice IS NULL AND :maxPrice IS NULL)
                    OR EXISTS (
                        SELECT 1
                        FROM product_variants vf
                        WHERE vf.product_id = p.id
                          AND vf.status = 'ACTIVE'
                          AND (:minPrice IS NULL OR vf.price >= :minPrice)
                          AND (:maxPrice IS NULL OR vf.price <= :maxPrice)
                    )
              )
            """,
            nativeQuery = true)
    Page<Product> searchPublic(
            @Param("q") String q,
            @Param("categoryId") Long categoryId,
            @Param("brandId") Long brandId,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("sortMode") String sortMode,
            Pageable pageable
    );

    @Query("""
            SELECT p
            FROM Product p
            WHERE (:q IS NULL OR :q = '' OR LOWER(p.name) LIKE LOWER(CONCAT('%', :q, '%')))
              AND (:status IS NULL OR p.status = :status)
            """)
    Page<Product> searchAdmin(
            @Param("q") String q,
            @Param("status") CatalogStatus status,
            Pageable pageable
    );
}
