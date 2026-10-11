package com.ooad.cosmetics.repository;

import com.ooad.cosmetics.entity.Order;
import com.ooad.cosmetics.entity.OrderStatus;
import com.ooad.cosmetics.entity.UserRole;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Các truy vấn thống kê cho Dashboard. Chỉ đọc, không đổi dữ liệu.
 * Dùng interface "Repository" thuần để không phải sửa repository của người khác.
 */
public interface DashboardRepository extends Repository<Order, Long> {

    /** DB01 */
    @Query("SELECT COUNT(u) FROM User u WHERE u.role = :role")
    long countUsersByRole(@Param("role") UserRole role);

    /** DB02 */
    @Query("SELECT COUNT(p) FROM Product p")
    long countProducts();

    /** DB03 */
    @Query("SELECT COUNT(o) FROM PurchaseOrder o")
    long countOrders();

    /** DB04: trả về null nếu chưa có đơn nào ở trạng thái đó. */
    @Query("SELECT SUM(o.totalAmount) FROM PurchaseOrder o WHERE o.status = :status")
    BigDecimal sumRevenueByStatus(@Param("status") OrderStatus status);

    /** DB05: mỗi dòng là [OrderStatus, Long]. */
    @Query("SELECT o.status, COUNT(o) FROM PurchaseOrder o GROUP BY o.status")
    List<Object[]> countOrdersGroupedByStatus();

    /** DB10: mỗi dòng là [Instant deliveredAt, BigDecimal totalAmount]. */
    @Query("""
            SELECT o.deliveredAt, o.totalAmount
            FROM PurchaseOrder o
            WHERE o.status = :status
              AND o.deliveredAt >= :from
              AND o.deliveredAt < :to
            """)
    List<Object[]> findRevenueRows(
            @Param("status") OrderStatus status,
            @Param("from") Instant from,
            @Param("to") Instant to
    );

    /**
     * DB11: mỗi dòng là [product_id, product_name, tổng số lượng, tổng tiền],
     * chỉ tính các đơn DELIVERED. Dùng SQL thuần vì phải nối bảng orders.
     */
    @Query(nativeQuery = true, value = """
            SELECT p.id, p.name, SUM(oi.quantity), SUM(oi.line_total)
            FROM order_items oi
            JOIN orders o ON o.id = oi.order_id
            JOIN product_variants v ON v.id = oi.variant_id
            JOIN products p ON p.id = v.product_id
            WHERE o.status = 'DELIVERED'
            GROUP BY p.id, p.name
            ORDER BY SUM(oi.quantity) DESC, p.id ASC
            LIMIT :limit
            """)
    List<Object[]> findTopSellingProducts(@Param("limit") int limit);
}
