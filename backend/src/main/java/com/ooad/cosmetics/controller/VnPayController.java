package com.ooad.cosmetics.controller;

import com.ooad.cosmetics.common.response.ApiResponse;
import com.ooad.cosmetics.config.VnPayProperties;
import com.ooad.cosmetics.service.PaymentService;
import com.ooad.cosmetics.service.VnPayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@Tag(name = "VNPay")
public class VnPayController {

    public record VnPayUrlResponse(String paymentUrl) {}
    public record PaymentModesResponse(boolean vnpayAvailable, boolean simulationEnabled) {}

    private final VnPayService vnPayService;
    private final PaymentService paymentService;
    private final VnPayProperties vnpayProperties;
    private final boolean simulationEnabled;

    public VnPayController(
            VnPayService vnPayService,
            PaymentService paymentService,
            VnPayProperties vnpayProperties,
            @Value("${app.payment.simulation-enabled:false}") boolean simulationEnabled
    ) {
        this.vnPayService = vnPayService;
        this.paymentService = paymentService;
        this.vnpayProperties = vnpayProperties;
        this.simulationEnabled = simulationEnabled;
    }

    /** Public capability discovery; never exposes VNPay credentials. */
    @GetMapping("/api/public/payment/modes")
    public ApiResponse<PaymentModesResponse> paymentModes() {
        return ApiResponse.success(new PaymentModesResponse(
                !vnpayProperties.getTmnCode().isBlank() && !vnpayProperties.getHashSecret().isBlank(),
                simulationEnabled
        ));
    }

    /** Customer đã đăng nhập: lấy link thanh toán VNPay cho đơn của mình. */
    @PostMapping("/api/orders/{orderId}/payment/vnpay")
    @Operation(summary = "Create a VNPay payment URL for my online order")
    public ApiResponse<VnPayUrlResponse> createPaymentUrl(@PathVariable Long orderId, HttpServletRequest request) {
        paymentService.get(orderId);

        String url = vnPayService.createPaymentUrl(orderId, clientIp(request));
        return ApiResponse.success(new VnPayUrlResponse(url));
    }

    /** VNPay server gọi (IPN). Public: bảo vệ bằng chữ ký. Phải trả đúng định dạng RspCode/Message. */
    @GetMapping("/api/public/payment/vnpay/ipn")
    @Operation(summary = "VNPay IPN callback (called by VNPay server)")
    public Map<String, String> ipn(HttpServletRequest request) {
        VnPayService.Result r = vnPayService.handleCallback(flatten(request));
        Map<String, String> body = new LinkedHashMap<>();
        body.put("RspCode", r.rspCode());
        body.put("Message", r.message());
        return body;
    }

    /** FE trang /payment/vnpay-return chuyển nguyên query string sang đây để xác minh và cập nhật. */
    @GetMapping("/api/public/payment/vnpay/return")
    @Operation(summary = "Verify the VNPay return parameters forwarded by the frontend")
    public ApiResponse<Map<String, Object>> verifyReturn(HttpServletRequest request) {
        VnPayService.Result r = vnPayService.handleCallback(flatten(request));
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("paid", r.paid());
        data.put("code", r.rspCode());
        data.put("message", r.message());
        data.put("orderId", r.orderId());
        return ApiResponse.success(data);
    }

    private static Map<String, String> flatten(HttpServletRequest request) {
        Map<String, String> params = new LinkedHashMap<>();
        request.getParameterMap().forEach((k, v) -> {
            if (v.length > 0) params.put(k, v[0]);
        });
        return params;
    }

    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        String ip = (forwarded != null && !forwarded.isBlank())
                ? forwarded.split(",")[0].trim()
                : request.getRemoteAddr();
        return ip.contains(":") ? "127.0.0.1" : ip; // IPv6 loopback -> IPv4
    }
}
