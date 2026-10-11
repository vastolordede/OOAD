# Hướng dẫn chạy test (phần Cart, Order, Voucher, Review, Dashboard)

Có hai loại test:

| Loại | Lệnh | Cần database? | Dùng để |
|---|---|---|---|
| Unit test | `gradlew.bat test` | Không | Kiểm tra logic nhanh, dữ liệu giả (mock) |
| Integration test | `gradlew.bat integrationTest` | Có, PostgreSQL thật | Kiểm tra transaction, rollback, tranh chấp đồng thời, ràng buộc dữ liệu, các truy vấn thật |

`gradlew test` **không** chạy integration test, nên máy nào chưa có database test vẫn chạy được unit test.

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
gradlew.bat test               # unit test
gradlew.bat integrationTest    # integration test
```

Chạy một lớp test: `gradlew.bat integrationTest --tests "*CheckoutOrderIntegrationTest"`.

## 4. Các bài test và việc trong sheet

| Việc | Test |
|---|---|
| C17 Test Cart API và Stock Validation | `CartIntegrationTest`, `CartServiceTest`, `StockServiceTest` |
| O43 Order Transaction Rollback | `CheckoutOrderIntegrationTest` (lỗi khi lưu thanh toán, lỗi khi ghi voucher, hết hàng trước lúc đặt) |
| O44 Cancel Order và Restore Stock | `CheckoutOrderIntegrationTest` (khách hủy, admin hủy, hoàn kho, trả voucher) |
| O45 Invalid Order Status Transition | `CheckoutOrderIntegrationTest`, `OrderStatusTest`, `OrderServiceTest` |
| O47 Hai request mua vượt Stock | `CheckoutOrderIntegrationTest` (8 người tranh 3 sản phẩm, 2 người tranh 1 lượt voucher, bấm đặt hai lần) |
| V16 Voucher Business Rules | `VoucherCheckoutIntegrationTest`, `VoucherServiceTest`, `OrderPricingTest` |
| R14 Review Eligibility và Rating | `ReviewIntegrationTest`, `ReviewServiceTest` |
| DB14 Dashboard Query | `DashboardIntegrationTest`, `DashboardServiceTest` |
| T15 Database constraints và invalid data | `DatabaseConstraintsIntegrationTest` |
| T11 Regression Backend | Toàn bộ test trên, cộng `DemoFlowApiIntegrationTest` |

`DemoFlowApiIntegrationTest` đi qua HTTP thật: đăng ký, đăng nhập, địa chỉ, giỏ hàng, xem trước đơn, đặt đơn, Admin đưa đơn qua từng bước, review, dashboard, kèm kiểm tra 401 và 403.

## 5. Khi test lỗi

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
