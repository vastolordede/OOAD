package com.ooad.cosmetics.controller;

import com.ooad.cosmetics.common.response.ApiResponse;
import com.ooad.cosmetics.dto.dashboard.DashboardSummaryResponse;
import com.ooad.cosmetics.dto.dashboard.RevenueGrouping;
import com.ooad.cosmetics.dto.dashboard.RevenuePointResponse;
import com.ooad.cosmetics.dto.dashboard.TopProductResponse;
import com.ooad.cosmetics.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admin/dashboard")
@Validated
public class AdminDashboardController {

    private final DashboardService dashboardService;

    public AdminDashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/summary")
    @Operation(summary = "Get dashboard totals and low-stock variants")
    public ApiResponse<DashboardSummaryResponse> summary(
            @Parameter(description = "Include active variants with stock at or below this value")
            @RequestParam(defaultValue = "10") @Min(0) int lowStockThreshold
    ) {
        return ApiResponse.success(dashboardService.summary(lowStockThreshold));
    }

    @GetMapping("/revenue")
    @Operation(summary = "Get delivered-order revenue grouped by day or month")
    public ApiResponse<List<RevenuePointResponse>> revenue(
            @Parameter(description = "First date to include, ISO date (yyyy-MM-dd); defaults to 29 days before 'to'")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "Last date to include, ISO date (yyyy-MM-dd); defaults to today in Asia/Ho_Chi_Minh")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @Parameter(description = "Revenue bucket; DAY or MONTH")
            @RequestParam(defaultValue = "DAY") RevenueGrouping groupBy
    ) {
        return ApiResponse.success(dashboardService.revenue(from, to, groupBy));
    }

    @GetMapping("/top-products")
    @Operation(summary = "Get best-selling products")
    public ApiResponse<List<TopProductResponse>> topProducts(
            @Parameter(description = "Maximum number of products to return (1-50)")
            @RequestParam(defaultValue = "5") @Min(1) @Max(50) int limit
    ) {
        return ApiResponse.success(dashboardService.topProducts(limit));
    }
}
