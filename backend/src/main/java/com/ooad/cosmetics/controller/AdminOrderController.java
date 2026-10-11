package com.ooad.cosmetics.controller;

import com.ooad.cosmetics.common.response.ApiResponse;
import com.ooad.cosmetics.common.response.PageResponse;
import com.ooad.cosmetics.dto.order.AdminOrderResponse;
import com.ooad.cosmetics.dto.order.OrderResponse;
import com.ooad.cosmetics.dto.order.OrderSummaryResponse;
import com.ooad.cosmetics.dto.order.UpdateOrderStatusRequest;
import com.ooad.cosmetics.entity.OrderStatus;
import com.ooad.cosmetics.service.OrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/admin/orders")
@Validated
public class AdminOrderController {

    private final OrderService orderService;

    public AdminOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public ApiResponse<PageResponse<OrderSummaryResponse>> list(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) String q,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return ApiResponse.success(orderService.adminList(status, q, from, to, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<AdminOrderResponse> detail(@PathVariable Long id) {
        return ApiResponse.success(orderService.adminDetail(id));
    }

    @PutMapping("/{id}/status")
    public ApiResponse<OrderResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateOrderStatusRequest request
    ) {
        return ApiResponse.success(
                "Order status updated",
                orderService.updateStatus(id, request.status())
        );
    }
}
