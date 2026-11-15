package com.ooad.cosmetics.controller;

import com.ooad.cosmetics.common.response.ApiResponse;
import com.ooad.cosmetics.dto.order.PaymentResponse;
import com.ooad.cosmetics.service.PaymentService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Trạng thái thanh toán; confirm/fail chỉ dành cho chế độ mô phỏng được bật rõ ràng. */
@RestController
@RequestMapping("/api/orders/{orderId}/payment")
public class PaymentController {

    private final PaymentService paymentService;
    private final boolean simulationEnabled;

    public PaymentController(
            PaymentService paymentService,
            @Value("${app.payment.simulation-enabled:false}") boolean simulationEnabled
    ) {
        this.paymentService = paymentService;
        this.simulationEnabled = simulationEnabled;
    }

    @GetMapping
    public ApiResponse<PaymentResponse> get(@PathVariable Long orderId) {
        return ApiResponse.success(paymentService.get(orderId));
    }

    @PostMapping("/confirm")
    public ResponseEntity<ApiResponse<PaymentResponse>> confirm(@PathVariable Long orderId) {
        if (!simulationEnabled) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.<PaymentResponse>failure("Payment simulation is disabled", null));
        }
        return ResponseEntity.ok(
                ApiResponse.success("Payment confirmed", paymentService.confirm(orderId))
        );
    }

    @PostMapping("/fail")
    public ResponseEntity<ApiResponse<PaymentResponse>> fail(@PathVariable Long orderId) {
        if (!simulationEnabled) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.<PaymentResponse>failure("Payment simulation is disabled", null));
        }
        return ResponseEntity.ok(
                ApiResponse.success("Payment marked as failed", paymentService.fail(orderId))
        );
    }
}
