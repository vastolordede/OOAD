package com.ooad.cosmetics.controller;

import com.ooad.cosmetics.common.response.ApiResponse;
import com.ooad.cosmetics.common.response.PageResponse;
import com.ooad.cosmetics.dto.voucher.VoucherRequest;
import com.ooad.cosmetics.dto.voucher.VoucherResponse;
import com.ooad.cosmetics.entity.CatalogStatus;
import com.ooad.cosmetics.service.VoucherService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/vouchers")
@Validated
public class AdminVoucherController {

    private final VoucherService voucherService;

    public AdminVoucherController(VoucherService voucherService) {
        this.voucherService = voucherService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<VoucherResponse>> create(
            @Valid @RequestBody VoucherRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        "Voucher created",
                        voucherService.create(request)
                ));
    }

    @PutMapping("/{id}")
    public ApiResponse<VoucherResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody VoucherRequest request
    ) {
        return ApiResponse.success(
                "Voucher updated",
                voucherService.update(id, request)
        );
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<VoucherResponse> status(
            @PathVariable Long id,
            @RequestParam CatalogStatus status
    ) {
        return ApiResponse.success(
                "Voucher status updated",
                voucherService.setStatus(id, status)
        );
    }

    @GetMapping("/{id}")
    public ApiResponse<VoucherResponse> detail(@PathVariable Long id) {
        return ApiResponse.success(voucherService.detail(id));
    }

    @GetMapping
    public ApiResponse<PageResponse<VoucherResponse>> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) CatalogStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return ApiResponse.success(voucherService.list(q, status, page, size));
    }
}
