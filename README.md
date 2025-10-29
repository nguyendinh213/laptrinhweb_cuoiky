# UTE Soccer - Hệ thống đặt sân bóng mini

## 📋 Tổng quan dự án

UTE Soccer là một hệ thống web đặt sân bóng mini theo giờ với 6 sân, được phát triển bằng Spring Boot và Thymeleaf. Hệ thống hỗ trợ đặt chỗ theo khung giờ 30 phút từ 17:00 đến 24:00, tích hợp thanh toán VNPay, quản lý người dùng và phân quyền admin/user.

## 🎯 Tính năng chính

### 👥 Đối với Guest (Khách)
- Xem thông tin 6 sân bóng mini
- Đặt chỗ theo giờ với thông tin cá nhân
- Xem khung giờ đã đặt (màu đỏ, không chọn được)
- Thanh toán qua VNPay (demo mode)
- Xem thông báo và liên hệ

### 🔐 Đối với User (Người dùng đã đăng ký)
- Tất cả tính năng của Guest
- Đăng nhập/đăng ký tài khoản
- Thông tin cá nhân được tự động điền khi đặt chỗ
- Xem lịch sử đặt chỗ của mình
- Quản lý profile cá nhân

### 👨‍💼 Đối với Admin
- Xem danh sách tất cả đặt chỗ theo ngày
- Xuất file CSV danh sách đặt chỗ
- Xem chi tiết từng đặt chỗ
- Quản lý người dùng và hệ thống

## 🛠️ Công nghệ sử dụng

### Backend Framework
- **Spring Boot 3.5.7** - Framework chính cho ứng dụng Java
- **Spring MVC** - Xử lý HTTP requests và responses
- **Spring Data JPA** - ORM và quản lý database
- **Spring Security** - Authentication và authorization
- **Spring WebSocket** - Real-time notifications

### Frontend Technologies
- **Thymeleaf** - Server-side template engine
- **Bootstrap 5.3.0** - CSS framework cho responsive design
- **Bootstrap Icons** - Icon library
- **Vanilla JavaScript** - Client-side logic
- **CSS3** - Custom styling với gradients và animations

### Database & Persistence
- **MySQL 8.0.34** - Database chính
- **Hibernate 6.6.33** - ORM framework
- **HikariCP** - Connection pooling

### Authentication & Security
- **Spring Security** - Authentication framework
- **JWT (JSON Web Tokens)** - Stateless authentication
- **BCrypt** - Password hashing
- **Session Management** - Hybrid JWT-Session approach

### Payment Integration
- **VNPay Sandbox** - Payment gateway (demo mode)
- **Payment Service** - Xử lý thanh toán và callback

### Development Tools
- **Maven** - Build tool và dependency management
- **Java 22** - Programming language
- **Tomcat 10.1.48** - Embedded web server

## 🏗️ Kiến trúc hệ thống

### MVC Pattern
```
Controller Layer (Spring MVC)
├── HomeController - Trang chủ, booking, availability
├── AuthController - Authentication, JWT APIs
├── AdminController - Quản trị admin
├── PaymentController - Xử lý thanh toán
└── MyBookingsController - Lịch sử đặt chỗ

Service Layer
├── PaymentService - Logic thanh toán
├── NotificationService - WebSocket notifications
└── JwtUtil - JWT token management

Repository Layer (Spring Data JPA)
├── BookingRepository - Quản lý đặt chỗ
├── AppUserRepository - Quản lý người dùng
└── FieldImageRepository - Quản lý hình ảnh sân

Model Layer (JPA Entities)
├── Booking - Entity đặt chỗ
├── AppUser - Entity người dùng
└── FieldImage - Entity hình ảnh sân
```

### Database Schema
```sql
-- Bảng người dùng
app_users (
    id BIGINT PRIMARY KEY,
    username VARCHAR(50) UNIQUE,
    password_hash VARCHAR(255),
    full_name VARCHAR(100),
    phone VARCHAR(20),
    role VARCHAR(20),
    enabled BOOLEAN,
    activation_token VARCHAR(255)
)

-- Bảng đặt chỗ
bookings (
    id BIGINT PRIMARY KEY,
    slot INT,
    date DATE,
    start_time TIME,
    end_time TIME,
    name VARCHAR(100),
    phone VARCHAR(20),
    amount DECIMAL(10,2),
    payment_code VARCHAR(50),
    status VARCHAR(20),
    expires_at TIMESTAMP,
    paid_at TIMESTAMP,
    username VARCHAR(50)
)
```

## 🚀 Cài đặt và chạy dự án

### Yêu cầu hệ thống
- Java 22+
- Maven 3.6+
- MySQL 8.0+
- IDE (IntelliJ IDEA, Eclipse, VS Code)

### Cài đặt

1. **Clone repository**
```bash
git clone <repository-url>
cd UTEsoccer
```

2. **Cấu hình database**
```sql
CREATE DATABASE utesoccer;
-- Tạo user với password: dinh2103
CREATE USER 'root'@'localhost' IDENTIFIED BY 'dinh2103';
GRANT ALL PRIVILEGES ON utesoccer.* TO 'root'@'localhost';
```

3. **Cấu hình application.properties**
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/utesoccer
spring.datasource.username=root
spring.datasource.password=dinh2103
spring.jpa.hibernate.ddl-auto=update
```

4. **Chạy ứng dụng**
```bash
mvn spring-boot:run
```

5. **Truy cập ứng dụng**
- URL: http://localhost:8080
- Admin: admin/admin123
- User demo: user/user123

## 📱 Giao diện người dùng

### Responsive Design
- **Desktop**: Grid layout 3 cột cho sân
- **Tablet**: Grid layout 2 cột
- **Mobile**: Grid layout 1 cột
- **Bootstrap breakpoints**: sm, md, lg, xl

### UI Components
- **Modal Booking Form** - Form đặt chỗ với time slots
- **Time Slot Chips** - Khung giờ có thể chọn (xanh) và đã đặt (đỏ)
- **Admin Table** - Bảng quản lý với pagination
- **Payment Interface** - Giao diện thanh toán VNPay
- **User Dashboard** - Trang cá nhân người dùng

## 🔧 API Endpoints

### Public APIs
```
GET  /                    - Trang chủ
GET  /announcements       - Thông báo
GET  /contact            - Liên hệ
GET  /api/availability   - Kiểm tra khung giờ trống
POST /book               - Đặt chỗ
POST /payment/callback    - Callback thanh toán
```

### Authentication APIs
```
GET  /auth/login         - Trang đăng nhập
GET  /auth/register      - Trang đăng ký
POST /auth/api/login     - API đăng nhập (JWT)
POST /auth/api/register  - API đăng ký (JWT)
GET  /auth/validate      - Validate JWT token
```

### Admin APIs
```
GET  /admin              - Dashboard admin
GET  /admin/export       - Xuất CSV
GET  /admin/booking/{id} - Chi tiết đặt chỗ
```

### User APIs
```
GET  /me/bookings        - Lịch sử đặt chỗ
```

## 🔐 Bảo mật

### Authentication Flow
1. **Form Login** - Spring Security form authentication
2. **JWT Generation** - Tạo JWT token sau khi login thành công
3. **Session Storage** - Lưu JWT trong HTTP session
4. **Token Validation** - Validate JWT cho API calls
5. **Role-based Access** - Phân quyền ADMIN/USER

### Security Features
- **Password Hashing** - BCrypt với salt rounds
- **CSRF Protection** - Disabled cho demo
- **Session Management** - Maximum 1 session per user
- **Input Validation** - Server-side validation
- **SQL Injection Prevention** - JPA parameterized queries

## 💳 Thanh toán

### VNPay Integration
- **Sandbox Mode** - Môi trường test
- **Payment Flow**:
  1. Tạo booking với status PENDING
  2. Redirect đến VNPay
  3. Callback xử lý kết quả
  4. Cập nhật status thành CONFIRMED
- **Timeout Handling** - Hủy booking sau 3 phút nếu chưa thanh toán

### Payment States
- **PENDING** - Chờ thanh toán (3 phút timeout)
- **CONFIRMED** - Đã thanh toán thành công
- **CANCELLED** - Hủy hoặc timeout

## 📊 Quản lý dữ liệu

### Booking Management
- **Real-time Availability** - API kiểm tra khung giờ trống
- **Conflict Prevention** - Không cho phép đặt trùng khung giờ
- **Status Tracking** - Theo dõi trạng thái đặt chỗ
- **Cleanup Jobs** - Tự động hủy booking hết hạn

### User Management
- **Registration** - Đăng ký với validation
- **Profile Management** - Quản lý thông tin cá nhân
- **Booking History** - Lịch sử đặt chỗ
- **Role Assignment** - Phân quyền ADMIN/USER

## 🎨 UI/UX Design

### Design System
- **Color Palette**:
  - Primary: #3182ce (Blue)
  - Success: #38a169 (Green)
  - Danger: #dc3545 (Red)
  - Warning: #d69e2e (Orange)
- **Typography**: Inter font family
- **Spacing**: 8px grid system
- **Shadows**: Layered shadow system

### Interactive Elements
- **Hover Effects** - Transform và shadow transitions
- **Loading States** - Pulse animations
- **Form Validation** - Real-time feedback
- **Modal Animations** - Slide-in effects
- **Responsive Images** - SVG field map

## 🔄 Real-time Features

### WebSocket Integration
- **STOMP Protocol** - Message broker
- **Notification Service** - Real-time updates
- **Booking Status Updates** - Live status changes
- **Admin Notifications** - New booking alerts

## 📈 Performance & Optimization

### Database Optimization
- **Connection Pooling** - HikariCP configuration
- **Query Optimization** - JPA query methods
- **Indexing** - Database indexes on frequently queried columns
- **Lazy Loading** - JPA lazy loading for relationships

### Frontend Optimization
- **CSS Variables** - Consistent theming
- **Responsive Images** - Optimized image loading
- **JavaScript Bundling** - Minified scripts
- **Caching** - Static resource caching

## 🧪 Testing

### Test Accounts
- **Admin**: admin/admin123
- **User**: user/user123
- **Demo Data**: Tự động tạo khi khởi động

### Test Scenarios
- **Booking Flow** - End-to-end booking process
- **Payment Flow** - VNPay integration testing
- **Authentication** - Login/logout functionality
- **Admin Functions** - Management features

## 🚀 Deployment

### Production Considerations
- **Environment Variables** - Database credentials
- **HTTPS Configuration** - SSL certificates
- **Database Migration** - Flyway or Liquibase
- **Monitoring** - Application metrics
- **Logging** - Structured logging

### Docker Support
```dockerfile
FROM openjdk:22-jdk-slim
COPY target/UTEsoccer-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app.jar"]
```

## 📝 Development Guidelines

### Code Structure
- **Package Organization** - Layered architecture
- **Naming Conventions** - Java naming standards
- **Documentation** - Javadoc comments
- **Error Handling** - Comprehensive exception handling

### Git Workflow
- **Feature Branches** - Feature-based development
- **Commit Messages** - Conventional commit format
- **Code Review** - Pull request reviews
- **CI/CD** - Automated testing and deployment

## 🔮 Future Enhancements

### Planned Features
- **Mobile App** - React Native or Flutter
- **Cloudinary Integration** - Image management
- **Email Notifications** - Booking confirmations
- **Multi-language Support** - i18n implementation
- **Analytics Dashboard** - Usage statistics

### Technical Improvements
- **Microservices Architecture** - Service decomposition
- **Redis Caching** - Performance optimization
- **Elasticsearch** - Search functionality
- **Kubernetes** - Container orchestration
- **Monitoring** - Prometheus + Grafana

## 📞 Support & Contact

### Development Team
- **Project Lead**: Development Team
- **Backend**: Spring Boot, JPA, Security
- **Frontend**: Thymeleaf, Bootstrap, JavaScript
- **Database**: MySQL, Hibernate

### Documentation
- **API Documentation**: Swagger/OpenAPI
- **User Manual**: In-app help system
- **Admin Guide**: Management documentation
- **Developer Guide**: Technical documentation

---

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 🙏 Acknowledgments

- Spring Boot community for excellent framework
- Bootstrap team for responsive design tools
- VNPay for payment gateway integration
- MySQL team for reliable database system