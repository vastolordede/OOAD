package com.ooad.cosmetics.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ooad.cosmetics.dto.address.AddressRequest;
import com.ooad.cosmetics.dto.auth.LoginRequest;
import com.ooad.cosmetics.dto.auth.RegisterRequest;
import com.ooad.cosmetics.dto.cart.AddCartItemRequest;
import com.ooad.cosmetics.dto.checkout.CheckoutPreviewRequest;
import com.ooad.cosmetics.dto.order.CreateOrderRequest;
import com.ooad.cosmetics.dto.order.UpdateOrderStatusRequest;
import com.ooad.cosmetics.dto.review.CreateReviewRequest;
import com.ooad.cosmetics.dto.voucher.VoucherRequest;
import com.ooad.cosmetics.entity.DiscountType;
import com.ooad.cosmetics.entity.OrderStatus;
import com.ooad.cosmetics.entity.PaymentMethod;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * T11 / T16: luồng demo đầy đủ qua HTTP thật (bảo mật JWT, kiểm tra dữ liệu vào, JSON, database).
 * Dữ liệu catalog được tạo thẳng bằng SQL để bài test này không phụ thuộc vào phần của người khác.
 */
@Tag("integration")
@AutoConfigureMockMvc
class DemoFlowApiIntegrationTest extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // ------------------------------------------------------------------
    // Bảo mật và kiểm tra dữ liệu vào
    // ------------------------------------------------------------------

    @Test
    void protectedEndpointsRejectAnonymousUsersAndCustomersCannotReachAdminApis() throws Exception {
        call(get("/api/cart"), null, null, 401);
        call(post("/api/orders"), null, null, 401);
        call(get("/api/cart"), "this.is.not-a-valid-token", null, 401);

        String customerToken = register("shopper@example.com");
        call(get("/api/cart"), customerToken, null, 200);

        call(get("/api/admin/dashboard/summary"), customerToken, null, 403);
        call(get("/api/admin/orders"), customerToken, null, 403);
        call(get("/api/admin/vouchers"), customerToken, null, 403);
        call(get("/api/admin/reviews"), customerToken, null, 403);
    }

    @Test
    void invalidRequestsAreRejectedWithClearStatusCodes() throws Exception {
        long variantId = createVariant("100000.00", 5);
        String token = register("shopper@example.com");

        JsonNode invalid = call(post("/api/cart/items"), token, new AddCartItemRequest(variantId, 0), 400);
        assertThat(invalid.path("message").asText()).isEqualTo("Validation failed");

        call(post("/api/cart/items"), token, new AddCartItemRequest(999999L, 1), 404);
        call(post("/api/cart/items"), token, new AddCartItemRequest(variantId, 6), 400);
        call(get("/api/orders/{id}", 999999L), token, null, 404);
        call(post("/api/reviews"), token, new CreateReviewRequest(999999L, 5, null), 404);
    }

    @Test
    void publicCatalogCanBeBrowsedWithoutLoggingIn() throws Exception {
        long variantId = createVariant("150000.00", 5);

        JsonNode list = call(get("/api/public/products"), null, null, 200);
        assertThat(list.path("data").path("content").size()).isEqualTo(1);

        JsonNode detail = call(get("/api/public/products/{id}", productIdOf(variantId)), null, null, 200);
        assertThat(detail.path("data").path("variants").size()).isEqualTo(1);
    }

    // ------------------------------------------------------------------
    // Luồng chính: đăng ký -> giỏ -> đặt đơn -> giao -> review -> dashboard
    // ------------------------------------------------------------------

    @Test
    void customerShopsFromRegistrationToReviewAndAdminFulfilsTheOrder() throws Exception {
        long variantId = createVariant("200000.00", 10);
        long productId = productIdOf(variantId);
        createAdmin("admin@example.com");

        // 1. Đăng ký, đăng nhập
        String customerToken = register("shopper@example.com");
        String adminToken = login("admin@example.com");

        // 2. Địa chỉ giao hàng
        JsonNode address = call(post("/api/users/me/addresses"), customerToken,
                new AddressRequest("Nguyen Van A", "0900000000", "1 Le Loi", null, null, "Ho Chi Minh", true),
                201);
        long addressId = address.path("data").path("id").asLong();

        // 3. Giỏ hàng: thêm hợp lệ, thêm vượt tồn kho bị từ chối
        JsonNode cart = call(post("/api/cart/items"), customerToken, new AddCartItemRequest(variantId, 2), 201);
        assertThat(cart.path("data").path("subtotal").decimalValue()).isEqualByComparingTo("400000");
        call(post("/api/cart/items"), customerToken, new AddCartItemRequest(variantId, 100), 400);

        // 4. Xem trước đơn: 400.000 + 30.000 phí ship
        JsonNode preview = call(post("/api/checkout/preview"), customerToken,
                new CheckoutPreviewRequest(addressId, null), 200);
        assertThat(preview.path("data").path("shippingFee").decimalValue()).isEqualByComparingTo("30000");
        assertThat(preview.path("data").path("totalAmount").decimalValue()).isEqualByComparingTo("430000");

        // 5. Đặt đơn
        JsonNode created = call(post("/api/orders"), customerToken,
                new CreateOrderRequest(addressId, null, PaymentMethod.COD, "Giao buoi sang"), 201);
        long orderId = created.path("data").path("id").asLong();
        long orderItemId = created.path("data").path("items").get(0).path("id").asLong();
        assertThat(created.path("data").path("status").asText()).isEqualTo("PENDING");
        assertThat(stockOf(variantId)).isEqualTo(8);

        // 6. Giỏ đã trống, lịch sử có đơn
        JsonNode emptyCart = call(get("/api/cart"), customerToken, null, 200);
        assertThat(emptyCart.path("data").path("items").size()).isZero();
        JsonNode history = call(get("/api/orders"), customerToken, null, 200);
        assertThat(history.path("data").path("totalElements").asLong()).isEqualTo(1);
        call(get("/api/orders/{id}", orderId), customerToken, null, 200);

        // 7. Admin đưa đơn qua từng bước; nhảy bước bị từ chối
        call(put("/api/admin/orders/{id}/status", orderId), adminToken,
                new UpdateOrderStatusRequest(OrderStatus.DELIVERED), 400);
        for (OrderStatus next : List.of(OrderStatus.CONFIRMED, OrderStatus.SHIPPING, OrderStatus.DELIVERED)) {
            JsonNode updated = call(put("/api/admin/orders/{id}/status", orderId), adminToken,
                    new UpdateOrderStatusRequest(next), 200);
            assertThat(updated.path("data").path("status").asText()).isEqualTo(next.name());
        }

        // COD được ghi nhận đã thanh toán khi giao thành công
        JsonNode delivered = call(get("/api/orders/{id}", orderId), customerToken, null, 200);
        assertThat(delivered.path("data").path("payment").path("status").asText()).isEqualTo("PAID");

        // 8. Đã giao thì khách không hủy được
        call(put("/api/orders/{id}/cancel", orderId), customerToken, null, 400);

        // 9. Review: một lần duy nhất, hiển thị công khai kèm điểm trung bình
        call(post("/api/reviews"), customerToken, new CreateReviewRequest(orderItemId, 5, "Rat hai long"), 201);
        call(post("/api/reviews"), customerToken, new CreateReviewRequest(orderItemId, 4, null), 400);
        JsonNode reviews = call(get("/api/public/products/{id}/reviews", productId), null, null, 200);
        assertThat(reviews.path("data").path("reviewCount").asLong()).isEqualTo(1);
        assertThat(reviews.path("data").path("averageRating").asDouble()).isEqualTo(5.0);

        // 10. Dashboard của Admin
        JsonNode summary = call(get("/api/admin/dashboard/summary"), adminToken, null, 200);
        assertThat(summary.path("data").path("totalCustomers").asLong()).isEqualTo(1);
        assertThat(summary.path("data").path("totalRevenue").decimalValue()).isEqualByComparingTo("430000");
        assertThat(summary.path("data").path("ordersByStatus").path("DELIVERED").asLong()).isEqualTo(1);

        JsonNode top = call(get("/api/admin/dashboard/top-products"), adminToken, null, 200);
        assertThat(top.path("data").get(0).path("quantitySold").asLong()).isEqualTo(2);

        JsonNode adminOrders = call(get("/api/admin/orders?status=DELIVERED"), adminToken, null, 200);
        assertThat(adminOrders.path("data").path("totalElements").asLong()).isEqualTo(1);
    }

    @Test
    void adminCreatesAVoucherThroughTheApiAndACustomerUsesItOnce() throws Exception {
        long variantId = createVariant("200000.00", 10);
        createAdmin("admin@example.com");
        String adminToken = login("admin@example.com");
        String customerToken = register("shopper@example.com");

        VoucherRequest voucher = new VoucherRequest(
                "sale20", "Giam 20k", DiscountType.FIXED, new BigDecimal("20000"),
                new BigDecimal("100000"), null, 5,
                Instant.now().minusSeconds(3600), Instant.now().plusSeconds(86400));

        JsonNode created = call(post("/api/admin/vouchers"), adminToken, voucher, 201);
        long voucherId = created.path("data").path("id").asLong();
        assertThat(created.path("data").path("code").asText()).isEqualTo("SALE20");
        call(post("/api/admin/vouchers"), adminToken, voucher, 400);          // mã trùng
        call(post("/api/admin/vouchers"), customerToken, voucher, 403);       // khách không có quyền

        JsonNode address = call(post("/api/users/me/addresses"), customerToken,
                new AddressRequest("Nguyen Van A", "0900000000", "1 Le Loi", null, null, "Ho Chi Minh", true), 201);
        long addressId = address.path("data").path("id").asLong();

        call(post("/api/cart/items"), customerToken, new AddCartItemRequest(variantId, 1), 201);
        JsonNode preview = call(post("/api/checkout/preview"), customerToken,
                new CheckoutPreviewRequest(addressId, "SALE20"), 200);
        assertThat(preview.path("data").path("discountAmount").decimalValue()).isEqualByComparingTo("20000");
        assertThat(preview.path("data").path("totalAmount").decimalValue()).isEqualByComparingTo("210000");

        call(post("/api/orders"), customerToken,
                new CreateOrderRequest(addressId, "SALE20", PaymentMethod.COD, null), 201);

        // Dùng lại bị từ chối; Admin tắt voucher thì không áp dụng được nữa.
        call(post("/api/cart/items"), customerToken, new AddCartItemRequest(variantId, 1), 201);
        JsonNode reuse = call(post("/api/checkout/preview"), customerToken,
                new CheckoutPreviewRequest(addressId, "SALE20"), 400);
        assertThat(reuse.path("message").asText()).isEqualTo("You have already used this voucher");

        JsonNode disabled = call(patch("/api/admin/vouchers/{id}/status", voucherId)
                .param("status", "INACTIVE"), adminToken, null, 200);
        assertThat(disabled.path("data").path("status").asText()).isEqualTo("INACTIVE");
        assertThat(disabled.path("data").path("usedCount").asInt()).isEqualTo(1);
    }

    // ------------------------------------------------------------------

    private String register(String email) throws Exception {
        JsonNode response = call(post("/api/auth/register"), null,
                new RegisterRequest(email, PASSWORD, "Shopper One", null), 201);
        return response.path("data").path("accessToken").asText();
    }

    private String login(String email) throws Exception {
        JsonNode response = call(post("/api/auth/login"), null, new LoginRequest(email, PASSWORD), 200);
        return response.path("data").path("accessToken").asText();
    }

    private JsonNode call(
            MockHttpServletRequestBuilder builder,
            String token,
            Object body,
            int expectedStatus
    ) throws Exception {
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        if (body != null) {
            builder.contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(body));
        }

        MvcResult result = mockMvc.perform(builder)
                .andExpect(status().is(expectedStatus))
                .andReturn();

        String content = result.getResponse().getContentAsString();
        return content.isBlank() ? objectMapper.createObjectNode() : objectMapper.readTree(content);
    }
}
