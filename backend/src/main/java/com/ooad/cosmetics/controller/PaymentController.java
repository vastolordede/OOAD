package com.ooad.cosmetics.controller;

import com.ooad.cosmetics.common.response.ApiResponse;
import com.ooad.cosmetics.dto.order.PaymentResponse;
import com.ooad.cosmetics.service.PaymentService;
import org.springframework.web.bind.annotation.*;

/** Thanh toán online mô phỏng: xem trạng thái, xác nhận thành công, báo thất bại. */
@RestController
@RequestMapping("/api/orders/{orderId}/payment")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping
    public ApiResponse<PaymentResponse> get(@PathVariable Long orderId) {
        return ApiResponse.success(paymentService.get(orderId));
    }

    @PostMapping("/confirm")
    public ApiResponse<PaymentResponse> confirm(@PathVariable Long orderId) {
        return ApiResponse.success("Payment confirmed", paymentService.confirm(orderId));
    }

    @PostMapping("/fail")
    public ApiResponse<PaymentResponse> fail(@PathVariable Long orderId) {
        return ApiResponse.success("Payment marked as failed", paymentService.fail(orderId));
    }
}
