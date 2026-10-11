package com.ooad.cosmetics.repository;

import com.ooad.cosmetics.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByOrderIdOrderByIdAsc(Long orderId);

    List<OrderItem> findByOrderIdInOrderByIdAsc(Collection<Long> orderIds);

    /** Dòng hàng chỉ được tìm thấy nếu đơn hàng chứa nó thuộc về userId. */
    Optional<OrderItem> findByIdAndOrderUserId(Long id, Long userId);
}
