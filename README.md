# UTE Soccer – Đặt sân bóng mini theo giờ (Spring Boot + Thymeleaf)

Ứng dụng web đặt sân bóng 6 sân theo giờ, hỗ trợ thanh toán (VNPay demo), đăng ký/đăng nhập, phân quyền ADMIN/USER, xem lịch đặt, chặn trùng lịch, và trang quản trị.

---

## 1) Tổng quan chức năng (Lược đồ và luồng chính)

- Homepage (khách, user):
  - Xem 6 sân, nút Đặt sân, banner sơ đồ vị trí 6 sân và căn tin
  - Với user (đã đăng nhập): hiển thị banner khuyến mãi, modal đặt sân ẩn các field họ tên/SĐT
- Modal đặt sân:
  - Chọn ngày (không quá khứ), chọn các khung 30 phút từ 17:00 đến 24:00
  - Bắt buộc chọn ≥ 2 khung liền kề
  - Hiển thị khung đã bận (PENDING chưa hết hạn và CONFIRMED)
- Đặt sân và thanh toán (VNPay – demo):
  - Tạo booking trạng thái PENDING, sinh `paymentCode`, hết hạn sau 3 phút
  - Trang thanh toán hiển thị đếm ngược 3 phút; nhấn “Đi tới VNPay” sẽ xác nhận thành công ngay (demo)
  - Khi thanh toán xong → CONFIRMED; nếu quá 3 phút → CANCELLED
  - Sau thanh toán hoặc hết hạn → tự động quay về trang chủ
- Quản trị (ADMIN):
  - Lọc theo ngày, chỉ hiển thị booking CONFIRMED
  - Xem chi tiết 1 booking (bao gồm họ tên, SĐT, tài khoản, thời gian thanh toán)
  - Xuất CSV danh sách CONFIRMED theo ngày
- Tài khoản:
  - Đăng ký (kèm họ tên, SĐT), kích hoạt (demo), đăng nhập
  - Sau đăng nhập: navbar hiển thị username, menu “Đặt chỗ của tôi”, “Đăng xuất”

Sơ đồ trình tự (rút gọn):
```
User → Trang chủ → Đặt sân → Modal → POST /book → Tạo PENDING (3 phút)
   → Trang thanh toán → (Demo) Xác nhận → Payment return → CONFIRMED → Trang chủ
```

---

## 2) Kiến trúc & Công nghệ

- Spring Boot 3.x, Java 22
- Thymeleaf 3.1 + `thymeleaf-extras-springsecurity6`
- Spring Web, Spring Data JPA (Hibernate), Spring Security
- MySQL 8.x (HikariCP)
- Scheduled tasks (cleanup PENDING hết hạn)
- Bootstrap 5 + Bootstrap Icons (UI nhất quán, responsive)
- JWT Authentication (REST APIs) – stateless
- WebSocket (STOMP over SockJS) – thông báo realtime
- Cloudinary – lưu trữ/quản lý hình ảnh sân/banners

Cấu trúc thư mục chính:
- `src/main/java/com/example/demo/`
  - `controller/` Home, Admin, Payment, Auth, MyBookings
  - `model/` Booking, AppUser
  - `repository/` BookingRepository, AppUserRepository
  - `service/` PaymentService, DbUserDetailsService, BookingCleanupJob
  - `config/` SecurityConfig, SchedulerConfig
  - `UtEsoccerApplication.java`
- `src/main/resources/templates/` – các trang Thymeleaf
- `src/main/resources/static/` – CSS, ảnh, JS tĩnh

---

## 3) Cài đặt & Chạy dự án

Yêu cầu:
- JDK 22 (hoặc 21 LTS), Maven 3.9+
- MySQL server (user: `root`, password: `dinh2103`)

B1. Tạo database (ví dụ `utesoccer`):
```sql
CREATE DATABASE IF NOT EXISTS utesoccer CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

B2. Cấu hình `src/main/resources/application.properties` (đã có sẵn mẫu):
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/utesoccer?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
spring.datasource.username=root
spring.datasource.password=dinh2103
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.thymeleaf.cache=false

# VNPay (demo)
vnpay.tmnCode=DEMO
vnpay.hashSecret=DEMO_SECRET
vnpay.payUrl=http://sandbox.vnpayment.vn/tryitnow/Home/CreateOrder
vnpay.returnUrl=http://localhost:8080/payment/return

# JWT (demo secret)
jwt.secret=mySecretKeyForJWTTokenGenerationThatIsAtLeast256BitsLong
jwt.expiration=86400000

# Cloudinary (điền thông tin thật nếu dùng upload)
cloudinary.cloud_name=your_cloud_name
cloudinary.api_key=your_api_key
cloudinary.api_secret=your_api_secret
```

B3. Chạy dự án:
```bash
mvn spring-boot:run
```
Truy cập: `http://localhost:8080`  
Trang thử nhanh các công nghệ: `http://localhost:8080/demo`

Tài khoản mẫu:
- User: `user` / `user123` (ROLE_USER)
- Admin: tạo qua DB hoặc thêm seeding tương tự (tuỳ chỉnh theo nhu cầu)

---

## 4) Database & Mô hình dữ liệu

Bảng `app_users` (entity `AppUser`):
- `id` (PK, bigint, auto)
- `username` (unique)
- `password_hash` (BCrypt)
- `role` (ROLE_USER/ROLE_ADMIN)
- `enabled` (boolean)
- `activation_token` (nullable)
- `full_name`, `phone`

Bảng `bookings` (entity `Booking`):
- `id` (PK, bigint, auto)
- `slot` (1..6)
- `date` (LocalDate)
- `start_time`, `end_time` (LocalTime)
- `name`, `phone`, `username`
- `status` (PENDING/CONFIRMED/CANCELLED)
- `payment_code` (unique trong phiên thanh toán)
- `expires_at` (LocalDateTime, giữ chỗ 3 phút)
- `amount` (long)
- `paid_at` (LocalDateTime)

Chỉ mục khuyến nghị:
- `bookings(date, slot, start_time, end_time, status)`
- `bookings(payment_code)`
- `app_users(username)`

Phát hiện trùng lịch: repository query kiểm tra giao thoa theo (slot, date, start_time < newEnd, end_time > newStart) và `status <> CANCELLED`.

---

## 5) Source code – Các lớp chính

### 5.1. Cấu hình bảo mật – `config/SecurityConfig.java`
- CSRF off (demo), session stateless cho APIs (JWT)
- Cho phép truy cập công khai: `/`, `/announcements`, `/contact`, `/css/**`, `/img/**`, `/api/**`, `/payment/**`, `/register`, `/activate`, `/auth/**`, `/demo`
- `/admin/**` yêu cầu `ROLE_ADMIN`
- JWT filter: `config/JwtRequestFilter.java`, tiện ích ký/giải mã `util/JwtUtil.java`

### 5.2. Người dùng – `model/AppUser.java`, `repository/AppUserRepository.java`, `service/DbUserDetailsService.java`
- Lưu người dùng trong DB, BCrypt password
- JWT REST endpoints: `controller/AuthController.java` (`/auth/login`, `/auth/register`, `/auth/validate`)

### 5.3. Đặt sân – `model/Booking.java`, `repository/BookingRepository.java`
- Trạng thái: `PENDING` (giữ 3 phút) → `CONFIRMED` (khi thanh toán) → `CANCELLED` (hết hạn hoặc huỷ)
- Truy vấn phục vụ: tìm trùng, tìm theo ngày/slot, tìm theo `paymentCode`, tìm theo `username`, lọc CONFIRMED theo ngày

### 5.4. Bộ điều khiển – `controller/`
- `HomeController`:
  - GET `/` (homepage), `/announcements`, `/contact`
  - POST `/book`: tạo booking PENDING, auto điền name/phone từ user nếu đã đăng nhập
  - GET `/api/availability`: trả về các index khung bận cho modal
- `PaymentController`:
  - GET `/payment/return`: callback VNPay (demo: xác nhận ngay)
  - GET `/api/payment/status?paymentCode=`: API để trang payment poll trạng thái
- `AdminController`:
  - GET `/admin?date=`: bảng CONFIRMED theo ngày, loại trùng theo (Sân, Ngày, Bắt đầu, Kết thúc)
  - GET `/admin/export?date=`: xuất CSV
  - GET `/admin/booking/{id}`: chi tiết 1 booking
- `AuthController`:
  - REST `/auth/login`, `/auth/register`, `/auth/validate`
- `MyBookingsController`:
  - GET `/me/bookings`: danh sách đặt của user hiện tại
- `ImageController`:
  - `/api/images/upload` (ADMIN), `/api/images/field/{slot}`, `/api/images/type/{imageType}`, `/api/images/all`
- `WebSocketController`:
  - Nhận/gửi thông báo demo qua STOMP: `/app/admin/notifications` → `/topic/admin`
- `DemoController`:
  - GET `/demo`: trang thử nhanh JWT/WebSocket/Upload

### 5.5. Payment – `service/PaymentService.java`
- Tạo URL thanh toán VNPay (demo), flow “tryitnow” xác nhận ngay
- Sinh `paymentCode` và ánh xạ vào booking

### 5.6. Cleanup job – `service/BookingCleanupJob.java`, `config/SchedulerConfig.java`
- Chạy định kỳ (mỗi phút) tìm PENDING hết hạn và đổi sang CANCELLED

### 5.7. Realtime & Media
- `config/WebSocketConfig.java` – cấu hình STOMP/SockJS (`/ws`, broker `/topic`, `/queue`)
- `service/NotificationService.java` – tiện ích gửi thông báo realtime
- `config/CloudinaryConfig.java`, `service/CloudinaryService.java`, `model/FieldImage.java`, `repository/FieldImageRepository.java`

---

## 6) Giao diện (Thymeleaf Templates)

- `templates/index.html` – Trang chủ: 6 sân, modal đặt sân, banner sơ đồ `img/field-map.svg`
- `templates/partials/navbar.html` – Navbar theo role (ADMIN/USER/Khách)
- `templates/payment.html` – Trang thanh toán có đếm ngược 3 phút, tự redirect
- `templates/admin.html` – Bảng CONFIRMED theo ngày, loại trùng, link chi tiết
- `templates/admin_booking_detail.html` – Chi tiết booking, định dạng thời gian thanh toán
- `templates/login.html`, `templates/register.html`, `templates/my_bookings.html`, `templates/announcements.html`, `templates/contact.html`
- `templates/demo.html` – Trang demo JWT/WebSocket/Upload (dùng để thử nhanh)

Static:
- `static/css/styles.css` – tông trung tính, độ tương phản cao hơn; card hover, modal/tables tinh gọn
- `static/img/field-map.svg` – sơ đồ 6 sân, căn tin ở góc dưới cùng gần Sân 4

---

## 7) API chính

- `GET /api/availability?slot={1..6}&date=YYYY-MM-DD`
  - Trả về `{ ok: true, occupied: [indices] }` – các index khung 30’ đã bận
- `GET /api/payment/status?paymentCode=...`
  - Trả về `{ status: PENDING|CONFIRMED|CANCELLED }`

- Auth (JWT)
  - `POST /auth/login` → `{ token, username, role }`
  - `POST /auth/register` → `{ token, username, role }`
  - `GET /auth/validate` (header `Authorization: Bearer <token>`)

- Cloudinary Images
  - `POST /api/images/upload` (ADMIN, multipart form-data)
  - `GET /api/images/field/{slot}` | `/type/{imageType}` | `/all`

- WebSocket
  - Endpoint: `/ws` (SockJS)
  - Broker: subscribe `/topic/admin`, `/topic/availability`; user queue `/queue/*`

---

## 8) Quy tắc & Ràng buộc

- Ngày đặt không được trong quá khứ
- Phải chọn ≥ 2 khung 30’ liền kề
- Không hiển thị “đã đặt” ở trang chủ (chỉ thể hiện bận trong modal đặt sân)
- Chống đặt chồng chéo theo (Sân, Ngày, Start-End) với PENDING chưa hết hạn và CONFIRMED

---

## 9) Hướng dẫn phát triển từng chức năng

### 9.1. Tạo Booking (POST /book)
1) Validate: ngày hợp lệ, số khung chọn ≥ 2, liên tục
2) Tính `startTime`, `endTime` từ các chip
3) Kiểm tra trùng lịch qua `BookingRepository.findOverlapping(...)`
4) Tạo `Booking` PENDING, set `expiresAt = now + 3 phút`, `paymentCode` unique, `amount`
5) Redirect tới `/payment?code=...`

Mã liên quan: `HomeController`, `Booking`, `BookingRepository`, `PaymentService`, `payment.html`.

### 9.2. Kiểm tra & hiển thị bận trong modal
1) Client gọi `GET /api/availability`
2) Server hợp nhất các PENDING chưa hết hạn + CONFIRMED trong ngày/slot
3) Trả về danh sách index khung bận; client disable các chip tương ứng

Mã liên quan: `HomeController` (`/api/availability`), `index.html` (JS đánh dấu chip bận).

### 9.3. Thanh toán VNPay (demo)
1) Khi vào trang payment, hiển thị `paymentCode` + đếm ngược 3 phút
2) Nút “Đi tới VNPay” → chuyển hướng tới URL demo (hoặc giả lập) và ngay lập tức callback như thanh toán thành công
3) `PaymentController` nhận return, set booking `CONFIRMED`, `paidAt = now`
4) Nếu quá 3 phút chưa thanh toán, cleanup job sẽ CANCELLED; trang payment poll `/api/payment/status` và redirect về `/`

Mã liên quan: `PaymentService`, `PaymentController`, `BookingCleanupJob`, `payment.html`.

### 9.4. Quản trị admin
1) GET `/admin?date=YYYY-MM-DD` → lấy toàn bộ CONFIRMED theo ngày
2) Loại bỏ trùng theo (slot, date, startTime, endTime)
3) Ẩn cột id; hiển thị nút “Xem” → `/admin/booking/{id}`
4) Xuất CSV `/admin/export?date=...`

Mã liên quan: `AdminController`, `admin.html`, `admin_booking_detail.html`.

### 9.5. Đăng ký/Đăng nhập/Phân quyền
1) Đăng ký `/register`: lưu `AppUser` (BCrypt), sinh `activationToken` (demo)
2) Kích hoạt `/activate?token=` → `enabled=true`
3) Đăng nhập `/login`
4) Sau đăng nhập: ADMIN → `/admin`, USER → `/`
5) Navbar: `sec:authorize` hiển thị menu theo role

Mã liên quan: `SecurityConfig`, `AuthController`, `DbUserDetailsService`, `partials/navbar.html`.

---

## 10) Build & đóng gói

```bash
mvn clean package
java -jar target/UTEsoccer-*.jar
```

Cấu hình runtime thông qua biến môi trường hoặc `application.properties`.

---

## 11) Troubleshooting

- Lỗi Thymeleaf format ngày/thời gian:
  - Với `LocalDate/LocalTime` dùng `#temporals.format(...)`
  - Với `java.util.Date` dùng `#dates.format(...)`
- 404 chi tiết booking: kiểm tra route `/admin/booking/{id}` và `th:href` trong `admin.html`
- Ảnh banner không hiển thị: đảm bảo đã cho phép `/img/**` trong `SecurityConfig`
- JWT không hoạt động: kiểm tra `Authorization: Bearer <token>` và `jwt.secret`
- WebSocket không kết nối: kiểm tra endpoint `/ws` và path subscribe `/topic/...`


---

## 12) Ghi chú

- Code được tối ưu cho demo và kiểm thử nhanh. Nếu triển khai thật, cần bật lại CSRF, thêm kiểm thử, logging, ràng buộc mạnh hơn và tích hợp VNPay/MoMo chính thức.
