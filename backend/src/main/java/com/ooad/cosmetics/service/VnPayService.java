package com.ooad.cosmetics.service;

import com.ooad.cosmetics.common.exception.BadRequestException;
import com.ooad.cosmetics.common.exception.ResourceNotFoundException;
import com.ooad.cosmetics.config.VnPayProperties;
import com.ooad.cosmetics.entity.Order;
import com.ooad.cosmetics.entity.OrderStatus;
import com.ooad.cosmetics.entity.Payment;
import com.ooad.cosmetics.entity.PaymentMethod;
import com.ooad.cosmetics.entity.PaymentStatus;
import com.ooad.cosmetics.repository.OrderRepository;
import com.ooad.cosmetics.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Thanh toán VNPay sandbox (API 2.1.0).
 * - createPaymentUrl: tạo link thanh toán đã ký HMAC-SHA512.
 * - handleCallback: dùng chung cho IPN (VNPay gọi server) và Return (FE chuyển tiếp).
 *   Idempotent: gọi nhiều lần cho cùng giao dịch vẫn an toàn.
 */
@Service
public class VnPayService {

    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter VNP_DATE = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /** rspCode theo quy ước IPN của VNPay: 00 ok, 01 không thấy đơn, 02 đã xác nhận, 04 sai số tiền, 97 sai chữ ký. */
    public record Result(String rspCode, String message, boolean paid, Long orderId) {}

    private final VnPayProperties props;
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;

    public VnPayService(
            VnPayProperties props,
            PaymentRepository paymentRepository,
            OrderRepository orderRepository
    ) {
        this.props = props;
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
    }

    @Transactional
    public String createPaymentUrl(Long orderId, String clientIp) {
        if (props.getTmnCode().isBlank() || props.getHashSecret().isBlank()) {
            throw new IllegalStateException("VNPay is not configured (vnpay.tmn-code / vnpay.hash-secret)");
        }

        // Lock the order before any payment mutation: cancel, confirmation and callback
        // must never race on the same pending order.
        orderRepository.lockById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
        Order order = payment.getOrder();

        if (payment.getMethod() != PaymentMethod.ONLINE) {
            throw new BadRequestException("Only online payments can be paid with VNPay");
        }
        if (payment.getStatus() == PaymentStatus.PAID) {
            throw new BadRequestException("This order has already been paid");
        }
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BadRequestException("Only a pending order can be paid");
        }
        // A fresh click must not overwrite the only reference for an in-flight payment.
        // Otherwise a late successful callback for the first attempt would be lost.
        if (payment.getStatus() == PaymentStatus.PENDING
                && payment.getTransactionRef() != null
                && !payment.getTransactionRef().startsWith("MOCK-")) {
            Instant attemptAt = payment.getUpdatedAt();
            if (attemptAt == null || attemptAt.isAfter(
                    Instant.now().minus(Duration.ofMinutes(props.getExpireMinutes() + 5L)))) {
                throw new BadRequestException("A VNPay attempt is pending; check its result before retrying");
            }
        }

        // Mỗi lần thử thanh toán cần mã tham chiếu mới (VNPay không cho trùng vnp_TxnRef).
        String txnRef = orderId + "-" + UUID.randomUUID().toString().replace("-", "");
        payment.setTransactionRef(txnRef);
        paymentRepository.save(payment);

        ZonedDateTime now = ZonedDateTime.now(VN_ZONE);
        Map<String, String> params = new TreeMap<>();
        params.put("vnp_Version", "2.1.0");
        params.put("vnp_Command", "pay");
        params.put("vnp_TmnCode", props.getTmnCode());
        params.put("vnp_Amount", toVnpAmount(payment.getAmount()));
        params.put("vnp_CurrCode", "VND");
        params.put("vnp_TxnRef", txnRef);
        params.put("vnp_OrderInfo", "Thanh toan don hang " + orderId); // ASCII để chữ ký không lệch
        params.put("vnp_OrderType", "other");
        params.put("vnp_Locale", "vn");
        params.put("vnp_ReturnUrl", props.getReturnUrl());
        params.put("vnp_IpAddr", clientIp);
        params.put("vnp_CreateDate", now.format(VNP_DATE));
        params.put("vnp_ExpireDate", now.plusMinutes(props.getExpireMinutes()).format(VNP_DATE));

        String query = buildQuery(params);
        return props.getPayUrl() + "?" + query + "&vnp_SecureHash=" + hmacSha512(query);
    }

    @Transactional
    public Result handleCallback(Map<String, String> rawParams) {
        // Fail closed: an empty HMAC secret would make callbacks forgeable.
        if (props.getTmnCode().isBlank() || props.getHashSecret().isBlank()) {
            return new Result("97", "Payment provider is not configured", false, null);
        }
        String received = rawParams.get("vnp_SecureHash");
        if (received == null || !verifySignature(rawParams, received)
                || !props.getTmnCode().equals(rawParams.get("vnp_TmnCode"))
                || !"VND".equals(rawParams.get("vnp_CurrCode"))
                || !hasText(rawParams.get("vnp_ResponseCode"))
                || !hasText(rawParams.get("vnp_TransactionStatus"))) {
            return new Result("97", "Invalid payment callback", false, null);
        }

        String txnRef = rawParams.get("vnp_TxnRef");
        Long orderId = parseOrderId(txnRef);
        if (orderId == null) {
            return new Result("01", "Order not found", false, null);
        }

        // Lock in the same order as cancellation and admin status transitions.
        Order lockedOrder = orderRepository.lockById(orderId).orElse(null);
        if (lockedOrder == null) {
            return new Result("01", "Order not found", false, orderId);
        }

        Payment payment = paymentRepository.findByOrderId(orderId).orElse(null);
        if (payment == null || payment.getMethod() != PaymentMethod.ONLINE
                || !txnRef.equals(payment.getTransactionRef())) {
            return new Result("01", "Order not found", false, orderId);
        }

        if (!toVnpAmount(payment.getAmount()).equals(rawParams.get("vnp_Amount"))) {
            return new Result("04", "Invalid amount", false, orderId);
        }

        if (payment.getStatus() == PaymentStatus.PAID) {
            return new Result("02", "Order already confirmed", true, orderId);
        }
        if (lockedOrder.getStatus() != OrderStatus.PENDING) {
            return new Result("02", "Order is no longer pending", false, orderId);
        }

        boolean success = "00".equals(rawParams.get("vnp_ResponseCode"))
                && "00".equals(rawParams.get("vnp_TransactionStatus"));
        if (success) {
            payment.setStatus(PaymentStatus.PAID);
            payment.setPaidAt(Instant.now());
        } else {
            payment.setStatus(PaymentStatus.FAILED);
        }
        paymentRepository.save(payment);

        return new Result("00", success ? "Payment successful" : "Payment failed", success, orderId);
    }

    // ---- helpers -------------------------------------------------------------

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    boolean verifySignature(Map<String, String> rawParams, String received) {
        Map<String, String> sorted = new TreeMap<>();
        rawParams.forEach((k, v) -> {
            if (k.startsWith("vnp_") && !k.equals("vnp_SecureHash") && !k.equals("vnp_SecureHashType")
                    && v != null && !v.isEmpty()) {
                sorted.put(k, v);
            }
        });
        String expected = hmacSha512(buildQuery(sorted));
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                received.toLowerCase().getBytes(StandardCharsets.UTF_8));
    }

    private static String buildQuery(Map<String, String> sorted) {
        return sorted.entrySet().stream()
                .filter(e -> e.getValue() != null && !e.getValue().isEmpty())
                .map(e -> enc(e.getKey()) + "=" + enc(e.getValue()))
                .collect(Collectors.joining("&"));
    }

    private static String enc(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String hmacSha512(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            mac.init(new SecretKeySpec(props.getHashSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
            return HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Cannot sign VNPay request", e);
        }
    }

    /** VNPay yêu cầu số tiền nhân 100, không có phần thập phân. */
    private static String toVnpAmount(BigDecimal amount) {
        return amount.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).toPlainString();
    }

    private static Long parseOrderId(String txnRef) {
        if (txnRef == null) return null;
        int dash = txnRef.indexOf('-');
        try {
            return Long.parseLong(dash < 0 ? txnRef : txnRef.substring(0, dash));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
