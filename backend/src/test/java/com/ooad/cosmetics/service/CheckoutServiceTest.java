package com.ooad.cosmetics.service;

import com.ooad.cosmetics.common.exception.BadRequestException;
import com.ooad.cosmetics.common.exception.ResourceNotFoundException;
import com.ooad.cosmetics.dto.checkout.CheckoutPreviewRequest;
import com.ooad.cosmetics.dto.checkout.CheckoutPreviewResponse;
import com.ooad.cosmetics.dto.order.CreateOrderRequest;
import com.ooad.cosmetics.dto.order.OrderResponse;
import com.ooad.cosmetics.entity.Address;
import com.ooad.cosmetics.entity.Brand;
import com.ooad.cosmetics.entity.Cart;
import com.ooad.cosmetics.entity.CartItem;
import com.ooad.cosmetics.entity.CartStatus;
import com.ooad.cosmetics.entity.CatalogStatus;
import com.ooad.cosmetics.entity.Category;
import com.ooad.cosmetics.entity.DiscountType;
import com.ooad.cosmetics.entity.Order;
import com.ooad.cosmetics.entity.OrderStatus;
import com.ooad.cosmetics.entity.Payment;
import com.ooad.cosmetics.entity.PaymentMethod;
import com.ooad.cosmetics.entity.PaymentStatus;
import com.ooad.cosmetics.entity.Product;
import com.ooad.cosmetics.entity.ProductVariant;
import com.ooad.cosmetics.entity.User;
import com.ooad.cosmetics.entity.Voucher;
import com.ooad.cosmetics.repository.AddressRepository;
import com.ooad.cosmetics.repository.CartItemRepository;
import com.ooad.cosmetics.repository.CartRepository;
import com.ooad.cosmetics.repository.OrderItemRepository;
import com.ooad.cosmetics.repository.OrderRepository;
import com.ooad.cosmetics.repository.PaymentRepository;
import com.ooad.cosmetics.repository.ProductImageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CheckoutServiceTest {

    private CartRepository cartRepository;
    private CartItemRepository cartItemRepository;
    private AddressRepository addressRepository;
    private VoucherService voucherService;
    private OrderRepository orderRepository;
    private OrderItemRepository orderItemRepository;
    private PaymentRepository paymentRepository;
    private StockService stockService;
    private CheckoutService service;

    private User user;
    private Cart cart;

    @BeforeEach
    void setUp() {
        cartRepository = mock(CartRepository.class);
        cartItemRepository = mock(CartItemRepository.class);
        addressRepository = mock(AddressRepository.class);
        ProductImageRepository imageRepository = mock(ProductImageRepository.class);
        voucherService = mock(VoucherService.class);
        orderRepository = mock(OrderRepository.class);
        orderItemRepository = mock(OrderItemRepository.class);
        paymentRepository = mock(PaymentRepository.class);
        stockService = mock(StockService.class);
        CurrentUserService currentUserService = mock(CurrentUserService.class);

        service = new CheckoutService(
                cartRepository,
                cartItemRepository,
                addressRepository,
                imageRepository,
                voucherService,
                orderRepository,
                orderItemRepository,
                paymentRepository,
                stockService,
                currentUserService
        );

        user = new User("customer@example.com", "hash", "Customer", null);
        ReflectionTestUtils.setField(user, "id", 1L);

        cart = new Cart();
        ReflectionTestUtils.setField(cart, "id", 100L);
        cart.setUser(user);

        Address address = new Address();
        ReflectionTestUtils.setField(address, "id", 5L);
        address.setUser(user);
        address.setReceiverName("Nguyen Van A");
        address.setPhone("0900000000");
        address.setAddressLine("1 Le Loi");
        address.setCity("Ho Chi Minh");

        when(currentUserService.requireCurrentUser()).thenReturn(user);
        when(addressRepository.findByIdAndUserId(5L, 1L)).thenReturn(Optional.of(address));
        when(cartRepository.findByUserIdAndStatus(1L, CartStatus.ACTIVE))
                .thenReturn(Optional.of(cart));
        when(cartRepository.lockByUserIdAndStatus(1L, CartStatus.ACTIVE))
                .thenReturn(Optional.of(cart));

        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(orderItemRepository.saveAll(anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    // ---------------- preview ----------------

    @Test
    void previewComputesSubtotalShippingAndTotalOnTheBackend() {
        stubCart(item(variant(10L, "100000.00", 5, CatalogStatus.ACTIVE), 1));

        CheckoutPreviewResponse response = service.preview(new CheckoutPreviewRequest(5L, null));

        assertThat(response.items()).hasSize(1);
        assertThat(response.subtotal()).isEqualByComparingTo(new BigDecimal("100000"));
        assertThat(response.shippingFee()).isEqualByComparingTo(new BigDecimal("30000"));
        assertThat(response.discountAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.totalAmount()).isEqualByComparingTo(new BigDecimal("130000"));
        assertThat(response.shippingAddress().receiverName()).isEqualTo("Nguyen Van A");
        assertThat(response.voucherCode()).isNull();
    }

    // ---------------- createOrder: happy paths ----------------

    @Test
    void createOrderDeductsStockSavesOrderAndClearsCart() {
        CartItem cartItem = item(variant(10L, "100000.00", 5, CatalogStatus.ACTIVE), 2);
        List<CartItem> cartItems = stubCart(cartItem);

        OrderResponse response = service.createOrder(
                new CreateOrderRequest(5L, null, PaymentMethod.COD, "  giao gio hanh chinh  ")
        );

        verify(stockService).deduct(10L, 2);
        verify(cartItemRepository).deleteAll(cartItems);

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());
        Order saved = orderCaptor.getValue();
        assertThat(saved.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(saved.getReceiverName()).isEqualTo("Nguyen Van A");
        assertThat(saved.getShippingCity()).isEqualTo("Ho Chi Minh");
        assertThat(saved.getNote()).isEqualTo("giao gio hanh chinh");

        assertThat(response.subtotal()).isEqualByComparingTo(new BigDecimal("200000"));
        assertThat(response.shippingFee()).isEqualByComparingTo(new BigDecimal("30000"));
        assertThat(response.totalAmount()).isEqualByComparingTo(new BigDecimal("230000"));
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).productName()).isEqualTo("Product 10");
        assertThat(response.items().get(0).unitPrice()).isEqualByComparingTo(new BigDecimal("100000"));
        assertThat(response.payment().method()).isEqualTo(PaymentMethod.COD);
        assertThat(response.payment().status()).isEqualTo(PaymentStatus.PENDING);
        assertThat(response.payment().amount()).isEqualByComparingTo(new BigDecimal("230000"));

        verify(voucherService, never()).recordUsage(any(), any(), any(), any());
    }

    @Test
    void createOrderIsFreeShippingAtThreshold() {
        stubCart(item(variant(10L, "100000.00", 10, CatalogStatus.ACTIVE), 5));

        OrderResponse response = service.createOrder(
                new CreateOrderRequest(5L, null, PaymentMethod.COD, null)
        );

        assertThat(response.shippingFee()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.totalAmount()).isEqualByComparingTo(new BigDecimal("500000"));
    }

    @Test
    void createOrderAppliesVoucherAndRecordsUsage() {
        stubCart(item(variant(10L, "100000.00", 5, CatalogStatus.ACTIVE), 2));

        Voucher voucher = new Voucher();
        voucher.setCode("SALE20");
        voucher.setDiscountType(DiscountType.FIXED);
        voucher.setDiscountValue(new BigDecimal("20000.00"));
        when(voucherService.validateForCheckout(eq("SALE20"), eq(1L), any(BigDecimal.class)))
                .thenReturn(voucher);

        OrderResponse response = service.createOrder(
                new CreateOrderRequest(5L, "SALE20", PaymentMethod.COD, null)
        );

        assertThat(response.discountAmount()).isEqualByComparingTo(new BigDecimal("20000"));
        assertThat(response.totalAmount()).isEqualByComparingTo(new BigDecimal("210000"));
        assertThat(response.voucherCode()).isEqualTo("SALE20");
        verify(voucherService).recordUsage(
                eq(voucher), eq(user), any(Order.class), any(BigDecimal.class)
        );
    }

    // ---------------- createOrder: rejections ----------------

    @Test
    void createOrderRejectsEmptyCart() {
        stubCart();

        assertThatThrownBy(() -> service.createOrder(
                new CreateOrderRequest(5L, null, PaymentMethod.COD, null)))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Cart is empty");

        verifyNoInteractions(stockService);
        verify(orderRepository, never()).save(any());
    }

    @Test
    void createOrderRejectsUnavailableProduct() {
        stubCart(item(variant(10L, "100000.00", 5, CatalogStatus.INACTIVE), 1));

        assertThatThrownBy(() -> service.createOrder(
                new CreateOrderRequest(5L, null, PaymentMethod.COD, null)))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Product 'Product 10' is no longer available");

        verifyNoInteractions(stockService);
    }

    @Test
    void createOrderRejectsInsufficientStock() {
        stubCart(item(variant(10L, "100000.00", 1, CatalogStatus.ACTIVE), 2));

        assertThatThrownBy(() -> service.createOrder(
                new CreateOrderRequest(5L, null, PaymentMethod.COD, null)))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Not enough stock for 'Product 10' (available: 1)");

        verifyNoInteractions(stockService);
    }

    @Test
    void createOrderRejectsAddressOfAnotherCustomer() {
        stubCart(item(variant(10L, "100000.00", 5, CatalogStatus.ACTIVE), 1));
        when(addressRepository.findByIdAndUserId(99L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createOrder(
                new CreateOrderRequest(99L, null, PaymentMethod.COD, null)))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(stockService);
    }

    @Test
    void createOrderRejectsOnlinePaymentForNow() {
        assertThatThrownBy(() -> service.createOrder(
                new CreateOrderRequest(5L, null, PaymentMethod.ONLINE, null)))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Only COD payment is supported at the moment");

        verifyNoInteractions(stockService);
    }

    @Test
    void createOrderStopsBeforeSavingAnythingWhenStockDeductionFails() {
        stubCart(item(variant(10L, "100000.00", 5, CatalogStatus.ACTIVE), 2));
        doThrow(new BadRequestException("Insufficient stock for variant 10"))
                .when(stockService).deduct(10L, 2);

        assertThatThrownBy(() -> service.createOrder(
                new CreateOrderRequest(5L, null, PaymentMethod.COD, null)))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Insufficient stock for variant 10");

        verify(orderRepository, never()).save(any());
        verify(paymentRepository, never()).save(any());
        verify(cartItemRepository, never()).deleteAll(anyList());
    }

    @Test
    void createOrderPropagatesVoucherFailureSoTheTransactionCanRollBack() {
        stubCart(item(variant(10L, "100000.00", 5, CatalogStatus.ACTIVE), 2));

        Voucher voucher = new Voucher();
        voucher.setCode("SALE20");
        voucher.setDiscountType(DiscountType.FIXED);
        voucher.setDiscountValue(new BigDecimal("20000.00"));
        when(voucherService.validateForCheckout(eq("SALE20"), eq(1L), any(BigDecimal.class)))
                .thenReturn(voucher);
        doThrow(new BadRequestException("Voucher usage limit reached"))
                .when(voucherService)
                .recordUsage(any(), any(), any(), any());

        assertThatThrownBy(() -> service.createOrder(
                new CreateOrderRequest(5L, "SALE20", PaymentMethod.COD, null)))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Voucher usage limit reached");

        // Giỏ hàng chỉ được dọn ở bước cuối cùng, sau khi voucher đã ghi nhận thành công.
        verify(cartItemRepository, never()).deleteAll(anyList());
    }

    // ---------------- helpers ----------------

    private List<CartItem> stubCart(CartItem... items) {
        List<CartItem> list = List.of(items);
        when(cartItemRepository.findByCartIdOrderByIdAsc(100L)).thenReturn(list);
        return list;
    }

    private CartItem item(ProductVariant variant, int quantity) {
        CartItem item = new CartItem();
        item.setCart(cart);
        item.setVariant(variant);
        item.setQuantity(quantity);
        return item;
    }

    private ProductVariant variant(Long id, String price, int stock, CatalogStatus status) {
        Category category = new Category();
        category.setName("Skincare");
        Brand brand = new Brand();
        brand.setName("Brand");

        Product product = new Product();
        product.setName("Product " + id);
        product.setCategory(category);
        product.setBrand(brand);

        ProductVariant variant = new ProductVariant();
        ReflectionTestUtils.setField(variant, "id", id);
        variant.setProduct(product);
        variant.setSku("SKU-" + id);
        variant.setPrice(new BigDecimal(price));
        variant.setStock(stock);
        variant.setStatus(status);
        return variant;
    }
}
