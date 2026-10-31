# Kế hoạch công việc của Trân — Frontend Order & Business

> Căn cứ: `Sheet Checklist Việc OOAD(1).ods` và prompt frontend React + TypeScript + Vite được cung cấp.  
> Phạm vi chính của Trân: **Frontend Order + Business** — Checkout, Voucher, Order, Admin Order, Review, Dashboard; cùng Admin Layout/Sidebar và Toast/Notification.  
> Lưu ý: Checklist ghi nhiều mốc ngày không đồng nhất với tuần mục tiêu. Kế hoạch bên dưới sắp xếp theo **phụ thuộc kỹ thuật**, không coi ngày cũ là deadline hiện tại.

## 1. Phân tích công việc của Trân

### Công việc được giao trong checklist

| ID | Công việc | Mốc trong checklist | Phân loại |
|---|---|---:|---|
| F16 | Tạo Admin Layout + Sidebar dùng chung | Tuần 1 | CORE |
| F18 | Tạo Toast/Notification component | Tuần 2 | CORE |
| D10 | Sequence Diagram Add To Cart | Tuần 3 | Đã hoàn thành |
| D15 | Wireframe Cart/Checkout/Order/Dashboard | Tuần 1 | CORE |
| D18 | Tổng hợp tài liệu OOAD bản final | Tuần 7 | CORE |
| O33–O36 | Checkout Screen; chọn Shipping Address; hiển thị Subtotal/Discount/Shipping/Total; Confirm/Create Order UI | Tuần 3 | CORE |
| O37–O39 | My Orders; Order Detail; Cancel Order UI | Tuần 4 | CORE |
| O40–O42 | Admin Order List/Filter; Admin Order Detail; Update Order Status UI | Tuần 4 | CORE |
| V13–V14 | Admin Voucher List; Create/Edit Voucher Form | Tuần 5 | CORE |
| V15 | Apply/Remove Voucher tại Checkout | Tuần 3 | CORE |
| R11–R12 | Review List trên Product Detail; Review Form sau khi Order DELIVERED | Tuần 4 | CORE |
| R13 | Admin Review Moderation Screen | Tuần 5 | CORE |
| DB07–DB09 | Dashboard Summary Cards; Order Status; Low Stock Widget | Tuần 5 | CORE |
| O46, V17, R15 | Test tích hợp Checkout/Order, Voucher, Review | Tuần 6 | CORE |
| T05–T09 | Test luồng Product → Cart → Checkout → Order; Admin cập nhật trạng thái; Cancel/Restore Stock; Voucher; DELIVERED → Review | Tuần 6 | CORE |
| T13 | Regression Frontend Cart/Order/Admin | Tuần 6 | CORE |
| DEP11–DEP12 | Chuẩn bị Demo Script; tài khoản và dữ liệu tình huống demo | Tuần 7 | CORE |

### Việc SHOULD / BONUS — chỉ làm sau khi CORE chạy ổn

- **R17:** UI sửa/xóa Review (SHOULD).
- **DB12–DB13:** Revenue Chart và Top Selling Product Chart (SHOULD).
- **O49:** Online Payment Screen (BONUS; checklist ghi BE Online Payment Gateway đã hoàn thành, nhưng cần xác nhận luồng tích hợp thực tế).
- **W05:** Wishlist Screen (BONUS).
- Làm đẹp UI, hiệu ứng hoặc thêm thư viện chỉ sau khi đã hỏi và được đồng ý; prompt hiện chỉ cho phép `react`, `react-dom`, `react-router-dom`, `axios`, `typescript`, `vite`.

### Phụ thuộc quan trọng

1. Không nên viết API frontend theo phỏng đoán. Cần lấy Controller, DTO request/response và cấu hình bảo mật từ backend của Thảo/Đặng trước khi tích hợp.
2. Các màn hình Checkout phụ thuộc vào Cart, Address, Voucher và Create Order API.
3. My Orders/Order Detail/Cancel phụ thuộc các Order API phía Customer.
4. Admin Order phụ thuộc Admin list/filter, detail và update-status API; cần xác nhận quyền ADMIN và các trạng thái hợp lệ.
5. Review Form phụ thuộc điều kiện backend: chỉ Order `DELIVERED` mới được review; rating từ 1–5; mỗi OrderItem chỉ review một lần (theo checklist).
6. Dashboard phải khớp các API tổng quan, trạng thái đơn, low-stock; biểu đồ doanh thu/top-selling là SHOULD.
7. Thông tin base URL, prefix API, cấu trúc response, phân trang và endpoint xác thực **chưa được xác định đầy đủ trong các tệp**. Không tự điền giá trị đoán.

---

## 2. Prompt đã điền để sử dụng với ChatGPT/Cursor

Sao chép prompt bên dưới. Trước khi yêu cầu viết một chức năng cụ thể, hãy gửi thêm Controller/DTO tương ứng từ backend.

```text
# VAI TRÒ
Bạn là senior frontend developer (React + TypeScript + Vite), đồng thời hướng dẫn một sinh viên mới học frontend. Hãy xây dựng/mở rộng frontend cho dự án theo đúng kiến trúc bên dưới. Giải thích ngắn gọn, dễ hiểu; làm theo từng bước nhỏ và không tự ý thay đổi kiến trúc.

# BỐI CẢNH DỰ ÁN
- Tên dự án: Chưa được xác nhận từ checklist — hãy dùng tên repo/package thực tế khi tôi cung cấp.
- Loại ứng dụng: Web thương mại điện tử/quản lý bán hàng có danh mục sản phẩm, giỏ hàng, checkout, voucher, đơn hàng, đánh giá và trang quản trị. Nếu tên đề tài chính thức khác, hỏi tôi trước khi đổi mô tả.
- Vai trò của tôi: Trân — phụ trách Frontend Order + Business.
- Công nghệ frontend: React + TypeScript + Vite.
- Backend: Spring Boot.
- Database phía backend: PostgreSQL.
- Backend base URL: CHƯA XÁC NHẬN; không tự đoán.
- Prefix API: CHƯA XÁC NHẬN; kiểm tra cấu hình backend hoặc hỏi tôi.
- Xác thực: Checklist có chức năng đăng nhập/JWT ở phần backend, nhưng cần xem AuthController và Security config để xác nhận format token, endpoint và quyền.
- Kiểu response backend: Backend có hạng mục Common API Response; phải đọc lớp response thực tế để xác định response bọc ngoài hay trả DTO trực tiếp.
- Phân trang: Backend có pagination response dùng chung; phải đọc DTO/kiểu trả về thực tế trước khi khai báo type.
- Không tự bịa endpoint, field DTO, status enum, quyền truy cập hoặc dữ liệu mẫu nghiệp vụ.

# PHẠM VI CÔNG VIỆC CỦA TRÂN
CORE, thực hiện theo thứ tự phụ thuộc:
1. D15 — Wireframe Cart/Checkout/Order/Dashboard (CORE; đối chiếu wireframe với giao diện cần làm).
2. F16 — Admin Layout + Sidebar dùng chung.
3. F18 — Toast/Notification component.
4. O33–O36 — Checkout Screen; chọn Shipping Address; hiển thị Subtotal/Discount/Shipping/Total; Confirm/Create Order UI.
5. V15 — Apply/Remove Voucher tại Checkout.
6. O37–O39 — My Orders; Order Detail; Cancel Order UI.
7. O40–O42 — Admin Order List/Filter; Admin Order Detail; Update Order Status UI.
8. V13–V14 — Admin Voucher List; Create/Edit Voucher Form.
9. R11–R12 — Review List trên Product Detail; Review Form sau khi Order DELIVERED.
10. R13 — Admin Review Moderation Screen.
11. DB07–DB09 — Dashboard Summary Cards; Order Status Dashboard UI; Low Stock Widget.
12. O46, V17, R15 và T05–T09 — kiểm thử tích hợp các luồng Checkout/Order/Voucher/Review.
13. T13 — Regression Frontend Cart/Order/Admin.
14. D18 — Tổng hợp tài liệu OOAD bản final.
15. DEP11–DEP12 — Chuẩn bị Demo Script, tài khoản và dữ liệu tình huống demo.

Đã hoàn thành theo checklist: D10 — Sequence Diagram Add To Cart.
Sau khi toàn bộ CORE hoạt động ổn mới cân nhắc:
- R17 — Edit/Delete Review UI (SHOULD).
- DB12–DB13 — Revenue Chart và Top Selling Product Chart (SHOULD).
- O49 — Online Payment Screen (BONUS; cần kiểm tra API/luồng thanh toán thực tế).
- W05 — Wishlist Screen (BONUS).

# THƯ VIỆN ĐƯỢC PHÉP
Chỉ dùng: react, react-dom, react-router-dom, axios, typescript, vite.
Không thêm thư viện UI, chart, form, icon hoặc state management khác nếu chưa hỏi và được tôi đồng ý.

# CẤU TRÚC THƯ MỤC BẮT BUỘC
Giữ nguyên cấu trúc trong prompt gốc:
frontend/src/api, types, contexts, hooks, routes, pages, components/common, theme, utils.
Chỉ `src/api/axios.ts` và các file `src/api/*.api.ts` được thực hiện HTTP request; page/component không gọi axios/fetch trực tiếp.

# NGUYÊN TẮC TÍCH HỢP
- Phân tầng một chiều: pages/components → hooks/contexts → api/*.api.ts → api/axios.ts → backend.
- Chỉ axios.ts biết baseURL và token; đọc baseURL từ import.meta.env.VITE_API_URL.
- Mỗi hàm API tương ứng đúng một endpoint thật, tên kết thúc bằng Api, nhận payload có type và trả về dữ liệu có type.
- Khai báo interface trong types/ theo đúng từng ký tự DTO backend; không dùng any; dùng import type.
- Theo prompt kiến trúc gốc về AuthContext/useAuth/ProtectedRoute/localStorage key accessToken, nhưng trước khi triển khai phải đối chiếu AuthController và response thực tế.
- Mỗi page quản lý loading/error/data phù hợp; có trạng thái loading, lỗi, danh sách rỗng và thao tác thành công.
- Dùng getErrorMessage(err) theo cấu trúc response thật của backend.
- Giao diện đơn giản, rõ ràng, responsive; ưu tiên đúng nghiệp vụ và dữ liệu trước khi trang trí.
- Không thay đổi endpoint/status/business rule ở frontend để che lỗi backend.

# QUY TRÌNH BẮT BUỘC KHI LÀM MỖI CHỨC NĂNG
1. Kiểm tra file backend tôi cung cấp: Controller, DTO request/response, service/business rule liên quan và Security config.
2. Liệt kê endpoint bằng bảng: method, path, params, request body, response, yêu cầu token/role. Nếu thiếu dữ liệu, hỏi tôi; không viết API giả.
3. Xác định dependency của chức năng và những API phải có trước.
4. Tạo/cập nhật types trước, khớp DTO thật.
5. Tạo/cập nhật api/<nhóm>.api.ts, mỗi endpoint một hàm.
6. Tạo UI/component/page; kết nối API qua tầng đúng quy định.
7. Thêm route và bảo vệ route theo quyền đã xác nhận.
8. Kiểm tra loading/error/empty/success, validate dữ liệu cơ bản và quyền truy cập.
9. Nêu cách chạy thử từng bước và kết quả mong đợi; không khẳng định đã test nếu chưa chạy thực tế.
10. Cuối mỗi lượt liệt kê file đã tạo/sửa theo đường dẫn, endpoint đang dùng, phần còn thiếu cần backend xác nhận và checklist test.

# ĐIỀU KIỆN NGHIỆP VỤ CẦN ĐỐI CHIẾU VỚI BACKEND
- Checkout: chọn địa chỉ giao hàng; hiển thị subtotal, discount, shipping, total; xác nhận tạo đơn.
- Voucher: áp dụng/gỡ voucher tại checkout; quy tắc hiệu lực, ngày bắt đầu/kết thúc, giá trị đơn tối thiểu, giới hạn lượt dùng và mức giảm phải lấy từ backend.
- Customer Order: danh sách, chi tiết và hủy đơn. Checklist backend nêu không cho hủy SHIPPING/DELIVERED; hủy đơn có thể khôi phục stock. Xác nhận trạng thái thực tế từ enum/service.
- Admin Order: danh sách/tìm kiếm/lọc, chi tiết, cập nhật trạng thái. Checklist nêu các chuyển trạng thái PENDING → CONFIRMED → SHIPPING → DELIVERED và một số luồng CANCELLED; không tự suy diễn thêm.
- Review: checklist nêu chỉ Order DELIVERED được review, rating 1–5, mỗi OrderItem review một lần; phải hiển thị lỗi backend rõ ràng.
- Dashboard: tổng customers/products/orders, revenue chỉ tính DELIVERED Orders, số lượng theo status, low-stock products. Revenue chart/top-selling chart là SHOULD.
- Các quy tắc trên là tóm tắt checklist; luôn kiểm tra implementation backend hiện tại trước khi code.

# NHIỆM VỤ ĐẦU TIÊN
Đừng tạo tất cả màn hình cùng lúc. Bắt đầu bằng:
A. Kiểm tra cấu trúc repo frontend hiện có và các file API/route/type đã có.
B. Đề xuất thứ tự triển khai dựa trên dependency.
C. Hỏi tôi gửi Controller + DTO + Security config cho chức năng đầu tiên chưa có đủ thông tin.
D. Chỉ sau khi có hợp đồng API thực tế mới viết code tích hợp.
```

---

## 3. Danh sách việc Trân cần làm theo thứ tự

### Giai đoạn 0 — Kiểm tra và chuẩn bị

- [ ] Xác nhận tên repo/tên đề tài chính thức và đường dẫn thư mục frontend.
- [ ] Mở checklist, đối chiếu trạng thái thực tế trên GitHub; cập nhật trạng thái cũ nếu nhóm đã làm thêm.
- [ ] Lấy cấu trúc frontend hiện tại và kiểm tra React/Vite/TypeScript, Router, Axios đã được thiết lập chưa.
- [ ] Xin backend các file Controller, DTO request/response, enum trạng thái, Security config và Common API Response.
- [ ] Xác nhận `.env`, base URL, prefix API, format response, pagination và quy tắc token. Không commit secret vào Git.
- [ ] Kiểm tra quyền Customer/Admin và luồng nghiệp vụ trước khi làm route.

### Giai đoạn 1 — Foundation và thiết kế

- [ ] **D15:** Hoàn thiện/đối chiếu wireframe Cart, Checkout, Order, Dashboard.
- [ ] **F16:** Làm Admin Layout + Sidebar dùng chung; xác định các route admin đã được backend xác nhận.
- [ ] **F18:** Làm Toast/Notification dùng lại; có trạng thái thành công/lỗi.
- [ ] Kiểm tra layout trên desktop và màn hình nhỏ; chưa thêm thư viện UI nếu chưa được duyệt.

### Giai đoạn 2 — Checkout và Voucher

- [ ] **O33:** Dựng Checkout Screen.
- [ ] **O34:** Làm UI chọn Shipping Address.
- [ ] **O35:** Hiển thị subtotal, discount, shipping, total theo response/quy tắc đã xác nhận.
- [ ] **V15:** Tích hợp Apply/Remove Voucher tại Checkout theo API thật.
- [ ] **O36:** Làm nút xác nhận/tạo đơn, trạng thái đang gửi, chặn gửi lặp và hiển thị lỗi backend.
- [ ] Kiểm thử giỏ hàng rỗng, địa chỉ thiếu, voucher không hợp lệ, hết stock và tạo đơn thất bại (theo lỗi backend có thể trả).
- [ ] Không tự tính lại giá trị cuối cùng để thay thế kết quả backend; xác định rõ giá nào do backend trả và giá nào chỉ là preview.

### Giai đoạn 3 — Customer Order

- [ ] **O37:** My Orders — danh sách đơn hàng, loading/error/empty state và phân trang nếu API hỗ trợ.
- [ ] **O38:** Order Detail — chi tiết đơn, sản phẩm, số lượng, giá và trạng thái theo DTO thật.
- [ ] **O39:** Cancel Order UI — chỉ hiển thị/cho thao tác theo rule backend; xác nhận trước khi hủy và tải lại dữ liệu sau khi thành công.
- [ ] Kiểm thử các trạng thái đơn và trường hợp hủy không được phép.

### Giai đoạn 4 — Admin Order

- [ ] **O40:** Admin Order List/Filter — danh sách, tìm kiếm/lọc theo các tham số API thực tế.
- [ ] **O41:** Admin Order Detail.
- [ ] **O42:** Update Order Status UI — chỉ hiển thị các chuyển trạng thái được backend cho phép.
- [ ] Kiểm tra quyền Admin, lỗi 401/403, cập nhật thất bại và trạng thái sau cập nhật.

### Giai đoạn 5 — Admin Voucher

- [ ] **V13:** Admin Voucher List.
- [ ] **V14:** Form Create/Edit Voucher.
- [ ] Tích hợp CRUD theo endpoint thật; validate phía UI để hỗ trợ người dùng nhưng vẫn hiển thị lỗi backend.
- [ ] Kiểm tra ngày hiệu lực, mã voucher, giá trị giảm, mức tối thiểu và giới hạn lượt dùng theo DTO/rule thực tế.

### Giai đoạn 6 — Review

- [ ] **R11:** Hiển thị Review List trên Product Detail; phối hợp với người phụ trách Product Detail để xác định điểm tích hợp.
- [ ] **R12:** Review Form sau khi đơn ở trạng thái DELIVERED; kiểm tra rating 1–5 và trạng thái đã review theo API thật.
- [ ] **R13:** Admin Review Moderation Screen — ẩn/hiện review theo API thật.
- [ ] **R17 (SHOULD):** Edit/Delete Review UI chỉ làm sau khi CORE ổn và xác nhận endpoint.

### Giai đoạn 7 — Dashboard

- [ ] **DB07:** Summary Cards cho tổng khách hàng, sản phẩm, đơn hàng và doanh thu theo API.
- [ ] **DB08:** Biểu diễn số đơn theo trạng thái.
- [ ] **DB09:** Low Stock Widget.
- [ ] **DB12–DB13 (SHOULD):** Revenue Chart và Top Selling Product Chart; chỉ triển khai khi endpoint và dữ liệu biểu đồ đã được xác nhận. Nếu cần thư viện chart, phải xin phép trước.

### Giai đoạn 8 — Kiểm thử tích hợp và sửa lỗi

- [ ] **O46:** Test Checkout/Order Integration.
- [ ] **V17:** Test Voucher Checkout Integration.
- [ ] **R15:** Test Review Integration.
- [ ] **T05:** Test Product → Cart → Checkout → Order.
- [ ] **T06:** Test Admin cập nhật Order → Customer thấy trạng thái mới.
- [ ] **T07:** Test Cancel Order → backend restore stock.
- [ ] **T08:** Test Voucher trong Checkout.
- [ ] **T09:** Test đơn DELIVERED → Review.
- [ ] **T13:** Regression Frontend Cart/Order/Admin.
- [ ] Với mỗi lỗi, ghi bước tái hiện, kết quả mong đợi/thực tế, endpoint liên quan và ảnh chụp nếu cần.
- [ ] Không đánh dấu hoàn thành nếu mới chỉ viết code mà chưa chạy thử.

### Giai đoạn 9 — Tài liệu và demo

- [ ] **D18:** Tổng hợp tài liệu OOAD bản final; kiểm tra diagram/wireframe khớp với ứng dụng cuối.
- [ ] **DEP11:** Viết demo script theo luồng nghiệp vụ từ đăng nhập → sản phẩm → giỏ hàng → checkout → đơn hàng → admin cập nhật → review.
- [ ] **DEP12:** Chuẩn bị tài khoản Customer/Admin và dữ liệu demo; không đưa mật khẩu thật vào repository công khai.
- [ ] Chạy lại demo từ đầu trên môi trường sạch, ghi rõ bước cần thao tác và kết quả mong đợi.
- [ ] Phối hợp nhóm để kiểm tra build frontend và bàn giao lỗi còn tồn tại.

### Giai đoạn 10 — Việc ngoài CORE

Chỉ làm khi CORE đã được tích hợp và kiểm thử:
- [ ] **DB12–DB13:** biểu đồ doanh thu/top-selling (SHOULD).
- [ ] **R17:** sửa/xóa review (SHOULD).
- [ ] **O49:** màn hình thanh toán online (BONUS).
- [ ] **W05:** Wishlist Screen (BONUS).

---

## 4. Quy tắc hoàn thành cho từng màn hình

Một chức năng chỉ được xem là xong khi:

- [ ] Đã đối chiếu endpoint và DTO với backend.
- [ ] TypeScript không có lỗi; không dùng `any`.
- [ ] Không gọi HTTP ngoài tầng `api/`.
- [ ] Có loading, error và empty state phù hợp.
- [ ] Có xử lý thao tác thành công và cập nhật/tải lại dữ liệu.
- [ ] Có kiểm tra quyền và trạng thái nghiệp vụ phù hợp.
- [ ] Đã chạy thử các ca thành công và thất bại chính.
- [ ] Đã ghi file tạo/sửa, endpoint sử dụng và phần backend còn cần xác nhận.
- [ ] Đã commit theo quy ước của nhóm nếu repo đã có quy ước.

## 5. Thông tin cần xin nhóm trước khi tích hợp

1. AuthController, Auth DTO và Security config.
2. Cart API/DTO và cách lấy Cart hiện tại.
3. Address API/DTO cho địa chỉ giao hàng.
4. Checkout preview/Create Order API và DTO.
5. Customer Order list/detail/cancel API.
6. Admin Order list/filter/detail/update-status API và enum trạng thái.
7. Voucher CRUD/validate/apply/remove API và DTO.
8. Review list/create/hide/unhide API và DTO.
9. Dashboard summary/status/low-stock API và DTO.
10. Common API Response, pagination response, API prefix, base URL và hướng dẫn chạy backend.

**Nguyên tắc cuối cùng:** Checklist cho biết phạm vi công việc, nhưng không thay thế API contract. Hãy triển khai theo thứ tự trên, mỗi lần một chức năng nhỏ, chỉ tích hợp khi có hợp đồng API thật.
