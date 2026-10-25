# API contract cho Admin Orders, Dashboard, Orders và Payment mock

Tài liệu này được đối chiếu với controller, DTO, service, `SecurityConfig` và exception handler hiện có. Tất cả ví dụ là JSON; giá trị ID, timestamp và dữ liệu là ví dụ.

## Quy ước chung

- Thành công trả `{ "success": true, "message": "...", "data": ..., "timestamp": "..." }`. `timestamp` là `Instant`. `message` mặc định là `"Success"`; các endpoint có message riêng được ghi trong ví dụ. `data` bị lược khỏi JSON khi null.
- Page có các field `content`, `page` (zero-based), `size`, `totalElements`, `totalPages`, `first`, `last`.
- Lỗi nghiệp vụ/không tìm thấy có dạng `{ "success": false, "message": "...", "timestamp": "..." }`. Lỗi validation body có `message: "Validation failed"` và `data: { "error": "Validation failed", "fields": { ... } }`.
- Các API `/api/admin/**` cần JWT role `ADMIN`; API khách hàng cần JWT hợp lệ (Customer sở hữu dữ liệu tương ứng). Thiếu xác thực trả `401 "Authentication required"`; thiếu quyền trả `403 "Access denied"`.
- Các lỗi chung khác do handler: `500 "An unexpected error occurred"`. Các lỗi validation/query dùng HTTP `400`; message nghiệp vụ cụ thể bên dưới.

## Admin Orders

### `GET /api/admin/orders`

- **Role:** `ADMIN`.
- **Query params:**

| Tên | Kiểu | Bắt buộc | Mặc định | Giá trị/ý nghĩa |
|---|---|---:|---|---|
| `status` | `OrderStatus` | Không | — | Một trong các giá trị enum ở cuối tài liệu |
| `keyword` | string | Không | — | Tìm trong ID số đơn, email khách, tên người nhận hoặc số điện thoại |
| `fromDate` | `LocalDate` | Không | — | `yyyy-MM-dd`, bao gồm ngày này; ngày được diễn giải theo `Asia/Ho_Chi_Minh` |
| `toDate` | `LocalDate` | Không | — | `yyyy-MM-dd`, bao gồm trọn ngày này theo `Asia/Ho_Chi_Minh` |
| `page` | integer | Không | `0` | `>= 0` |
| `size` | integer | Không | `20` | `1..100` |
| `sort` | — | — | — | **Chưa được controller hỗ trợ.** Kết quả hiện sắp `createdAt DESC`, rồi `id DESC`; truyền `sort` không đổi thứ tự. |

- **Request body:** không có.
- **Response mẫu:**

```json
{
  "success": true,
  "message": "Success",
  "data": {
    "content": [{
      "id": 120,
      "status": "PENDING",
      "receiverName": "Nguyen Van A",
      "customerEmail": "a@example.com",
      "itemCount": 2,
      "firstItemName": "Serum Vitamin C",
      "totalAmount": 520000.00,
      "paymentMethod": "ONLINE",
      "paymentStatus": "PENDING",
      "createdAt": "2026-10-24T08:00:00Z"
    }],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1,
    "first": true,
    "last": true
  },
  "timestamp": "2026-10-24T08:01:00Z"
}
```

- **Lỗi:** `400` — `from must not be after to`; query/page không hợp lệ trả lỗi validation `400 "Validation failed"` hoặc lỗi bind tham số. `401 "Authentication required"`; `403 "Access denied"`; `500 "An unexpected error occurred"`.

### `GET /api/admin/orders/{id}`

- **Role:** `ADMIN`.
- **Path:** `id` là `Long`, bắt buộc.
- **Query params:** không có.
- **Request body:** không có.
- **Response mẫu (đủ field của `AdminOrderResponse` và `OrderResponse`):**

```json
{
  "success": true,
  "message": "Success",
  "data": {
    "customerId": 18,
    "customerEmail": "a@example.com",
    "customerName": "Nguyen Van A",
    "order": {
      "id": 120,
      "status": "PENDING",
      "receiverName": "Nguyen Van A",
      "receiverPhone": "0900000000",
      "shippingAddressLine": "12 Nguyen Trai",
      "shippingWard": "Ben Thanh",
      "shippingDistrict": "District 1",
      "shippingCity": "Ho Chi Minh City",
      "items": [{
        "id": 350,
        "variantId": 40,
        "productName": "Serum Vitamin C",
        "sku": "SERUM-C-30",
        "unitPrice": 250000.00,
        "quantity": 2,
        "lineTotal": 500000.00
      }],
      "subtotal": 500000.00,
      "shippingFee": 20000.00,
      "discountAmount": 0.00,
      "totalAmount": 520000.00,
      "voucherCode": null,
      "note": null,
      "payment": {
        "method": "ONLINE",
        "status": "PENDING",
        "amount": 520000.00,
        "transactionRef": "MOCK-0123456789ABCDEFGHIJ",
        "paidAt": null
      },
      "createdAt": "2026-10-24T08:00:00Z",
      "deliveredAt": null,
      "cancelledAt": null
    }
  },
  "timestamp": "2026-10-24T08:01:00Z"
}
```

- **Lỗi:** `404 "Order not found: {id}"`; `401 "Authentication required"`; `403 "Access denied"`; `500 "An unexpected error occurred"`.

### `PATCH /api/admin/orders/{id}/status`

- **Role:** `ADMIN`.
- **Path:** `id` là `Long`, bắt buộc.
- **Query params:** không có.
- **Request body JSON:**

```json
{ "status": "CONFIRMED" }
```

`status` bắt buộc, là một giá trị `OrderStatus`.

- **Response mẫu:** trả toàn bộ `OrderResponse` như object `order` ở endpoint detail (không bọc `AdminOrderResponse`), với `message: "Order status updated"`.
- **Lỗi:** `404 "Order not found: {id}"`; `400 "Cannot change order status from {current} to {next}"`; khi chuyển đơn online sang `CONFIRMED` mà chưa PAID, `400 "Online payment has not been completed"`; body thiếu/enum không hợp lệ trả `400 "Validation failed"` (message field do validation cung cấp); `401 "Authentication required"`; `403 "Access denied"`; `500 "An unexpected error occurred"`.

## Dashboard

Các endpoint dưới đây đều cần role `ADMIN`.

### `GET /api/admin/dashboard/summary`

- **Query params:** `lowStockThreshold` — integer, không bắt buộc, mặc định `10`, tối thiểu `0`; ngưỡng tồn kho inclusive.
- **Request body:** không có.
- **Response mẫu (đủ field):**

```json
{
  "success": true,
  "message": "Success",
  "data": {
    "totalCustomers": 25,
    "totalProducts": 80,
    "totalOrders": 42,
    "totalRevenue": 12500000.00,
    "ordersByStatus": {
      "PENDING": 2, "CONFIRMED": 3, "SHIPPING": 1, "DELIVERED": 34, "CANCELLED": 2
    },
    "lowStockThreshold": 10,
    "lowStockVariants": [{
      "id": 40,
      "productId": 9,
      "productName": "Serum Vitamin C",
      "sku": "SERUM-C-30",
      "price": 250000.00,
      "stock": 3,
      "status": "ACTIVE"
    }]
  },
  "timestamp": "2026-10-24T08:01:00Z"
}
```

- **Lỗi:** threshold ngoài giới hạn trả `400 "Validation failed"` (field validation); `401 "Authentication required"`; `403 "Access denied"`; `500 "An unexpected error occurred"`.

### `GET /api/admin/dashboard/revenue`

- **Query params:**

| Tên | Kiểu | Bắt buộc | Mặc định | Giá trị/ý nghĩa |
|---|---|---:|---|---|
| `from` | `LocalDate` | Không | `to - 29 ngày` | `yyyy-MM-dd`; đầu kỳ inclusive |
| `to` | `LocalDate` | Không | Hôm nay theo `Asia/Ho_Chi_Minh` | `yyyy-MM-dd`; cuối kỳ inclusive |
| `groupBy` | `RevenueGrouping` | Không | `DAY` | `DAY` hoặc `MONTH`; tham số tên chính xác là `groupBy` |

Khoảng ngày không được dài quá 366 ngày chênh lệch. Chỉ tính đơn `DELIVERED` theo ngày giao; bucket không có đơn vẫn có mặt, revenue/count bằng 0.

- **Request body:** không có.
- **Response mẫu:** mảng từng điểm, mỗi phần tử đủ các field `period`, `revenue`, `orderCount`. `period` là `yyyy-MM-dd` cho DAY và `yyyy-MM` cho MONTH.

```json
{
  "success": true,
  "message": "Success",
  "data": [
    { "period": "2026-10-23", "revenue": 0.00, "orderCount": 0 },
    { "period": "2026-10-24", "revenue": 520000.00, "orderCount": 1 }
  ],
  "timestamp": "2026-10-24T08:01:00Z"
}
```

- **Lỗi:** `400 "from must not be after to"`; `400 "Date range must not exceed 366 days"`; sai định dạng ngày hoặc grouping trả `400` do bind tham số; `401 "Authentication required"`; `403 "Access denied"`; `500 "An unexpected error occurred"`.

### `GET /api/admin/dashboard/top-products`

- **Query params:** `limit` — integer, không bắt buộc, mặc định `5`, hợp lệ `1..50`.
- **Request body:** không có.
- **Response mẫu:** `data` là mảng; mỗi điểm có đủ `productId`, `productName`, `quantitySold`, `revenue`.

```json
{
  "success": true,
  "message": "Success",
  "data": [{
    "productId": 9,
    "productName": "Serum Vitamin C",
    "quantitySold": 18,
    "revenue": 4500000.00
  }],
  "timestamp": "2026-10-24T08:01:00Z"
}
```

- **Lỗi:** `400 "limit must be between 1 and 50"`; `401 "Authentication required"`; `403 "Access denied"`; `500 "An unexpected error occurred"`.

### `GET /api/admin/inventory/low-stock` (API low-stock)

API này nằm trong **`InventoryController`**, không phải Dashboard controller. Chỉ liệt kê variant ACTIVE có `stock <= threshold`, sắp stock tăng dần rồi ID tăng dần.

- **Query params:** `threshold` integer default `10`, `>= 0`; `page` integer default `0`, `>= 0`; `size` integer default `20`, `1..100`.
- **Request body:** không có.
- **Response mẫu:** bọc `PageResponse<ProductVariantResponse>`; một item có đủ field như sau:

```json
{
  "success": true,
  "message": "Success",
  "data": {
    "content": [{
      "id": 40, "productId": 9, "productName": "Serum Vitamin C",
      "sku": "SERUM-C-30", "price": 250000.00, "stock": 3, "status": "ACTIVE"
    }],
    "page": 0, "size": 20, "totalElements": 1, "totalPages": 1,
    "first": true, "last": true
  },
  "timestamp": "2026-10-24T08:01:00Z"
}
```

- **Lỗi:** query ngoài giới hạn trả `400 "Validation failed"` (field validation); `401 "Authentication required"`; `403 "Access denied"`; `500 "An unexpected error occurred"`.

## Checkout và Customer Orders

### `POST /api/checkout/preview`

- **Role:** Customer đã đăng nhập.
- **Query params:** không có.
- **Request body JSON:**

```json
{ "addressId": 7, "voucherCode": "WELCOME10" }
```

`addressId` là `Long`, bắt buộc; `voucherCode` là string tùy chọn, tối đa 50 ký tự.

- **Response mẫu (đủ field):**

```json
{
  "success": true,
  "message": "Success",
  "data": {
    "items": [{
      "variantId": 40, "productId": 9, "productName": "Serum Vitamin C",
      "sku": "SERUM-C-30", "unitPrice": 250000.00, "quantity": 2,
      "lineTotal": 500000.00, "imageUrl": "https://example.com/serum.jpg"
    }],
    "shippingAddress": {
      "id": 7, "receiverName": "Nguyen Van A", "phone": "0900000000",
      "addressLine": "12 Nguyen Trai", "ward": "Ben Thanh",
      "district": "District 1", "city": "Ho Chi Minh City",
      "defaultAddress": true, "createdAt": "2026-10-20T08:00:00Z",
      "updatedAt": "2026-10-20T08:00:00Z"
    },
    "subtotal": 500000.00, "shippingFee": 20000.00,
    "discountAmount": 50000.00, "totalAmount": 470000.00,
    "voucherCode": "WELCOME10"
  },
  "timestamp": "2026-10-24T08:01:00Z"
}
```

- **Lỗi:** `404 "Address not found: {addressId}"`; `400 "Cart is empty"`; `400 "Product '{name}' is no longer available"`; `400 "Not enough stock for '{name}' (available: {stock})"`; voucher không hợp lệ có thể trả `400` với một trong các message: `"Voucher code is invalid"`, `"Voucher is not active"`, `"Voucher is not yet valid"`, `"Voucher has expired"`, `"Voucher usage limit reached"`, `"Order value is below the voucher minimum ({minOrderValue})"`, `"You have already used this voucher"`; body validation trả `400 "Validation failed"`; `401 "Authentication required"`; `500 "An unexpected error occurred"`.

### `POST /api/orders`

- **Role:** Customer đã đăng nhập.
- **Query params:** không có.
- **Request body JSON:**

```json
{ "addressId": 7, "voucherCode": "WELCOME10", "paymentMethod": "ONLINE", "note": "Gọi trước khi giao" }
```

`addressId` và `paymentMethod` bắt buộc; `paymentMethod` nhận `COD` hoặc `ONLINE`; `voucherCode` tối đa 50 ký tự; `note` tối đa 500 ký tự.

- **HTTP:** `201 Created`.
- **Response mẫu:** bọc `ApiResponse<OrderResponse>` như response detail bên dưới, đủ field; message `"Order created"`. Với đơn mới, `status` là `PENDING`, payment là `PENDING`; phương thức ONLINE được cấp `transactionRef` mock.
- **Lỗi:** gồm lỗi địa chỉ/giỏ/sản phẩm/tồn kho/voucher giống preview; thêm body validation `400 "Validation failed"`; `401 "Authentication required"`; `500 "An unexpected error occurred"`. Giao dịch thất bại được rollback.

### `GET /api/orders`

- **Role:** Customer đã đăng nhập; chỉ trả đơn của người dùng hiện tại.
- **Query params:** `status` (`OrderStatus`, tùy chọn); `page` integer default `0`, `>= 0`; `size` integer default `10`, `1..100`.
- **Request body:** không có.
- **Response mẫu:** `ApiResponse<PageResponse<OrderSummaryResponse>>`; mỗi phần tử `content` có đủ field:

```json
{
  "success": true, "message": "Success",
  "data": {
    "content": [{
      "id": 120, "status": "PENDING", "receiverName": "Nguyen Van A",
      "customerEmail": "a@example.com", "itemCount": 2,
      "firstItemName": "Serum Vitamin C", "totalAmount": 520000.00,
      "paymentMethod": "ONLINE", "paymentStatus": "PENDING",
      "createdAt": "2026-10-24T08:00:00Z"
    }],
    "page": 0, "size": 10, "totalElements": 1, "totalPages": 1,
    "first": true, "last": true
  },
  "timestamp": "2026-10-24T08:01:00Z"
}
```

- **Lỗi:** query/page không hợp lệ trả `400` do bind/validation; `401 "Authentication required"`; `500 "An unexpected error occurred"`.

### `GET /api/orders/{id}`

- **Role:** Customer đã đăng nhập; đơn phải thuộc user hiện tại.
- **Query params:** không có. **Request body:** không có.
- **Response mẫu:** `ApiResponse<OrderResponse>` đủ field: `id`, `status`, `receiverName`, `receiverPhone`, `shippingAddressLine`, `shippingWard`, `shippingDistrict`, `shippingCity`, `items` (mỗi item có `id`, `variantId`, `productName`, `sku`, `unitPrice`, `quantity`, `lineTotal`), `subtotal`, `shippingFee`, `discountAmount`, `totalAmount`, `voucherCode`, `note`, `payment` (`method`, `status`, `amount`, `transactionRef`, `paidAt`), `createdAt`, `deliveredAt`, `cancelledAt`. Ví dụ object được bọc ở `data.order` trong response Admin detail ở trên; tại đây object ấy chính là `data`.
- **Lỗi:** `404 "Order not found: {id}"` (không tìm thấy hoặc không thuộc người dùng); `401 "Authentication required"`; `500 "An unexpected error occurred"`.

### `PUT /api/orders/{id}/cancel`

- **Role:** Customer đã đăng nhập; chỉ đơn của user hiện tại.
- **Query params:** không có. **Request body:** không có.
- **Response mẫu:** `ApiResponse<OrderResponse>` đủ field như GET detail; message `"Order cancelled"`.
- **Lỗi:** `404 "Order not found: {id}"`; `400 "Order is already cancelled"`; `400 "Order can no longer be cancelled"` (chỉ PENDING/CONFIRMED có thể hủy); `401 "Authentication required"`; `500 "An unexpected error occurred"`.

## Payment mock

Đây là mô phỏng, không kết nối cổng thanh toán. Sau `POST /api/orders` với `paymentMethod: ONLINE`, order ở `PENDING` và payment được tạo với `PENDING` cùng mã `MOCK-...`. FE có thể GET trạng thái rồi gọi một trong hai POST; `confirm` đổi payment thành `PAID` và đặt `paidAt`, `fail` đổi thành `FAILED`. Đơn vẫn `PENDING` sau cả hai thao tác. Sau `FAILED`, có thể thử lại bằng confirm/fail; sau `PAID` không thể thao tác lại. Chỉ khi payment PAID, Admin mới chuyển order từ PENDING sang CONFIRMED. Payment COD không được confirm/fail qua mock. Khách cũng có thể hủy đơn PENDING.

### `GET /api/orders/{id}/payment`

- **Role:** Customer đã đăng nhập; order phải thuộc user.
- **Query params/body:** không có.
- **Response:** `ApiResponse<PaymentResponse>`:

```json
{
  "success": true, "message": "Success",
  "data": {
    "method": "ONLINE", "status": "PENDING", "amount": 520000.00,
    "transactionRef": "MOCK-0123456789ABCDEFGHIJ", "paidAt": null
  },
  "timestamp": "2026-10-24T08:01:00Z"
}
```

- **Lỗi:** `404 "Order not found: {id}"`; `404 "Payment not found: {id}"`; `401 "Authentication required"`; `500 "An unexpected error occurred"`.

### `POST /api/orders/{id}/payment/confirm`

- **Role:** Customer đã đăng nhập, sở hữu order.
- **Query params/body:** không có.
- **Response:** `ApiResponse<PaymentResponse>` cùng shape trên, message `"Payment confirmed"`; `status: "PAID"`, `paidAt` là thời điểm xác nhận.
- **Lỗi:** `404 "Order not found: {id}"`; `404 "Payment not found: {id}"`; `400 "This order is not paid online"`; `400 "Order is already paid"`; `400 "Order is not awaiting payment"`; `401 "Authentication required"`; `500 "An unexpected error occurred"`.

### `POST /api/orders/{id}/payment/fail`

- **Role:** Customer đã đăng nhập, sở hữu order.
- **Query params/body:** không có.
- **Response:** `ApiResponse<PaymentResponse>` cùng shape trên, message `"Payment marked as failed"`; `status: "FAILED"`, `paidAt` vẫn null.
- **Lỗi:** `404 "Order not found: {id}"`; `404 "Payment not found: {id}"`; `400 "This order is not paid online"`; `400 "Order is already paid"`; `400 "Order is not awaiting payment"`; `401 "Authentication required"`; `500 "An unexpected error occurred"`.

## Admin Review

Các endpoint cần role `ADMIN`.

### `GET /api/admin/reviews`

- **Query params:** `status` (`ReviewStatus`, tùy chọn); `productId` (`Long`, tùy chọn); `page` integer default `0`, `>= 0`; `size` integer default `20`, `1..100`.
- **Request body:** không có.
- **Response:** `ApiResponse<PageResponse<AdminReviewResponse>>`; `content` item gồm đủ fields:

```json
{
  "success": true, "message": "Success",
  "data": {
    "content": [{
      "id": 31, "productId": 9, "productName": "Serum Vitamin C",
      "orderItemId": 350, "customerId": 18, "customerEmail": "a@example.com",
      "customerName": "Nguyen Van A", "rating": 5, "comment": "Rất tốt",
      "status": "VISIBLE", "createdAt": "2026-10-24T08:00:00Z"
    }],
    "page": 0, "size": 20, "totalElements": 1, "totalPages": 1,
    "first": true, "last": true
  },
  "timestamp": "2026-10-24T08:01:00Z"
}
```

- **Lỗi:** tham số/page không hợp lệ trả `400` do bind/validation; `401 "Authentication required"`; `403 "Access denied"`; `500 "An unexpected error occurred"`.

### `PATCH /api/admin/reviews/{id}/status`

- **Query params:** `status` là bắt buộc, kiểu `ReviewStatus` (`VISIBLE` hoặc `HIDDEN`).
- **Request body:** không có; status được truyền qua query string.
- **Response:** `ApiResponse<AdminReviewResponse>` đầy đủ field như một phần tử list; message `"Review status updated"`.
- **Lỗi:** `404 "Review not found: {id}"`; thiếu/sai status trả `400` do bind tham số; `401 "Authentication required"`; `403 "Access denied"`; `500 "An unexpected error occurred"`.

## Admin Voucher

Các endpoint cần role `ADMIN`.

### `GET /api/admin/vouchers`

- **Query params:** `q` string tùy chọn; `status` (`CatalogStatus`, tùy chọn); `page` integer default `0`, `>= 0`; `size` integer default `20`, `1..100`.
- **Body:** không có.
- **Response:** `ApiResponse<PageResponse<VoucherResponse>>`; `content` items dùng đủ field response ở các endpoint voucher bên dưới.
- **Lỗi:** tham số/page không hợp lệ trả `400` do bind/validation; `401 "Authentication required"`; `403 "Access denied"`; `500 "An unexpected error occurred"`.

### `GET /api/admin/vouchers/{id}`

- **Path:** `id` là `Long`, bắt buộc. Không query/body.
- **Response:** `ApiResponse<VoucherResponse>`:

```json
{
  "success": true, "message": "Success",
  "data": {
    "id": 6, "code": "WELCOME10", "description": "Discount for new customers",
    "discountType": "PERCENTAGE", "discountValue": 10.00,
    "minOrderValue": 100000.00, "maxDiscount": 50000.00,
    "usageLimit": 100, "usedCount": 4,
    "startDate": "2026-10-01T00:00:00Z", "endDate": "2026-12-31T23:59:59Z",
    "status": "ACTIVE", "createdAt": "2026-09-20T08:00:00Z",
    "updatedAt": "2026-09-20T08:00:00Z"
  },
  "timestamp": "2026-10-24T08:01:00Z"
}
```

- **Lỗi:** `404 "Voucher not found: {id}"`; `401 "Authentication required"`; `403 "Access denied"`; `500 "An unexpected error occurred"`.

### `POST /api/admin/vouchers`

- **Request body JSON** (cũng là body cho PUT):

```json
{
  "code": "WELCOME10", "description": "Discount for new customers",
  "discountType": "PERCENTAGE", "discountValue": 10.00,
  "minOrderValue": 100000.00, "maxDiscount": 50000.00,
  "usageLimit": 100,
  "startDate": "2026-10-01T00:00:00Z", "endDate": "2026-12-31T23:59:59Z"
}
```

`code` bắt buộc, 3–50 ký tự, chỉ chữ/số/`_`/`-`; `description` tùy chọn tối đa 255; `discountType` và `discountValue` bắt buộc, value > 0; `minOrderValue` tùy chọn >= 0; `maxDiscount` tùy chọn > 0; `usageLimit` tùy chọn >= 1; `startDate`, `endDate` bắt buộc là Instant.

- **HTTP:** `201 Created`.
- **Response:** `ApiResponse<VoucherResponse>` shape voucher ở GET detail, message `"Voucher created"`. Voucher có đủ field `id`, `code`, `description`, `discountType`, `discountValue`, `minOrderValue`, `maxDiscount`, `usageLimit`, `usedCount`, `startDate`, `endDate`, `status`, `createdAt`, `updatedAt`.
- **Lỗi:** `400 "Voucher code already exists"`; `400 "startDate must be before endDate"`; `400 "Percentage discount must not exceed 100"`; lỗi ràng buộc body trả `400 "Validation failed"` với `data.error`/`data.fields`; `401 "Authentication required"`; `403 "Access denied"`; `500 "An unexpected error occurred"`.

### `PUT /api/admin/vouchers/{id}`

- **Path:** `id` là `Long`, bắt buộc. **Body:** cùng JSON/validation của POST; thay thế trường voucher theo request.
- **Response:** `ApiResponse<VoucherResponse>` đầy đủ voucher fields, message `"Voucher updated"`.
- **Lỗi:** `404 "Voucher not found: {id}"`; `400 "Voucher code already exists"`; `400 "usageLimit must not be lower than the number of times the voucher has been used"`; `400 "startDate must be before endDate"`; `400 "Percentage discount must not exceed 100"`; validation body trả `400 "Validation failed"`; `401 "Authentication required"`; `403 "Access denied"`; `500 "An unexpected error occurred"`.

### `PATCH /api/admin/vouchers/{id}/status`

- **Query params:** `status` bắt buộc, `CatalogStatus`: `ACTIVE` hoặc `INACTIVE`.
- **Body:** không có.
- **Response:** `ApiResponse<VoucherResponse>` đầy đủ voucher fields, message `"Voucher status updated"`.
- **Lỗi:** `404 "Voucher not found: {id}"`; thiếu/sai status trả `400` do bind tham số; `401 "Authentication required"`; `403 "Access denied"`; `500 "An unexpected error occurred"`.

## Enums, tiền và thời gian

| Enum | Giá trị |
|---|---|
| `OrderStatus` | `PENDING`, `CONFIRMED`, `SHIPPING`, `DELIVERED`, `CANCELLED` |
| `PaymentStatus` | `PENDING`, `PAID`, `FAILED`, `REFUNDED` |
| `PaymentMethod` | `COD`, `ONLINE` |
| `DiscountType` | `FIXED`, `PERCENTAGE` |
| `ReviewStatus` | `VISIBLE`, `HIDDEN` |
| `RevenueGrouping` | `DAY`, `MONTH` |
| `CatalogStatus` | `ACTIVE`, `INACTIVE` |

### Chuyển trạng thái Order hợp lệ

| Từ | Sang hợp lệ |
|---|---|
| `PENDING` | `CONFIRMED`, `CANCELLED` |
| `CONFIRMED` | `SHIPPING`, `CANCELLED` |
| `SHIPPING` | `DELIVERED` |
| `DELIVERED` | Không chuyển tiếp |
| `CANCELLED` | Không chuyển tiếp |

Riêng chuyển ONLINE `PENDING -> CONFIRMED` đòi payment `PAID`. Khi chuyển `DELIVERED`, payment COD đang `PENDING` tự thành `PAID`. Hủy đơn hoàn kho; payment đã PAID đổi thành `REFUNDED`.

- **Tiền:** các trường tiền dùng `BigDecimal`, JSON number (không phải chuỗi); ví dụ hiển thị hai chữ số thập phân. Voucher được scale 2 chữ số bằng HALF_UP. FE nên parse dạng decimal, tránh tính toán bằng floating-point.
- **Thời gian:** các DTO dùng `Instant`, serialize ISO-8601 UTC (đuôi `Z`), ví dụ `2026-10-24T08:00:00Z`. Input voucher Instant phải là ISO-8601 có timezone/offset. `from`/`to`/`fromDate`/`toDate` là ngày lịch ISO `yyyy-MM-dd`, hiểu theo múi giờ nghiệp vụ `Asia/Ho_Chi_Minh`.

## Những phần FE cần nhưng API hiện chưa có

- `GET /api/admin/orders` chưa nhận query `sort`; chỉ sort mặc định mới nhất. Nếu UI cần tùy chỉnh cột sort, cần bổ sung API riêng.
- Admin order list **đã có** `customerEmail` và `receiverName`; không có field tên khách riêng (không nhất thiết bằng người nhận). Admin detail có `customerName`/`customerEmail`.
- Admin order detail **đã có** payment, items, snapshot địa chỉ giao hàng, voucher code và số tiền discount; không trả nguyên object/metadata voucher (chỉ `voucherCode`, `discountAmount`).
- Không có API xem chi tiết payment/voucher liên kết bổ sung ngoài response order/payment đã nêu; không có tích hợp gateway thật (payment mock chỉ đổi trạng thái nội bộ).
