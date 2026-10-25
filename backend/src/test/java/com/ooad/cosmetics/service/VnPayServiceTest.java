package com.ooad.cosmetics.service;

import com.ooad.cosmetics.config.VnPayProperties;
import com.ooad.cosmetics.entity.Order;
import com.ooad.cosmetics.entity.OrderStatus;
import com.ooad.cosmetics.entity.Payment;
import com.ooad.cosmetics.entity.PaymentMethod;
import com.ooad.cosmetics.entity.PaymentStatus;
import com.ooad.cosmetics.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class VnPayServiceTest {

    @Mock PaymentRepository paymentRepository;

    private VnPayService service;
    private Payment payment;

    @BeforeEach
    void setUp() {
        VnPayProperties props = new VnPayProperties();
        props.setTmnCode("TESTCODE");
        props.setHashSecret("TESTSECRET");
        service = new VnPayService(props, paymentRepository);

        Order order = new Order();
        ReflectionTestUtils.setField(order, "id", 7L);
        order.setStatus(OrderStatus.PENDING);

        payment = new Payment();
        payment.setOrder(order);
        payment.setMethod(PaymentMethod.ONLINE);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setAmount(new BigDecimal("530000.00"));

        lenient().when(paymentRepository.findByOrderId(7L)).thenReturn(Optional.of(payment));
        lenient().when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));
    }

    private Map<String, String> queryOf(String url) {
        Map<String, String> map = new LinkedHashMap<>();
        for (String pair : URI.create(url).getRawQuery().split("&")) {
            String[] kv = pair.split("=", 2);
            map.put(kv[0], URLDecoder.decode(kv[1], StandardCharsets.UTF_8));
        }
        return map;
    }

    @Test
    void createdUrlCarriesAmountTimesOneHundredAndAValidSignature() {
        Map<String, String> q = queryOf(service.createPaymentUrl(7L, "127.0.0.1"));

        assertThat(q.get("vnp_Amount")).isEqualTo("53000000");
        assertThat(q.get("vnp_TxnRef")).startsWith("7-");
        assertThat(service.verifySignature(q, q.get("vnp_SecureHash"))).isTrue();
    }

    @Test
    void successfulCallbackMarksThePaymentPaidAndIsIdempotent() {
        Map<String, String> q = queryOf(service.createPaymentUrl(7L, "127.0.0.1"));
        Map<String, String> callback = signedCallback(q, "00", "00");

        VnPayService.Result first = service.handleCallback(callback);
        VnPayService.Result second = service.handleCallback(callback);

        assertThat(first.rspCode()).isEqualTo("00");
        assertThat(first.paid()).isTrue();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(payment.getPaidAt()).isNotNull();
        assertThat(second.rspCode()).isEqualTo("02");
    }

    @Test
    void failedCallbackMarksThePaymentFailedSoItCanBeRetried() {
        Map<String, String> q = queryOf(service.createPaymentUrl(7L, "127.0.0.1"));

        VnPayService.Result r = service.handleCallback(signedCallback(q, "24", "02"));

        assertThat(r.paid()).isFalse();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
    }

    @Test
    void tamperedAmountIsRejectedBecauseTheSignatureNoLongerMatches() {
        Map<String, String> q = queryOf(service.createPaymentUrl(7L, "127.0.0.1"));
        Map<String, String> callback = signedCallback(q, "00", "00");
        callback.put("vnp_Amount", "100");

        assertThat(service.handleCallback(callback).rspCode()).isEqualTo("97");
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    void callbackWithoutSignatureIsRejected() {
        assertThat(service.handleCallback(Map.of("vnp_TxnRef", "7-1")).rspCode()).isEqualTo("97");
    }

    /** Giả lập VNPay: giữ params của link, thêm mã kết quả rồi ký lại bằng đúng secret. */
    private Map<String, String> signedCallback(Map<String, String> q, String responseCode, String txnStatus) {
        Map<String, String> cb = new LinkedHashMap<>(q);
        cb.remove("vnp_SecureHash");
        cb.put("vnp_ResponseCode", responseCode);
        cb.put("vnp_TransactionStatus", txnStatus);
        cb.put("vnp_SecureHash", sign(cb));
        return cb;
    }

    private String sign(Map<String, String> params) {
        // Dùng chính logic ký của service qua phản chiếu để test không lặp lại thuật toán.
        Map<String, String> sorted = new java.util.TreeMap<>(params);
        String query = (String) ReflectionTestUtils.invokeMethod(VnPayService.class, "buildQuery", sorted);
        return (String) ReflectionTestUtils.invokeMethod(service, "hmacSha512", query);
    }
}
