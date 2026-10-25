package com.ooad.cosmetics.controller;

import com.ooad.cosmetics.common.response.ApiResponse;
import com.ooad.cosmetics.common.response.PageResponse;
import com.ooad.cosmetics.dto.order.AdminOrderResponse;
import com.ooad.cosmetics.dto.order.OrderResponse;
import com.ooad.cosmetics.dto.order.OrderSummaryResponse;
import com.ooad.cosmetics.dto.order.UpdateOrderStatusRequest;
import com.ooad.cosmetics.entity.OrderStatus;
import com.ooad.cosmetics.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Admin Orders", description = "Admin order search and lifecycle management")
public class AdminOrderController {

    private final OrderService orderService;

    public AdminOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    @Operation(summary = "List and filter orders")
    public ApiResponse<PageResponse<OrderSummaryResponse>> list(
            @Parameter(description = "Filter by order status")
            @RequestParam(required = false) OrderStatus status,
            @Parameter(description = "Search order ID, customer email, receiver name, or phone")
            @RequestParam(required = false) String keyword,
            @Parameter(description = "Include orders created on or after this date")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @Parameter(description = "Include orders created on this date")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return ApiResponse.success(orderService.adminList(
                status, keyword, fromDate, toDate, page, size
        ));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get order details for admin")
    public ApiResponse<AdminOrderResponse> detail(@PathVariable Long id) {
        return ApiResponse.success(orderService.adminDetail(id));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update an order status")
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
