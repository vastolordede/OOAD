package com.ooad.cosmetics.service;

import com.ooad.cosmetics.common.exception.BadRequestException;
import com.ooad.cosmetics.common.exception.ResourceNotFoundException;
import com.ooad.cosmetics.common.response.PageResponse;
import com.ooad.cosmetics.dto.order.AdminOrderResponse;
import com.ooad.cosmetics.dto.order.OrderResponse;
import com.ooad.cosmetics.dto.order.OrderSummaryResponse;
import com.ooad.cosmetics.entity.Order;
import com.ooad.cosmetics.entity.OrderItem;
import com.ooad.cosmetics.entity.OrderStatus;
import com.ooad.cosmetics.entity.Payment;
import com.ooad.cosmetics.entity.PaymentMethod;
import com.ooad.cosmetics.entity.PaymentStatus;
import com.ooad.cosmetics.entity.User;
import com.ooad.cosmetics.repository.OrderItemRepository;
import com.ooad.cosmetics.repository.OrderRepository;
import com.ooad.cosmetics.repository.OrderSpecifications;
import com.ooad.cosmetics.repository.PaymentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class OrderService {

    /** Ngày lọc của Admin được hiểu theo giờ Việt Nam. */
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PaymentRepository paymentRepository;
    private final StockService stockService;
    private final VoucherService voucherService;
    private final CurrentUserService currentUserService;

    public OrderService(
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            PaymentRepository paymentRepository,
            StockService stockService,
            VoucherService voucherService,
            CurrentUserService currentUserService
    ) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.paymentRepository = paymentRepository;
        this.stockService = stockService;
        this.voucherService = voucherService;
        this.currentUserService = currentUserService;
    }

    // ------------------------------------------------------------------
    // Customer
    // ------------------------------------------------------------------

    /** O19: lịch sử đơn hàng của chính khách. */
    @Transactional(readOnly = true)
    public PageResponse<OrderSummaryResponse> listMine(OrderStatus status, int page, int size) {
        User user = currentUserService.requireCurrentUser();
        PageRequest pageable = PageRequest.of(page, size, newestFirst());

        Page<Order> orders = status == null
                ? orderRepository.findByUserId(user.getId(), pageable)
                : orderRepository.findByUserIdAndStatus(user.getId(), status, pageable);

        return new PageResponse<>(
                toSummaries(orders.getContent()),
                orders.getNumber(),
                orders.getSize(),
                orders.getTotalElements(),
                orders.getTotalPages(),
                orders.isFirst(),
                orders.isLast()
        );
    }

    /** O20: chi tiết một đơn của chính khách. */
    @Transactional(readOnly = true)
    public OrderResponse detailMine(Long id) {
        User user = currentUserService.requireCurrentUser();
        Order order = orderRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Order", id));
        return toResponse(order);
    }

    /** O21, O22, O23, O24: khách hủy đơn khi còn PENDING hoặc CONFIRMED. */
    @Transactional
    public OrderResponse cancelMine(Long id) {
        User user = currentUserService.requireCurrentUser();
        Order order = orderRepository.lockByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Order", id));

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new BadRequestException("Order is already cancelled");
        }
        if (!order.getStatus().canTransitionTo(OrderStatus.CANCELLED)) {
            throw new BadRequestException("Order can no longer be cancelled");
        }

        cancel(order);
        return toResponse(order);
    }

    // ------------------------------------------------------------------
    // Admin
    // ------------------------------------------------------------------

    /** O25: danh sách, tìm kiếm, lọc đơn hàng. */
    @Transactional(readOnly = true)
    public PageResponse<OrderSummaryResponse> adminList(
            OrderStatus status,
            String q,
            LocalDate from,
            LocalDate to,
            int page,
            int size
    ) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new BadRequestException("from must not be after to");
        }

        Instant fromInstant = from == null ? null : from.atStartOfDay(BUSINESS_ZONE).toInstant();
        Instant toExclusive = to == null ? null : to.plusDays(1).atStartOfDay(BUSINESS_ZONE).toInstant();

        PageRequest pageable = PageRequest.of(page, size, newestFirst());
        Page<Order> orders = orderRepository.findAll(
                OrderSpecifications.adminFilter(status, normalizeQuery(q), fromInstant, toExclusive),
                pageable
        );

        return new PageResponse<>(
                toSummaries(orders.getContent()),
                orders.getNumber(),
                orders.getSize(),
                orders.getTotalElements(),
                orders.getTotalPages(),
                orders.isFirst(),
                orders.isLast()
        );
    }

    @Transactional(readOnly = true)
    public AdminOrderResponse adminDetail(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", id));
        User customer = order.getUser();

        return new AdminOrderResponse(
                customer.getId(),
                customer.getEmail(),
                customer.getFullName(),
                toResponse(order)
        );
    }

    /**
     * O26 - O30 + O32: đổi trạng thái đơn theo đúng bảng chuyển trạng thái.
     * - Sang CANCELLED: hoàn tồn kho và lượt dùng voucher.
     * - Sang DELIVERED: ghi thời điểm giao; thanh toán COD tự chuyển sang PAID.
     */
    @Transactional
    public OrderResponse updateStatus(Long id, OrderStatus next) {
        Order order = orderRepository.lockById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", id));

        OrderStatus current = order.getStatus();
        if (!current.canTransitionTo(next)) {
            throw new BadRequestException(
                    "Cannot change order status from " + current + " to " + next
            );
        }

        if (next == OrderStatus.CANCELLED) {
            cancel(order);
        } else {
            order.setStatus(next);
            if (next == OrderStatus.DELIVERED) {
                markDelivered(order);
            }
        }

        return toResponse(order);
    }

    // ------------------------------------------------------------------

    /** O24: hoàn tồn kho, trả lượt dùng voucher, ghi nhận hủy. */
    private void cancel(Order order) {
        List<OrderItem> items = orderItemRepository.findByOrderIdOrderByIdAsc(order.getId());

        // Cùng thứ tự khóa dòng như lúc tạo đơn để tránh deadlock.
        items.stream()
                .sorted(Comparator.comparing(item -> item.getVariant().getId()))
                .forEach(item -> stockService.restore(item.getVariant().getId(), item.getQuantity()));

        voucherService.releaseUsage(order);

        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelledAt(Instant.now());

        paymentRepository.findByOrderId(order.getId()).ifPresent(payment -> {
            if (payment.getStatus() == PaymentStatus.PAID) {
                payment.setStatus(PaymentStatus.REFUNDED);
            }
        });
    }

    /** O32 */
    private void markDelivered(Order order) {
        Instant now = Instant.now();
        order.setDeliveredAt(now);

        paymentRepository.findByOrderId(order.getId()).ifPresent(payment -> {
            if (payment.getMethod() == PaymentMethod.COD
                    && payment.getStatus() == PaymentStatus.PENDING) {
                payment.setStatus(PaymentStatus.PAID);
                payment.setPaidAt(now);
            }
        });
    }

    private OrderResponse toResponse(Order order) {
        List<OrderItem> items = orderItemRepository.findByOrderIdOrderByIdAsc(order.getId());
        Payment payment = paymentRepository.findByOrderId(order.getId()).orElse(null);
        return OrderResponse.from(order, items, payment);
    }

    /** Gom dữ liệu của cả trang bằng 2 truy vấn thay vì 2 truy vấn cho mỗi đơn. */
    private List<OrderSummaryResponse> toSummaries(List<Order> orders) {
        if (orders.isEmpty()) {
            return List.of();
        }

        List<Long> ids = orders.stream().map(Order::getId).toList();

        Map<Long, List<OrderItem>> itemsByOrder = orderItemRepository
                .findByOrderIdInOrderByIdAsc(ids)
                .stream()
                .collect(Collectors.groupingBy(item -> item.getOrder().getId()));

        Map<Long, Payment> paymentByOrder = paymentRepository
                .findByOrderIdIn(ids)
                .stream()
                .collect(Collectors.toMap(
                        payment -> payment.getOrder().getId(),
                        Function.identity()
                ));

        return orders.stream().map(order -> {
            List<OrderItem> items = itemsByOrder.getOrDefault(order.getId(), List.of());
            Payment payment = paymentByOrder.get(order.getId());

            return new OrderSummaryResponse(
                    order.getId(),
                    order.getStatus(),
                    order.getReceiverName(),
                    order.getUser().getEmail(),
                    items.stream().mapToInt(OrderItem::getQuantity).sum(),
                    items.isEmpty() ? null : items.get(0).getProductName(),
                    order.getTotalAmount(),
                    payment == null ? null : payment.getMethod(),
                    payment == null ? null : payment.getStatus(),
                    order.getCreatedAt()
            );
        }).toList();
    }

    private Sort newestFirst() {
        return Sort.by(Sort.Direction.DESC, "createdAt")
                .and(Sort.by(Sort.Direction.DESC, "id"));
    }

    private String normalizeQuery(String q) {
        if (q == null) {
            return null;
        }
        String value = q.trim();
        return value.isEmpty() ? null : value;
    }
}
