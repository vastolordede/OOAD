# OOAD — Báo cáo rà soát mã nguồn BE/FE (15/11/2026)

## Phạm vi và giới hạn

Nguồn: `OOAD(1).zip` được người dùng cung cấp. Kiểm tra tất cả tệp mã nguồn đang hoạt động trong `backend/src` và `frontend/src`, cùng cấu hình dự án, migration SQL, thử nghiệm Java, giao diện API và liên kết import. Tài liệu và sơ đồ đính kèm chỉ làm mẫu ký pháp; không được lấy làm bằng chứng code đúng/sai.

Gói ZIP gốc có 1.086 tệp (ngoài thư mục); trong đó có 167 Java ở backend main, 30 Java test, 9 migration SQL, 91 file trong FE src. Các file biên dịch sẵn dưới `backend/build`, `backend/bin`, các ZIP lịch sử/backup và PDF dưới `backend/work` **không phải mã nguồn vận hành**; không apply/chỉnh sửa chúng.

## Phát hiện và thay đổi trong một bản vá

| Mức độ | Khu vực | Phát hiện / hướng sửa |
|---|---|---|
| Cao | Thanh toán | API mô phỏng được bật quá rộng: mặc định tắt, chỉ bật ở demo/test; production ép tắt. |
| Cao | Thanh toán | FE thiếu luồng VNPay: bổ sung khám phá payment mode, tạo URL thanh toán, trang return và xác minh từ BE. |
| Cao | Thanh toán | IPN/return xác minh chữ ký, merchant/currency/trạng thái, từ chối callback khi thiếu secret, ký URL dùng UTF-8; cập nhật DB có khóa order; bổ sung hồi quy. |
| Cao | Thanh toán | Mô phỏng từng có thể xác nhận giao dịch đang dùng VNPay: chỉ cho phép mã MOCK khi đánh dấu thành công/thất bại. |
| Cao | Đơn hàng | `REFUNDED` từng chỉ là thay đổi DB không thực sự hoàn tiền: từ chối hủy đơn đã PAID online, chặn hủy khi phiên VNPay chưa kết thúc. |
| Cao | Phiên đăng nhập | JWT vẫn còn hiệu lực sau đổi/đặt lại mật khẩu: bổ sung fingerprint mật khẩu vào token để token cũ tự hết giá trị; từ chối TTL không hợp lệ. |
| Trung bình | Mật khẩu | Khóa bản ghi reset token khi sử dụng để tránh hai yêu cầu đồng thời; hủy token reset cũ sau đổi mật khẩu qua hồ sơ. |
| Trung bình | Đơn hàng | BE từ chối tạo đơn ONLINE nếu không bật demo và không có cấu hình VNPay, thay vì chỉ kiểm tra ở FE. |
| Trung bình | Giỏ hàng | Bảo vệ phép cộng quantity chống integer overflow; serialize thao tác tạo/ghi giỏ qua khóa owner/cart. |
| Trung bình | Địa chỉ / wishlist | Khóa owner khi tạo/đổi địa chỉ mặc định và tạo/thêm/xóa wishlist để giảm conflict request đồng thời. |
| Trung bình | Frontend auth | Xóa phiên local khi nhận HTTP 401, đồng bộ auth context và route sau token không hợp lệ. |
| Trung bình | API lỗi | Trả 400 với JSON và tham số không hợp lệ, tránh bị biến thành 500 chung. |
| Trung bình | Cloudinary | Không che lỗi nghiệp vụ 4xx thành 502; kiểm tra phản hồi upload có URL HTTPS và public_id. |
| Thấp | Tài liệu | Cập nhật hướng dẫn giao diện đơn hàng và cảnh báo hạn chế workflow, các cấu hình profile. |

## Kiểm tra đã thực sự chạy trong môi trường tạo patch

- Trình biên dịch Java 21 chỉ chạy **giai đoạn phân tích cú pháp (`JavacTask.parse`)**: 199 tệp `.java`, 0 lỗi cú pháp. **Không phải biên dịch/link Spring Boot đầy đủ**.
- TypeScript parser và kiểm tra đường dẫn import tương đối: 77 tệp `.ts/.tsx`, 0 lỗi.
- ZIP patch có `PATCH_MANIFEST.json` chứa SHA-256 của file nguồn tương ứng trong ZIP gốc và SHA-256 của file đích để phát hiện chênh lệch trước khi apply.
- Bộ dry-run và apply của `backend/work/apply_patch.py` đã được kiểm tra trên bản sao sạch; tất cả payload khớp SHA-256 sau apply.

## Chưa nghiệm thu / hạn chế còn tồn tại

- Không thể xác nhận `./gradlew clean test bootJar`, `npm ci`, `npm run build`, `npm run lint` ở môi trường này do thiếu dependency ngoại tuyến và Gradle distribution chưa tải được. Người sử dụng phải chạy các lệnh trên local/CI trước commit, push hay nghiệm thu.
- Chưa chạy PostgreSQL/Flyway integration test hoặc trình duyệt E2E, và chưa xác minh thực tế tài khoản merchant VNPay/IPN; cần cấu hình và giao dịch sandbox thật.
- Reset password **chưa có dịch vụ gửi email**; khi `RESET_EXPOSE_TOKEN=false`, backend không cung cấp đường dẫn đặt lại mật khẩu. Chỉ phù hợp demo thủ công khi bật chế độ thử nghiệm tại local; cần bổ sung mail sender trước production.
- Chưa có refund gateway thực. Đơn PAID online bị chặn hủy để không ghi nhận đã hoàn tiền sai; cần nghiệp vụ hoàn tiền thật ở giai đoạn riêng.
- Giới hạn tần suất login/forgot-password, kiểm thử tải nhiều client, đối soát callback chậm, và xử lý ảnh Cloudinary orphan chưa được kiểm chứng / hoàn thiện.
- Rà soát tĩnh không bảo đảm không còn lỗi runtime hay lỗi nghiệp vụ chưa được phát hiện.

## Kiểm thử bắt buộc trước khi tạo một commit

Tại nhánh fix được tạo từ `dev` mới nhất:

```powershell
cd backend
.\gradlew.bat clean test bootJar
cd ..\frontend
npm ci
npm run lint
npm run build
cd ..
git diff --check
git status --short
```

Kiểm thử thủ công: JWT cũ không truy cập sau đổi/reset mật khẩu; POST đặt đơn ONLINE tắt khi không có mode; hai thao tác checkout cùng giỏ không tạo đơn trùng; không xác nhận MOCK sau khi có txnRef VNPay; callback hợp lệ, sai chữ ký, sai merchant, sai số tiền, gửi lặp; không hủy đơn online đang PAID hoặc phiên VNPay chưa kết thúc; cancel đơn COD bình thường; giỏ/wishlist/địa chỉ có thể thao tác liên tiếp.

## Quy trình Git chính thức

Một ZIP duy nhất → SHA preflight → dry-run → apply → test → một commit → một push (chỉ sau khi người dùng cho phép) → người dùng tạo một PR vào dev → ghi mã PR vào Google Sheets → review, nghiệm thu rồi mới merge. Không đồng bộ SHA commit lên Google Sheets.
