package com.ooad.cosmetics.controller;

import com.ooad.cosmetics.common.response.ApiResponse;
import com.ooad.cosmetics.dto.dashboard.DashboardSummaryResponse;
import com.ooad.cosmetics.dto.dashboard.RevenueGrouping;
import com.ooad.cosmetics.dto.dashboard.RevenuePointResponse;
import com.ooad.cosmetics.dto.dashboard.TopProductResponse;
import com.ooad.cosmetics.service.DashboardService;
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
    public ApiResponse<DashboardSummaryResponse> summary(
            @RequestParam(defaultValue = "10") @Min(0) int lowStockThreshold
    ) {
        return ApiResponse.success(dashboardService.summary(lowStockThreshold));
    }

    @GetMapping("/revenue")
    public ApiResponse<List<RevenuePointResponse>> revenue(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "DAY") RevenueGrouping groupBy
    ) {
        return ApiResponse.success(dashboardService.revenue(from, to, groupBy));
    }

    @GetMapping("/top-products")
    public ApiResponse<List<TopProductResponse>> topProducts(
            @RequestParam(defaultValue = "5") @Min(1) @Max(50) int limit
    ) {
        return ApiResponse.success(dashboardService.topProducts(limit));
    }
}
