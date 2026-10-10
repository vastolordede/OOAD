package com.ooad.cosmetics.service;

import com.ooad.cosmetics.common.exception.BadRequestException;
import com.ooad.cosmetics.common.exception.ResourceNotFoundException;
import com.ooad.cosmetics.dto.address.AddressResponse;
import com.ooad.cosmetics.dto.checkout.CheckoutItemResponse;
import com.ooad.cosmetics.dto.checkout.CheckoutPreviewRequest;
import com.ooad.cosmetics.dto.checkout.CheckoutPreviewResponse;
import com.ooad.cosmetics.dto.order.CreateOrderRequest;
import com.ooad.cosmetics.dto.order.OrderResponse;
import com.ooad.cosmetics.entity.Address;
import com.ooad.cosmetics.entity.Cart;
import com.ooad.cosmetics.entity.CartItem;
import com.ooad.cosmetics.entity.CartStatus;
import com.ooad.cosmetics.entity.Order;
import com.ooad.cosmetics.entity.OrderItem;
import com.ooad.cosmetics.entity.OrderStatus;
import com.ooad.cosmetics.entity.Payment;
import com.ooad.cosmetics.entity.PaymentMethod;
import com.ooad.cosmetics.entity.PaymentStatus;
import com.ooad.cosmetics.entity.Product;
import com.ooad.cosmetics.entity.ProductImage;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class CheckoutService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final AddressRepository addressRepository;
    private final ProductImageRepository imageRepository;
    private final VoucherService voucherService;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PaymentRepository paymentRepository;
    private final StockService stockService;
    private final CurrentUserService currentUserService;

    public CheckoutService(
            CartRepository cartRepository,
            CartItemRepository cartItemRepository,
            AddressRepository addressRepository,
            ProductImageRepository imageRepository,
            VoucherService voucherService,
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            PaymentRepository paymentRepository,
            StockService stockService,
            CurrentUserService currentUserService
    ) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.addressRepository = addressRepository;
        this.imageRepository = imageRepository;
        this.voucherService = voucherService;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.paymentRepository = paymentRepository;
        this.stockService = stockService;
        this.currentUserService = currentUserService;
    }

    /** O06: xem trước đơn hàng, không ghi gì vào database. */
    @Transactional(readOnly = true)
    public CheckoutPreviewResponse preview(CheckoutPreviewRequest request) {
        User user = currentUserService.requireCurrentUser();
        Quote quote = prepare(user, request.addressId(), request.voucherCode(), false);

        List<CheckoutItemResponse> items = quote.lines().stream()
                .map(this::toCheckoutItem)
                .toList();

        return new CheckoutPreviewResponse(
                items,
                AddressResponse.from(quote.address()),
                quote.subtotal(),
                quote.shippingFee(),
                quote.discount(),
                quote.total(),
                quote.voucher() == null ? null : quote.voucher().getCode()
        );
    }

    /**
     * O13: tạo đơn hàng trong MỘT transaction.
     * Bất kỳ lỗi nào (hết hàng, voucher hết lượt, lỗi ghi database...) đều rollback toàn bộ:
     * tồn kho trả lại, không có đơn, không có lượt dùng voucher, giỏ hàng vẫn còn (O18).
     */
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        if (request.paymentMethod() != PaymentMethod.COD) {
            throw new BadRequestException("Only COD payment is supported at the moment");
        }

        User user = currentUserService.requireCurrentUser();

        // Tính lại toàn bộ ở backend (O09 - O12), khóa giỏ hàng chống bấm đặt hai lần.
        Quote quote = prepare(user, request.addressId(), request.voucherCode(), true);

        // O14 + O15: trừ kho nguyên tử. Điều kiện stock >= quantity được kiểm tra ngay trong
        // câu UPDATE nên đây vừa là bước "kiểm tra lần cuối" vừa là bước trừ kho.
        // Sắp xếp theo variantId để hai đơn cùng lúc khóa các dòng theo cùng thứ tự (tránh deadlock).
        List<Line> sortedLines = quote.lines().stream()
                .sorted(Comparator.comparing(line -> line.variant().getId()))
                .toList();
        for (Line line : sortedLines) {
            stockService.deduct(line.variant().getId(), line.item().getQuantity());
        }

        Order order = buildOrder(user, quote, request.note());
        orderRepository.save(order);

        List<OrderItem> orderItems = new ArrayList<>();
        for (Line line : quote.lines()) {
            orderItems.add(buildOrderItem(order, line));
        }
        List<OrderItem> savedItems = orderItemRepository.saveAll(orderItems);

        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setMethod(PaymentMethod.COD);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setAmount(quote.total());
        Payment savedPayment = paymentRepository.save(payment);

        // V12
        if (quote.voucher() != null) {
            voucherService.recordUsage(quote.voucher(), user, order, quote.discount());
        }

        OrderResponse response = OrderResponse.from(order, savedItems, savedPayment);

        // O17: dọn giỏ hàng sau khi mọi bước trên đã thành công.
        cartItemRepository.deleteAll(quote.cartItems());

        return response;
    }

    // ------------------------------------------------------------------

    private Quote prepare(User user, Long addressId, String voucherCode, boolean lockCart) {
        // O07: địa chỉ phải thuộc khách đang đăng nhập.
        Address address = addressRepository.findByIdAndUserId(addressId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Address", addressId));

        Cart cart = (lockCart
                ? cartRepository.lockByUserIdAndStatus(user.getId(), CartStatus.ACTIVE)
                : cartRepository.findByUserIdAndStatus(user.getId(), CartStatus.ACTIVE))
                .orElseThrow(() -> new BadRequestException("Cart is empty"));

        List<CartItem> cartItems = cartItemRepository.findByCartIdOrderByIdAsc(cart.getId());
        if (cartItems.isEmpty()) {
            throw new BadRequestException("Cart is empty");
        }

        List<Line> lines = new ArrayList<>();
        BigDecimal subtotal = new BigDecimal("0.00");

        for (CartItem item : cartItems) {
            ProductVariant variant = item.getVariant();
            String name = variant.getProduct().getName();

            if (!VariantAvailability.isSellable(variant)) {
                throw new BadRequestException("Product '" + name + "' is no longer available");
            }
            if (variant.getStock() < item.getQuantity()) {
                throw new BadRequestException(
                        "Not enough stock for '" + name + "' (available: " + variant.getStock() + ")"
                );
            }

            // O09: dùng giá hiện tại trong database, không tin giá phía client.
            BigDecimal lineTotal = variant.getPrice()
                    .multiply(BigDecimal.valueOf(item.getQuantity()));
            lines.add(new Line(item, variant, lineTotal));
            subtotal = subtotal.add(lineTotal);
        }

        Voucher voucher = null;
        BigDecimal discount = new BigDecimal("0.00");
        if (voucherCode != null && !voucherCode.isBlank()) {
            voucher = voucherService.validateForCheckout(voucherCode, user.getId(), subtotal);
            discount = OrderPricing.discount(voucher, subtotal);
        }

        BigDecimal shippingFee = OrderPricing.shippingFee(subtotal);
        BigDecimal total = OrderPricing.total(subtotal, shippingFee, discount);

        return new Quote(address, cartItems, lines, voucher, subtotal, shippingFee, discount, total);
    }

    /** O08: snapshot người nhận và địa chỉ giao hàng. */
    private Order buildOrder(User user, Quote quote, String note) {
        Address address = quote.address();

        Order order = new Order();
        order.setUser(user);
        order.setStatus(OrderStatus.PENDING);
        order.setReceiverName(address.getReceiverName());
        order.setReceiverPhone(address.getPhone());
        order.setShippingAddressLine(address.getAddressLine());
        order.setShippingWard(address.getWard());
        order.setShippingDistrict(address.getDistrict());
        order.setShippingCity(address.getCity());
        order.setSubtotal(quote.subtotal());
        order.setShippingFee(quote.shippingFee());
        order.setDiscountAmount(quote.discount());
        order.setTotalAmount(quote.total());
        order.setNote(normalizeNullable(note));

        if (quote.voucher() != null) {
            order.setVoucher(quote.voucher());
            order.setVoucherCode(quote.voucher().getCode());
        }
        return order;
    }

    /** O16: snapshot tên sản phẩm, SKU và đơn giá tại thời điểm đặt. */
    private OrderItem buildOrderItem(Order order, Line line) {
        ProductVariant variant = line.variant();

        OrderItem orderItem = new OrderItem();
        orderItem.setOrder(order);
        orderItem.setVariant(variant);
        orderItem.setProductName(variant.getProduct().getName());
        orderItem.setSku(variant.getSku());
        orderItem.setUnitPrice(variant.getPrice());
        orderItem.setQuantity(line.item().getQuantity());
        orderItem.setLineTotal(line.lineTotal());
        return orderItem;
    }

    private CheckoutItemResponse toCheckoutItem(Line line) {
        ProductVariant variant = line.variant();
        Product product = variant.getProduct();

        List<ProductImage> images = imageRepository
                .findByProductIdOrderByPrimaryImageDescSortOrderAscIdAsc(product.getId());
        String imageUrl = images.isEmpty() ? null : images.get(0).getImageUrl();

        return new CheckoutItemResponse(
                variant.getId(),
                product.getId(),
                product.getName(),
                variant.getSku(),
                variant.getPrice(),
                line.item().getQuantity(),
                line.lineTotal(),
                imageUrl
        );
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }

    private record Line(CartItem item, ProductVariant variant, BigDecimal lineTotal) {
    }

    private record Quote(
            Address address,
            List<CartItem> cartItems,
            List<Line> lines,
            Voucher voucher,
            BigDecimal subtotal,
            BigDecimal shippingFee,
            BigDecimal discount,
            BigDecimal total
    ) {
    }
}
