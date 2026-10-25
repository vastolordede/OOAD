# Hướng dẫn chạy test (phần Cart, Order, Voucher, Review, Dashboard)

Backend hiện chỉ khai báo Gradle task `test` và chưa có task `integrationTest`. Task `test` chạy mọi test JUnit được phát hiện, bao gồm cả lớp integration có `@Tag("integration")`; tag hiện chưa được dùng để loại chúng khỏi task. Vì vậy, chạy toàn bộ `test` cần PostgreSQL test riêng.

Để chạy một nhóm unit test không cần database, dùng các bộ lọc theo lớp:

```powershell
cd backend
.\gradlew.bat test --tests "*OrderStatusTest" --tests "*OrderPricingTest" --tests "*ReviewServiceTest" --tests "*VoucherServiceTest" --tests "*DashboardServiceTest" --tests "*CartServiceTest" --tests "*OrderServiceTest"
```

Để chỉ chạy integration test, dùng task `test` với bộ lọc `--tests` tương ứng sau khi cấu hình PostgreSQL test như bên dưới. Không dùng lệnh `gradlew.bat integrationTest` cho đến khi task đó được khai báo trong `build.gradle`.

## 1. Tạo database test (làm một lần)

Integration test chạy trên một database **riêng**, tên có chứa chữ `test`. Test sẽ **xóa sạch dữ liệu** của database này trước mỗi bài, vì vậy tuyệt đối không dùng database đang chạy ứng dụng.

```powershell
psql -U postgres -c "CREATE DATABASE ooad_cosmetics_test;"
```

Không cần tạo bảng. Lần chạy đầu, Flyway tự tạo toàn bộ bảng.

## 2. Cấu hình kết nối

File `backend/src/test/resources/application-test.properties` mặc định dùng:

- URL: `jdbc:postgresql://localhost:5432/ooad_cosmetics_test`
- Tài khoản và mật khẩu: lấy từ `DB_USERNAME` và `DB_PASSWORD` trong `backend/.env` (cùng tài khoản đang chạy ứng dụng).

Muốn đổi, đặt biến môi trường `TEST_DB_URL`, `TEST_DB_USERNAME`, `TEST_DB_PASSWORD` trước khi chạy.

**Chốt an toàn:** nếu URL không chứa chữ `test`, mọi integration test dừng ngay và không xóa gì.

## 3. Chạy

```powershell
cd backend
.\gradlew.bat test --tests "*OrderStatusTest" --tests "*OrderPricingTest" --tests "*ReviewServiceTest" --tests "*VoucherServiceTest" --tests "*DashboardServiceTest" --tests "*CartServiceTest" --tests "*OrderServiceTest"
.\gradlew.bat test --tests "*CheckoutOrderIntegrationTest"  # integration; requires dedicated test database
```

Chạy một lớp integration test khác bằng cách thay bộ lọc `--tests`. Không chạy toàn bộ task `test` nếu chưa cấu hình database test.

## 4. Các bài test và việc trong sheet

| Việc | Test |
|---|---|
| C17 Test Cart API và Stock Validation | `CartIntegrationTest`, `CartServiceTest`, `StockServiceTest` |
| O43 Order Transaction Rollback | `CheckoutOrderIntegrationTest` (lỗi khi lưu thanh toán, lỗi khi ghi voucher, hết hàng trước lúc đặt) |
| O44 Cancel Order và Restore Stock | `CheckoutOrderIntegrationTest` (khách hủy, admin hủy, hoàn kho, trả voucher) |
| O45 Invalid Order Status Transition | `CheckoutOrderIntegrationTest`, `OrderStatusTest`, `OrderServiceTest` |
| O47 Hai request mua vượt Stock | `CheckoutOrderIntegrationTest` (8 người tranh 3 sản phẩm, 2 người tranh 1 lượt voucher, bấm đặt hai lần) |
| V16 Voucher Business Rules | `VoucherCheckoutIntegrationTest`, `VoucherServiceTest`, `OrderPricingTest` |
| V05–V12 Voucher Rule Coverage | Ma trận cụ thể ở mục 5; gồm kiểm tra ngày/giá trị, giới hạn, làm tròn, ghi nhận, hủy và dùng lại |
| R14 Review Eligibility và Rating | `ReviewIntegrationTest`, `ReviewServiceTest` |
| DB14 Dashboard Query | `DashboardIntegrationTest`, `DashboardServiceTest` |
| DB09 Low-stock API | `GET /api/admin/inventory/low-stock`; covered by `DashboardIntegrationTest` |
| DB10–DB11 Revenue and top-products APIs | `GET /api/admin/dashboard/revenue` and `/api/admin/dashboard/top-products`; covered by `DashboardIntegrationTest` |
| O25–O30 Admin Order HTTP | `DemoFlowApiIntegrationTest` (401/403, list/filter/detail, chuyển trạng thái, COD PAID, hủy hoàn kho) |
| DEP07 Demo seed integrity | `DemoSeedIntegrationTest` (database riêng, profile `demo`) |
| T15 Database constraints và invalid data | `DatabaseConstraintsIntegrationTest` |
| T11 Regression Backend | Toàn bộ test trên, cộng `DemoFlowApiIntegrationTest` |

`DemoFlowApiIntegrationTest` đi qua HTTP thật: đăng ký, đăng nhập, địa chỉ, giỏ hàng, xem trước đơn, đặt đơn, Admin đưa đơn qua từng bước, review, dashboard, kèm kiểm tra 401 và 403.

## 5. Frontend regression (T05-T09, T13)

Frontend hiện chưa có test runner riêng; các bước dưới đây là kiểm thử thủ công trên UI, cần backend và PostgreSQL đang chạy. Không đánh dấu hoàn thành các ca API chỉ dựa trên việc build thành công.

| Việc | Luồng kiểm tra | Kết quả mong đợi |
|---|---|---|
| T05 | Customer đăng nhập → mở `/products/{id}` → thêm variant vào cart → cập nhật số lượng → checkout → tạo order COD | Cart/checkout hiển thị dữ liệu backend; giá và tồn kho được backend xác nhận; tạo đơn không gửi lặp |
| T06 | Admin cập nhật trạng thái đơn → Customer tải lại My Orders | Admin API `PATCH /api/admin/orders/{id}/status` đã được bổ sung; xác minh UI gọi API và tải lại trạng thái đơn |
| T07 | Hủy đơn PENDING/CONFIRMED trong Order Detail; kiểm tra stock sau đó | UI xác nhận trước khi hủy, trạng thái được tải lại; kiểm tra tồn kho khôi phục bằng backend/integration test |
| T08 | Checkout nhập voucher hợp lệ, voucher sai, gỡ voucher | Preview backend cập nhật tổng; mã sai báo lỗi; gỡ mã gọi preview không có voucher |
| T09 | Dùng đơn DELIVERED chưa review để gửi rating 1–5; thử gửi trùng OrderItem | Form chỉ xuất hiện cho DELIVERED; gửi trùng/rating không hợp lệ hiển thị lỗi backend |
| T13 | Mở lần lượt Cart, Checkout, My Orders, Order Detail, My Reviews, Wishlist, Payment simulation, Dashboard, Voucher, Review Moderation ở desktop và mobile | Không vỡ layout; loading/error/empty state và điều hướng hoạt động |
| R17 | Mở `/my-reviews`, sửa rating/comment, xóa review sau xác nhận | PUT/DELETE cập nhật danh sách; lỗi backend được hiển thị; không xóa khi người dùng hủy xác nhận |
| W05 | Thêm sản phẩm vào wishlist từ Product Detail, tải `/wishlist`, xóa sản phẩm và phân trang | Chỉ lưu sản phẩm cho Customer hiện tại; giá/khả dụng lấy từ backend; trạng thái rỗng/lỗi hiển thị rõ |
| O49 | Checkout chọn ONLINE; trên màn hình thanh toán thử confirm và fail; thử lại sau FAILED | Payment status đồng bộ với API; chỉ PENDING được thao tác; UI ghi rõ là mô phỏng, không có giao dịch thật |

Ghi lại API response/lỗi và bước tái hiện khi test lỗi. Dùng dữ liệu/tài khoản test cục bộ, không đưa mật khẩu/token vào repository. Xem [frontend demo script](frontend-order-business-demo.md) để chuẩn bị kịch bản và [frontend handoff](frontend-order-business-handoff.md) để biết phần còn chặn.

Các màn hình O36 (Create Order), R13 (Admin Review Moderation) và DB09 (Low Stock Widget) đã được triển khai trong code trước đó, dù một số dòng sheet đang hiển thị `Chưa Bắt Đầu`. Hash tham chiếu lần lượt là `cd1eb12`, `4052afc`, `583e25c`; hãy xác minh trạng thái sheet thay vì tạo lại.

O40–O42/T06 không còn bị chặn bởi backend: Admin Order HTTP API đã có list/filter/detail/status, và Dashboard API có `/api/admin/dashboard/revenue` cùng `/api/admin/dashboard/top-products`. Vẫn cần chạy regression UI để xác nhận frontend tích hợp hai bề mặt này. O49 là mô phỏng, không thay thế cổng thanh toán thật.

Hợp đồng chi tiết và các bề mặt FE chưa được hỗ trợ được ghi trong [api-contract-admin](api-contract-admin.md).

### Kiểm tra CORS preflight cho Admin Order PATCH

`backend/.env.example` cấu hình `CORS_ALLOWED_ORIGINS=http://localhost:5173`; `application.properties` lấy giá trị này với mặc định cùng origin. `SecurityConfig` cho phép `GET, POST, PUT, PATCH, DELETE, OPTIONS` và các header `Authorization`, `Content-Type`, `Accept`.

Khi backend đang chạy, gửi preflight từ PowerShell (hoặc `curl.exe`):

```powershell
curl.exe -i -X OPTIONS "http://localhost:8080/api/admin/orders/1/status" `
  -H "Origin: http://localhost:5173" `
  -H "Access-Control-Request-Method: PATCH" `
  -H "Access-Control-Request-Headers: authorization,content-type"
```

Kết quả cần là HTTP thành công cho preflight và có `Access-Control-Allow-Origin: http://localhost:5173`, `Access-Control-Allow-Methods` chứa `PATCH`, cùng `Access-Control-Allow-Headers` cho `authorization` và `content-type`. Nếu thay origin, cập nhật `CORS_ALLOWED_ORIGINS` trong `backend/.env`.

### Voucher rule matrix (V05–V12)

| Rule | Coverage |
|---|---|
| V05 – start date phải trước end date | `VoucherServiceTest.createRejectsStartDateNotBeforeEndDate`; database check trong `DatabaseConstraintsIntegrationTest` |
| V06 – voucher phải ACTIVE và nằm trong thời hạn | `VoucherServiceTest.validateRejectsInactiveVoucher`, `VoucherServiceTest.validateRejectsVoucherThatHasNotStarted`, `VoucherServiceTest.validateRejectsExpiredVoucher`; HTTP/integration: `VoucherCheckoutIntegrationTest.inactiveVoucherIsRejected`, `VoucherCheckoutIntegrationTest.voucherThatHasNotStartedYetIsRejected`, `VoucherCheckoutIntegrationTest.expiredVoucherIsRejected` |
| V07 – subtotal đạt minimum | `VoucherServiceTest.validateRejectsOrderBelowMinimumValue`, `VoucherServiceTest.validateAcceptsSubtotalEqualToMinimumOrderValue`; integration: `VoucherCheckoutIntegrationTest.orderBelowTheMinimumValueIsRejectedWithoutSideEffects` |
| V08 – usage limit | `VoucherServiceTest.validateRejectsWhenUsageLimitReached`, `VoucherServiceTest.validateAcceptsVoucherWithOneUseRemaining`, `VoucherServiceTest.validateAcceptsVoucherWithoutUsageLimit`, `VoucherServiceTest.updateAcceptsUsageLimitEqualToAlreadyUsedCount`; integration: `VoucherCheckoutIntegrationTest.voucherWithUsageLimitIsRejectedOnceTheLimitIsReached`, `DemoFlowApiIntegrationTest.concurrentCheckoutRequestsByOneCustomerUseTheSameVoucherOnlyOnce` |
| V09 – percentage discount and 100% boundary | `VoucherServiceTest.createAcceptsPercentageDiscountOfExactlyOneHundred`, `OrderPricingTest.percentageDiscountIsComputedFromSubtotal`, `OrderPricingTest.percentageDiscountIsRoundedToTwoDecimals`, `OrderPricingTest.percentageDiscountRoundsHalfUpAtHalfACent` |
| V10 – percentage max discount | `OrderPricingTest.percentageDiscountIsCappedByMaxDiscount`, `OrderPricingTest.percentageDiscountCanEqualMaxDiscount`, `OrderPricingTest.percentageDiscountIsNotCappedWhenMaxDiscountIsNull` |
| V11 – fixed discount không vượt subtotal | `OrderPricingTest.fixedDiscountReturnsExactValue`, `OrderPricingTest.fixedDiscountNeverExceedsSubtotal`, `OrderPricingTest.fixedDiscountCanEqualSubtotal` |
| V12 – ghi nhận/hoàn lượt dùng | `VoucherServiceTest.recordUsageSavesUsageWhenCounterIncreased`, `VoucherServiceTest.recordUsageRejectsWhenCounterCouldNotBeIncreased`, `VoucherServiceTest.releaseUsageDecreasesCounterAndDeletesUsageWhenUsageExists`, `VoucherServiceTest.releaseUsageDoesNothingWhenUsageDoesNotExist`; integration: `VoucherCheckoutIntegrationTest.percentageVoucherIsCappedByMaxDiscountAndUsageIsRecorded`, `VoucherCheckoutIntegrationTest.cancellingAnOrderReleasesVoucherUsageSoTheCustomerCanUseItAgain` |

`DemoSeedIntegrationTest` uses a separate demo test database, not the destructive database used by `IntegrationTestBase`. See [database.md](database.md) for setup and profile commands.

## 6. Khi test lỗi

| Triệu chứng | Nguyên nhân thường gặp |
|---|---|
| `Connection refused` hoặc `database "ooad_cosmetics_test" does not exist` | Chưa bật PostgreSQL hoặc chưa tạo database test (mục 1) |
| `password authentication failed` | Sai `DB_PASSWORD` trong `backend/.env`, hoặc đặt `TEST_DB_PASSWORD` |
| `Integration tests must run on a dedicated database` | URL database không chứa chữ `test`, kiểm tra `TEST_DB_URL` |
| `Schema-validation: missing column` | Entity không khớp với migration, so lại tên cột trong file `V*.sql` |
| `Migration checksum mismatch` | Một file migration đã chạy bị sửa nội dung; xóa database test rồi tạo lại |

Để làm sạch hoàn toàn database test:

```powershell
psql -U postgres -c "DROP DATABASE ooad_cosmetics_test;"
psql -U postgres -c "CREATE DATABASE ooad_cosmetics_test;"
```
