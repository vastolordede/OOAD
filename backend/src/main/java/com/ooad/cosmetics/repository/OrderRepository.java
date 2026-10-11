package com.ooad.cosmetics.repository;

import com.ooad.cosmetics.entity.Order;
import com.ooad.cosmetics.entity.OrderStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface OrderRepository
        extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {

    Optional<Order> findByIdAndUserId(Long id, Long userId);

    Page<Order> findByUserId(Long userId, Pageable pageable);

    Page<Order> findByUserIdAndStatus(Long userId, OrderStatus status, Pageable pageable);

    /** Khóa dòng đơn hàng khi đổi trạng thái / hủy, tránh khách và admin thao tác cùng lúc. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM PurchaseOrder o WHERE o.id = :id")
    Optional<Order> lockById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM PurchaseOrder o WHERE o.id = :id AND o.user.id = :userId")
    Optional<Order> lockByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);
}
