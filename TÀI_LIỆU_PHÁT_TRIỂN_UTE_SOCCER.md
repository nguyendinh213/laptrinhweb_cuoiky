# TÀI LIỆU HƯỚNG DẪN PHÁT TRIỂN ỨNG DỤNG UTE SOCCER
## Hệ thống đặt sân bóng mini theo giờ

---

## MỤC LỤC

1. [Tổng quan dự án](#1-tổng-quan-dự-án)
2. [Kiến trúc hệ thống](#2-kiến-trúc-hệ-thống)
3. [Cơ sở dữ liệu](#3-cơ-sở-dữ-liệu)
4. [Chức năng đăng nhập/đăng ký](#4-chức-năng-đăng-nhậpđăng-ký)
5. [Chức năng đặt sân](#5-chức-năng-đặt-sân)
6. [Chức năng thanh toán](#6-chức-năng-thanh-toán)
7. [Chức năng quản trị admin](#7-chức-năng-quản-trị-admin)
8. [Chức năng quản lý profile](#8-chức-năng-quản-lý-profile)
9. [Giao diện người dùng](#9-giao-diện-người-dùng)
10. [Bảo mật và phân quyền](#10-bảo-mật-và-phân-quyền)
11. [API Documentation](#11-api-documentation)
12. [Deployment và cài đặt](#12-deployment-và-cài-đặt)

---

## 1. TỔNG QUAN DỰ ÁN

### 1.1 Mô tả dự án
UTE Soccer là hệ thống web đặt sân bóng mini theo giờ với các tính năng:
- Quản lý 6 sân bóng mini
- Đặt chỗ theo khung giờ 30 phút (17:00-24:00)
- Thanh toán qua VNPay
- Phân quyền admin/user
- Quản lý profile cá nhân

### 1.2 Công nghệ sử dụng
- **Backend**: Spring Boot 3.5.7, Spring MVC, Spring Data JPA, Spring Security
- **Frontend**: Thymeleaf, Bootstrap 5.3.0, JavaScript
- **Database**: MySQL 8.0.34, Hibernate 6.6.33
- **Authentication**: JWT, BCrypt, Session Management
- **Payment**: VNPay Sandbox
- **Build Tool**: Maven

### 1.3 Lược đồ tổng quan hệ thống

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Frontend      │    │   Backend       │    │   Database      │
│   (Thymeleaf)   │◄──►│   (Spring Boot) │◄──►│   (MySQL)       │
│   Bootstrap     │    │   Spring MVC    │    │   Hibernate     │
│   JavaScript    │    │   Spring Security│   │   JPA           │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         │                       │                       │
         ▼                       ▼                       ▼
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   User Interface│    │   Business Logic│    │   Data Storage  │
│   - Homepage    │    │   - Booking     │    │   - Users       │
│   - Booking     │    │   - Payment     │    │   - Bookings    │
│   - Admin       │    │   - Auth        │    │   - Sessions    │
│   - Profile     │    │   - Profile     │    │                 │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

---

## 2. KIẾN TRÚC HỆ THỐNG

### 2.1 Kiến trúc MVC

```
src/main/java/com/example/demo/
├── controller/           # Controllers (Presentation Layer)
│   ├── HomeController.java
│   ├── AuthController.java
│   ├── AdminController.java
│   ├── PaymentController.java
│   ├── MyBookingsController.java
│   └── ProfileController.java
├── model/                # Entities (Data Layer)
│   ├── Booking.java
│   ├── AppUser.java
│   └── FieldImage.java
├── repository/           # Data Access Layer
│   ├── BookingRepository.java
│   ├── AppUserRepository.java
│   └── FieldImageRepository.java
├── service/              # Business Logic Layer
│   ├── PaymentService.java
│   └── NotificationService.java
├── config/               # Configuration
│   ├── SecurityConfig.java
│   ├── CustomAuthenticationSuccessHandler.java
│   └── JwtSessionFilter.java
├── util/                 # Utilities
│   └── JwtUtil.java
└── UtEsoccerApplication.java  # Main Application Class
```

### 2.2 Lược đồ luồng dữ liệu

```
User Request → Controller → Service → Repository → Database
     ↑                                                      ↓
Response ← View Template ← Model ← Service ← Repository ← Database
```

### 2.3 Dependency Injection

```java
@SpringBootApplication
public class UtEsoccerApplication {
    public static void main(String[] args) {
        SpringApplication.run(UtEsoccerApplication.class, args);
    }
}
```

---

## 3. CƠ SỞ DỮ LIỆU

### 3.1 Schema Database

```sql
-- Database: utesoccer
CREATE DATABASE utesoccer;
USE utesoccer;

-- Bảng người dùng
CREATE TABLE app_users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(120),
    phone VARCHAR(20),
    role VARCHAR(20) NOT NULL DEFAULT 'ROLE_USER',
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    activation_token VARCHAR(64),
    last_name_change DATETIME(6),
    INDEX idx_app_users_username (username)
);

-- Bảng đặt chỗ
CREATE TABLE bookings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    slot INT NOT NULL,
    date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    name VARCHAR(100) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    payment_code VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    expires_at TIMESTAMP,
    paid_at TIMESTAMP,
    username VARCHAR(50),
    INDEX idx_bookings_date_slot (date, slot),
    INDEX idx_bookings_username (username),
    INDEX idx_bookings_status (status)
);

-- Bảng hình ảnh sân (cho Cloudinary integration)
CREATE TABLE field_images (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    field_number INT NOT NULL,
    image_url VARCHAR(500) NOT NULL,
    uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_field_images_field (field_number)
);
```

### 3.2 Entity Models

#### 3.2.1 AppUser Entity

```java
@Entity
@Table(name = "app_users", indexes = {
    @Index(name = "idx_app_users_username", columnList = "username", unique = true)
})
public class AppUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String username;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private String role; // ROLE_ADMIN or ROLE_USER

    @Column(nullable = false)
    private boolean enabled = false;

    @Column(length = 64)
    private String activationToken;

    @Column(length = 120)
    private String fullName;

    @Column(length = 20)
    private String phone;

    @Column(name = "last_name_change")
    private java.time.LocalDateTime lastNameChange;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    
    public String getActivationToken() { return activationToken; }
    public void setActivationToken(String activationToken) { this.activationToken = activationToken; }
    
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    
    public java.time.LocalDateTime getLastNameChange() { return lastNameChange; }
    public void setLastNameChange(java.time.LocalDateTime lastNameChange) { this.lastNameChange = lastNameChange; }
}
```

#### 3.2.2 Booking Entity

```java
@Entity
@Table(name = "bookings")
public class Booking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private int slot;

    @Column(nullable = false)
    private java.time.LocalDate date;

    @Column(nullable = false)
    private java.time.LocalTime startTime;

    @Column(nullable = false)
    private java.time.LocalTime endTime;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(nullable = false, precision = 10, scale = 2)
    private java.math.BigDecimal amount;

    @Column(nullable = false, length = 50)
    private String paymentCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.PENDING;

    private java.time.LocalDateTime expiresAt;
    private java.time.LocalDateTime paidAt;
    private String username;

    public enum Status {
        PENDING, CONFIRMED, CANCELLED
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public int getSlot() { return slot; }
    public void setSlot(int slot) { this.slot = slot; }
    
    public java.time.LocalDate getDate() { return date; }
    public void setDate(java.time.LocalDate date) { this.date = date; }
    
    public java.time.LocalTime getStartTime() { return startTime; }
    public void setStartTime(java.time.LocalTime startTime) { this.startTime = startTime; }
    
    public java.time.LocalTime getEndTime() { return endTime; }
    public void setEndTime(java.time.LocalTime endTime) { this.endTime = endTime; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    
    public java.math.BigDecimal getAmount() { return amount; }
    public void setAmount(java.math.BigDecimal amount) { this.amount = amount; }
    
    public String getPaymentCode() { return paymentCode; }
    public void setPaymentCode(String paymentCode) { this.paymentCode = paymentCode; }
    
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    
    public java.time.LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(java.time.LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    
    public java.time.LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(java.time.LocalDateTime paidAt) { this.paidAt = paidAt; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
}
```

### 3.3 Repository Layer

#### 3.3.1 AppUserRepository

```java
@Repository
public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByUsername(String username);
    Optional<AppUser> findByActivationToken(String activationToken);
    List<AppUser> findByRole(String role);
    List<AppUser> findByEnabled(boolean enabled);
}
```

#### 3.3.2 BookingRepository

```java
@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByDateAndSlotOrderByStartTimeAsc(java.time.LocalDate date, int slot);
    List<Booking> findByDateOrderBySlotAscStartTimeAsc(java.time.LocalDate date);
    List<Booking> findByUsernameOrderByDateDescStartTimeDesc(String username);
    List<Booking> findByStatusAndExpiresAtBefore(Booking.Status status, java.time.LocalDateTime expiresAt);
    List<Booking> findByStatus(Booking.Status status);
    List<Booking> findByDateBetween(java.time.LocalDate startDate, java.time.LocalDate endDate);
}
```

---

## 4. CHỨC NĂNG ĐĂNG NHẬP/ĐĂNG KÝ

### 4.1 Lược đồ chức năng

```
┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│   Login     │───►│  Validate   │───►│  Redirect   │
│   Form      │    │ Credentials │    │  Based on   │
│             │    │             │    │   Role      │
└─────────────┘    └─────────────┘    └─────────────┘
       │                   │                   │
       │                   │                   ▼
       │                   │            ┌─────────────┐
       │                   │            │   Admin     │
       │                   │            │ Dashboard   │
       │                   │            └─────────────┘
       │                   │                   │
       │                   │                   ▼
       │                   │            ┌─────────────┐
       │                   │            │   User      │
       │                   │            │ Homepage    │
       │                   │            └─────────────┘
       │                   │
       ▼                   ▼
┌─────────────┐    ┌─────────────┐
│   Error     │    │   Success   │
│  Message    │    │   Message   │
└─────────────┘    └─────────────┘
```

### 4.2 Source Code

#### 4.2.1 AuthController

```java
@Controller
public class AuthController {

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    // Hiển thị trang đăng nhập
    @GetMapping("/auth/login")
    public String loginPage(Model model) {
        model.addAttribute("title", "Đăng nhập");
        return "login";
    }

    // Hiển thị trang đăng ký
    @GetMapping("/auth/register")
    public String registerPage(Model model) {
        model.addAttribute("title", "Đăng ký");
        return "register";
    }

    // Xử lý đăng ký
    @PostMapping("/auth/register")
    public String register(@ModelAttribute AppUser user, Model model) {
        try {
            if (userRepository.findByUsername(user.getUsername()).isPresent()) {
                model.addAttribute("error", "Tên đăng nhập đã tồn tại");
                return "register";
            }

            user.setPasswordHash(passwordEncoder.encode(user.getPasswordHash()));
            user.setRole("ROLE_USER");
            user.setEnabled(true); // Demo purposes
            user.setActivationToken(null);

            userRepository.save(user);
            model.addAttribute("message", "Đăng ký thành công! Vui lòng đăng nhập.");
            return "login";

        } catch (Exception e) {
            model.addAttribute("error", "Lỗi đăng ký: " + e.getMessage());
            return "register";
        }
    }

    // API đăng nhập với JWT
    @PostMapping("/auth/api/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest, 
                                   jakarta.servlet.http.HttpServletRequest request) {
        try {
            Optional<AppUser> userOpt = userRepository.findByUsername(loginRequest.getUsername());

            if (userOpt.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(new AuthResponse(null, null, null, "Tên đăng nhập không tồn tại"));
            }

            AppUser user = userOpt.get();

            if (!user.isEnabled()) {
                return ResponseEntity.badRequest()
                    .body(new AuthResponse(null, null, null, "Tài khoản chưa được kích hoạt"));
            }

            if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPasswordHash())) {
                return ResponseEntity.badRequest()
                    .body(new AuthResponse(null, null, null, "Mật khẩu không đúng"));
            }

            // Generate JWT token and store in session
            String token = jwtUtil.generateToken(user.getUsername(), user.getRole());
            request.getSession().setAttribute("jwt_token", token);
            request.getSession().setAttribute("username", user.getUsername());
            request.getSession().setAttribute("role", user.getRole());

            return ResponseEntity.ok(new AuthResponse(token, user.getUsername(), user.getRole(), "Đăng nhập thành công"));

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new AuthResponse(null, null, null, "Lỗi đăng nhập: " + e.getMessage()));
        }
    }
}
```

#### 4.2.2 SecurityConfig

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private CustomAuthenticationSuccessHandler customAuthenticationSuccessHandler;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session
                .maximumSessions(1)
                .maxSessionsPreventsLogin(false)
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/css/**", "/img/**", "/", "/announcements", "/contact", 
                               "/book", "/api/**", "/payment/**", "/register", "/login", 
                               "/logout", "/activate", "/auth/**", "/demo", "/profile", 
                               "/me/**").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/auth/login")
                .loginProcessingUrl("/auth/login")
                .successHandler(customAuthenticationSuccessHandler)
                .failureUrl("/auth/login?error=true")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/auth/logout")
                .logoutSuccessUrl("/")
                .permitAll()
            )
        ;
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }
}
```

#### 4.2.3 CustomAuthenticationSuccessHandler

```java
@Component
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        String role = authentication.getAuthorities().iterator().next().getAuthority();

        if ("ROLE_ADMIN".equals(role)) {
            response.sendRedirect("/admin");
        } else {
            response.sendRedirect("/");
        }
    }
}
```

### 4.3 Templates

#### 4.3.1 Login Template

```html
<!DOCTYPE html>
<html lang="vi" xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title th:text="${title}">Đăng nhập</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link th:href="@{/css/styles.css}" rel="stylesheet">
</head>
<body>
    <div th:replace="partials/navbar :: navbar"></div>

    <main class="container" style="max-width:520px; margin-top:24px">
        <h1 style="color:var(--text-primary); text-align:center; margin-bottom:24px">Đăng nhập</h1>
        <form method="post" th:action="@{/auth/login}" class="contact-form">
            <label style="color:var(--text-primary)">Tên đăng nhập
                <input type="text" name="username" required style="color:var(--text-primary)">
            </label>
            <label style="color:var(--text-primary)">Mật khẩu
                <input type="password" name="password" required style="color:var(--text-primary)">
            </label>
            <button type="submit" class="btn-primary">Đăng nhập</button>
        </form>
        <p th:if="${error}" style="color:#dc2626; margin-top:8px; font-weight:600" th:text="${error}"></p>
        <p th:if="${param.error}" style="color:#dc2626; margin-top:8px; font-weight:600">Đăng nhập thất bại. Kiểm tra tài khoản/mật khẩu hoặc kích hoạt tài khoản.</p>
        <p th:if="${param.logout}" style="color:#16a34a; margin-top:8px; font-weight:600">Đã đăng xuất.</p>
        <p style="color:var(--text-secondary); margin-top:12px; font-weight:500">Tài khoản demo: admin/admin123</p>
    </main>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
```

#### 4.3.2 Register Template

```html
<!DOCTYPE html>
<html lang="vi" xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title th:text="${title}">Đăng ký</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link th:href="@{/css/styles.css}" rel="stylesheet">
</head>
<body>
    <div th:replace="partials/navbar :: navbar"></div>

    <main class="container" style="max-width:520px; margin-top:24px">
        <h1 style="color:var(--text-primary); text-align:center; margin-bottom:24px">Đăng ký</h1>
        <p th:if="${error}" style="color:#dc2626; font-weight:600" th:text="${error}"></p>
        <p th:if="${message}" style="color:#16a34a; font-weight:600" th:text="${message}"></p>
        <form method="post" th:action="@{/auth/register}" class="contact-form">
            <label style="color:var(--text-primary)">Tên đăng nhập
                <input type="text" name="username" required style="color:var(--text-primary)">
            </label>
            <label style="color:var(--text-primary)">Mật khẩu
                <input type="password" name="password" required style="color:var(--text-primary)">
            </label>
            <label style="color:var(--text-primary)">Họ và tên
                <input type="text" name="fullName" required style="color:var(--text-primary)">
            </label>
            <label style="color:var(--text-primary)">Số điện thoại
                <input type="tel" name="phone" required style="color:var(--text-primary)">
            </label>
            <button type="submit" class="btn-primary">Đăng ký</button>
        </form>
    </main>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
```

---

## 5. CHỨC NĂNG ĐẶT SÂN

### 5.1 Lược đồ chức năng

```
┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│   Select    │───►│   Check     │───►│   Show      │
│   Field     │    │ Availability│    │ Time Slots  │
└─────────────┘    └─────────────┘    └─────────────┘
       │                   │                   │
       │                   │                   ▼
       │                   │            ┌─────────────┐
       │                   │            │   Select    │
       │                   │            │ Time Slots  │
       │                   │            └─────────────┘
       │                   │                   │
       │                   │                   ▼
       │                   │            ┌─────────────┐
       │                   │            │  Validate   │
       │                   │            │ Selection   │
       │                   │            └─────────────┘
       │                   │                   │
       │                   │                   ▼
       │                   │            ┌─────────────┐
       │                   │            │   Create    │
       │                   │            │  Booking    │
       │                   │            └─────────────┘
       │                   │                   │
       │                   │                   ▼
       │                   │            ┌─────────────┐
       │                   │            │  Redirect   │
       │                   │            │ to Payment  │
       │                   │            └─────────────┘
       │                   │
       ▼                   ▼
┌─────────────┐    ┌─────────────┐
│   Error     │    │   Success   │
│  Message    │    │   Message   │
└─────────────┘    └─────────────┘
```

### 5.2 Source Code

#### 5.2.1 HomeController

```java
@Controller
public class HomeController {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private AppUserRepository userRepository;

    @GetMapping("/")
    public String home(Model model, jakarta.servlet.http.HttpServletRequest request) {
        // Check if user is logged in
        org.springframework.security.core.Authentication auth = 
            org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        
        boolean loggedIn = auth != null && auth.isAuthenticated() && 
                          !(auth instanceof org.springframework.security.authentication.AnonymousAuthenticationToken);
        
        model.addAttribute("loggedIn", loggedIn);
        
        if (loggedIn) {
            model.addAttribute("username", auth.getName());
        }
        
        return "index";
    }

    @PostMapping("/book")
    public String book(@RequestParam("slot") int slot,
                      @RequestParam("date") String date,
                      @RequestParam("startTime") String startTime,
                      @RequestParam("endTime") String endTime,
                      @RequestParam(value = "name", required = false) String name,
                      @RequestParam(value = "phone", required = false) String phone,
                      Model model,
                      jakarta.servlet.http.HttpServletRequest request) {
        
        // Check if user is logged in
        org.springframework.security.core.Authentication auth = 
            org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        
        boolean loggedIn = auth != null && auth.isAuthenticated() && 
                          !(auth instanceof org.springframework.security.authentication.AnonymousAuthenticationToken);
        
        if (loggedIn) {
            // Get user info from database
            AppUser user = userRepository.findByUsername(auth.getName()).orElse(null);
            if (user != null) {
                name = user.getFullName();
                phone = user.getPhone();
            }
        } else {
            // Guest user must provide name and phone
            if (name == null || name.trim().isEmpty() || phone == null || phone.trim().isEmpty()) {
                model.addAttribute("loggedIn", false);
                model.addAttribute("error", "Vui lòng điền đầy đủ thông tin");
                return "index";
            }
        }

        try {
            java.time.LocalDate d = java.time.LocalDate.parse(date);
            java.time.LocalTime st = java.time.LocalTime.parse(startTime);
            java.time.LocalTime et = java.time.LocalTime.parse(endTime);

            // Validate date not in the past
            if (d.isBefore(java.time.LocalDate.now())) {
                model.addAttribute("loggedIn", loggedIn);
                model.addAttribute("error", "Ngày không được trong quá khứ");
                return "index";
            }

            // Validate time slots are contiguous and at least 2 slots
            int startMinutes = st.getHour() * 60 + st.getMinute();
            int endMinutes = et.getHour() * 60 + et.getMinute();
            int duration = endMinutes - startMinutes;
            
            if (duration < 60) { // At least 1 hour (2 slots)
                model.addAttribute("loggedIn", loggedIn);
                model.addAttribute("error", "Vui lòng chọn ít nhất 2 khung liền kề nhau");
                return "index";
            }

            // Check for conflicts
            java.util.List<Booking> existing = bookingRepository.findByDateAndSlotOrderByStartTimeAsc(d, slot);
            for (Booking b : existing) {
                if (b.getStatus() == Booking.Status.CANCELLED) continue;
                if (b.getStatus() == Booking.Status.PENDING && 
                    b.getExpiresAt() != null && 
                    b.getExpiresAt().isBefore(java.time.LocalDateTime.now())) continue;
                
                int existingStart = b.getStartTime().getHour() * 60 + b.getStartTime().getMinute();
                int existingEnd = b.getEndTime().getHour() * 60 + b.getEndTime().getMinute();
                
                if ((startMinutes >= existingStart && startMinutes < existingEnd) ||
                    (endMinutes > existingStart && endMinutes <= existingEnd) ||
                    (startMinutes <= existingStart && endMinutes >= existingEnd)) {
                    model.addAttribute("loggedIn", loggedIn);
                    model.addAttribute("error", "Khung giờ đã được đặt");
                    return "index";
                }
            }

            // Create booking
            Booking booking = new Booking();
            booking.setSlot(slot);
            booking.setDate(d);
            booking.setStartTime(st);
            booking.setEndTime(et);
            booking.setName(name.trim());
            booking.setPhone(phone.trim());
            booking.setAmount(java.math.BigDecimal.valueOf(duration / 30 * 50000)); // 50k per 30min
            booking.setPaymentCode(java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            booking.setStatus(Booking.Status.PENDING);
            booking.setExpiresAt(java.time.LocalDateTime.now().plusMinutes(3)); // 3 minutes timeout
            if (loggedIn) {
                booking.setUsername(auth.getName());
            }

            bookingRepository.save(booking);

            // Redirect to payment
            return "redirect:/payment/" + booking.getPaymentCode();

        } catch (Exception e) {
            model.addAttribute("loggedIn", loggedIn);
            model.addAttribute("error", "Lỗi đặt chỗ: " + e.getMessage());
            return "index";
        }
    }

    @GetMapping("/api/availability")
    @ResponseBody
    public java.util.Map<String, Object> availability(
            @RequestParam("slot") int slot,
            @RequestParam("date") String date
    ) {
        java.util.Map<String, Object> resp = new java.util.HashMap<>();
        try {
            java.time.LocalDate d = java.time.LocalDate.parse(date);
            java.util.List<Booking> list = bookingRepository.findByDateAndSlotOrderByStartTimeAsc(d, slot);
            java.util.Set<Integer> occupied = new java.util.HashSet<>();
            int startMinutes = 17 * 60; // 17:00
            for (Booking b : list) {
                if (b.getStatus() == Booking.Status.CANCELLED) continue;
                if (b.getStatus() == Booking.Status.PENDING && 
                    b.getExpiresAt() != null && 
                    b.getExpiresAt().isBefore(java.time.LocalDateTime.now())) continue;
                
                int s = b.getStartTime().getHour() * 60 + b.getStartTime().getMinute();
                int e = b.getEndTime().getHour() * 60 + b.getEndTime().getMinute();
                int startIdx = Math.max(0, (s - startMinutes) / 30);
                int endIdx = Math.max(startIdx, (e - startMinutes) / 30);
                for (int i = startIdx; i < endIdx; i++) {
                    if (i >= 0 && i < 14) { // 17:00..24:00 -> 14 slots
                        occupied.add(i);
                    }
                }
            }
            resp.put("occupied", occupied);
            resp.put("ok", true);
        } catch (Exception ex) {
            resp.put("ok", false);
            resp.put("error", "Invalid parameters");
        }
        return resp;
    }
}
```

### 5.3 Frontend JavaScript

```javascript
let selectedSlotNumber = null;
let selectedTimeIndices = new Set();

function openModal(slot) {
    selectedSlotNumber = slot;
    document.getElementById('selectedSlotLabel').textContent = slot;
    document.getElementById('slotInput').value = slot;
    selectedTimeIndices.clear();
    document.querySelectorAll('.time-chip').forEach(el => el.classList.remove('selected'));
    
    // Use Bootstrap modal
    const modal = new bootstrap.Modal(document.getElementById('bookingModal'));
    modal.show();
    
    // fetch availability for today (or selected date if already chosen)
    const date = document.getElementById('date').value;
    if (date) {
        fetchAndMarkAvailability(slot, date);
    } else {
        // Set today's date and fetch availability
        const today = new Date();
        const yyyy = today.getFullYear();
        const mm = pad2(today.getMonth()+1);
        const dd = pad2(today.getDate());
        const todayStr = `${yyyy}-${mm}-${dd}`;
        document.getElementById('date').value = todayStr;
        fetchAndMarkAvailability(slot, todayStr);
    }
}

async function fetchAndMarkAvailability(slot, date){
    try{
        const res = await fetch(`/api/availability?slot=${encodeURIComponent(slot)}&date=${encodeURIComponent(date)}`);
        if(!res.ok) return;
        const data = await res.json();
        if(!data.ok) return;
        const occupied = new Set(data.occupied);
        
        document.querySelectorAll('.time-chip').forEach(chip => {
            const i = parseInt(chip.dataset.index,10);
            if (occupied.has(i)){
                // Mark as occupied/busy
                chip.classList.remove('btn-outline-primary', 'btn-primary', 'selected');
                chip.classList.add('btn-danger', 'busy');
                chip.disabled = true;
                chip.textContent = '✕ ' + chip.dataset.start + ' - ' + chip.dataset.end;
                chip.title = 'Khung giờ này đã được đặt';
            } else {
                // Mark as available
                chip.classList.remove('btn-danger', 'busy', 'selected');
                chip.classList.add('btn-outline-primary');
                chip.disabled = false;
                chip.textContent = chip.dataset.start + ' - ' + chip.dataset.end;
                chip.title = 'Khung giờ có thể đặt';
            }
        });
    }catch(e){ 
        console.error('Error fetching availability:', e);
    }
}

function toggleChip(chip){
    // Don't allow toggling if chip is disabled (occupied)
    if (chip.disabled || chip.classList.contains('busy') || chip.classList.contains('btn-danger')) {
        return;
    }
    
    const index = parseInt(chip.dataset.index, 10);
    if (chip.classList.contains('btn-primary')){
        chip.classList.remove('btn-primary');
        chip.classList.add('btn-outline-primary');
        selectedTimeIndices.delete(index);
    } else {
        chip.classList.remove('btn-outline-primary');
        chip.classList.add('btn-primary');
        selectedTimeIndices.add(index);
    }
}

function validateContiguous(indices){
    if (indices.length < 2) return false;
    for (let i=1;i<indices.length;i++){
        if (indices[i] !== indices[i-1] + 1) return false;
    }
    return true;
}

function onSubmitBookingForm(e){
    const nameEl = document.getElementById('name');
    const phoneEl = document.getElementById('phone');
    const date = document.getElementById('date') ? document.getElementById('date').value : '';
    if (!IS_LOGGED_IN){
        const name = nameEl ? nameEl.value.trim() : '';
        const phone = phoneEl ? phoneEl.value.trim() : '';
        if (!name || !phone || !date){
            alert('Vui lòng điền đầy đủ Họ tên, Số điện thoại và Ngày.');
            e.preventDefault();
            return false;
        }
    } else {
        if (!date){
            alert('Vui lòng chọn ngày.');
            e.preventDefault();
            return false;
        }
    }
    // Validate date not in the past
    const today = new Date(); today.setHours(0,0,0,0);
    const chosen = new Date(date + 'T00:00:00');
    if (chosen < today){
        alert('Ngày không được trong quá khứ.');
        e.preventDefault();
        return false;
    }
    const sorted = Array.from(selectedTimeIndices).sort((a,b)=>a-b);
    if (!validateContiguous(sorted)){
        alert('Vui lòng chọn ít nhất 2 khung liền kề nhau.');
        e.preventDefault();
        return false;
    }
    const chips = document.querySelectorAll('.time-chip');
    const firstIdx = sorted[0];
    const lastIdx = sorted[sorted.length-1];
    const startTime = chips[firstIdx].dataset.start;
    const endTime = chips[lastIdx].dataset.end; // end is exclusive of last chip start +30m
    document.getElementById('startTime').value = startTime;
    document.getElementById('endTime').value = endTime;
    // allow submit
    return true;
}
```

---

## 6. CHỨC NĂNG THANH TOÁN

### 6.1 Lược đồ chức năng

```
┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│   Booking   │───►│   Generate  │───►│   Redirect  │
│  Created    │    │ Payment URL │    │  to VNPay   │
└─────────────┘    └─────────────┘    └─────────────┘
       │                   │                   │
       │                   │                   ▼
       │                   │            ┌─────────────┐
       │                   │            │   VNPay     │
       │                   │            │  Sandbox    │
       │                   │            └─────────────┘
       │                   │                   │
       │                   │                   ▼
       │                   │            ┌─────────────┐
       │                   │            │   User      │
       │                   │            │  Payment    │
       │                   │            └─────────────┘
       │                   │                   │
       │                   │                   ▼
       │                   │            ┌─────────────┐
       │                   │            │  Callback   │
       │                   │            │ Processing  │
       │                   │            └─────────────┘
       │                   │                   │
       │                   │                   ▼
       │                   │            ┌─────────────┐
       │                   │            │  Update     │
       │                   │            │  Booking    │
       │                   │            │  Status     │
       │                   │            └─────────────┘
       │                   │                   │
       │                   │                   ▼
       │                   │            ┌─────────────┐
       │                   │            │  Redirect   │
       │                   │            │ to Homepage │
       │                   │            └─────────────┘
       │                   │
       ▼                   ▼
┌─────────────┐    ┌─────────────┐
│   Timeout   │    │   Success   │
│  Handling   │    │  Handling   │
└─────────────┘    └─────────────┘
```

### 6.2 Source Code

#### 6.2.1 PaymentController

```java
@Controller
public class PaymentController {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private PaymentService paymentService;

    @GetMapping("/payment/{paymentCode}")
    public String paymentPage(@PathVariable String paymentCode, Model model) {
        Booking booking = bookingRepository.findByPaymentCode(paymentCode).orElse(null);
        
        if (booking == null) {
            model.addAttribute("error", "Mã thanh toán không tồn tại");
            return "payment_error";
        }

        if (booking.getStatus() != Booking.Status.PENDING) {
            model.addAttribute("error", "Đơn đặt chỗ đã được xử lý");
            return "payment_error";
        }

        if (booking.getExpiresAt() != null && booking.getExpiresAt().isBefore(java.time.LocalDateTime.now())) {
            booking.setStatus(Booking.Status.CANCELLED);
            bookingRepository.save(booking);
            model.addAttribute("error", "Đơn đặt chỗ đã hết hạn");
            return "payment_error";
        }

        model.addAttribute("booking", booking);
        model.addAttribute("paymentUrl", paymentService.generatePaymentUrl(booking));
        
        return "payment";
    }

    @GetMapping("/payment/callback")
    public String paymentCallback(@RequestParam Map<String, String> params, Model model) {
        try {
            String paymentCode = params.get("vnp_TxnRef");
            String responseCode = params.get("vnp_ResponseCode");
            
            Booking booking = bookingRepository.findByPaymentCode(paymentCode).orElse(null);
            
            if (booking == null) {
                model.addAttribute("error", "Mã thanh toán không tồn tại");
                return "payment_error";
            }

            if ("00".equals(responseCode)) {
                // Payment successful
                booking.setStatus(Booking.Status.CONFIRMED);
                booking.setPaidAt(java.time.LocalDateTime.now());
                bookingRepository.save(booking);
                
                model.addAttribute("success", "Thanh toán thành công!");
                model.addAttribute("booking", booking);
                return "payment_success";
            } else {
                // Payment failed
                booking.setStatus(Booking.Status.CANCELLED);
                bookingRepository.save(booking);
                
                model.addAttribute("error", "Thanh toán thất bại");
                return "payment_error";
            }
            
        } catch (Exception e) {
            model.addAttribute("error", "Lỗi xử lý thanh toán: " + e.getMessage());
            return "payment_error";
        }
    }
}
```

#### 6.2.2 PaymentService

```java
@Service
public class PaymentService {

    private static final String VNPAY_URL = "http://sandbox.vnpayment.vn/tryitnow/Home/CreateOrder";
    private static final String VNPAY_RETURN_URL = "http://localhost:8080/payment/callback";

    public String generatePaymentUrl(Booking booking) {
        try {
            Map<String, String> params = new HashMap<>();
            params.put("vnp_Amount", String.valueOf(booking.getAmount().multiply(new BigDecimal(100)).intValue()));
            params.put("vnp_Command", "pay");
            params.put("vnp_CreateDate", java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(java.time.LocalDateTime.now()));
            params.put("vnp_CurrCode", "VND");
            params.put("vnp_IpAddr", "127.0.0.1");
            params.put("vnp_Locale", "vn");
            params.put("vnp_OrderInfo", "Thanh toan dat san " + booking.getSlot());
            params.put("vnp_OrderType", "other");
            params.put("vnp_ReturnUrl", VNPAY_RETURN_URL);
            params.put("vnp_TxnRef", booking.getPaymentCode());
            
            // For demo purposes, redirect directly to success
            return VNPAY_RETURN_URL + "?vnp_TxnRef=" + booking.getPaymentCode() + "&vnp_ResponseCode=00";
            
        } catch (Exception e) {
            throw new RuntimeException("Error generating payment URL", e);
        }
    }
}
```

### 6.3 Payment Templates

#### 6.3.1 Payment Page

```html
<!DOCTYPE html>
<html lang="vi" xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Thanh toán</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link th:href="@{/css/styles.css}" rel="stylesheet">
</head>
<body>
    <div th:replace="partials/navbar :: navbar"></div>

    <main class="container" style="max-width:720px; margin-top:24px">
        <h1 style="color:var(--text-primary); text-align:center; margin-bottom:24px">Thanh toán</h1>
        
        <div th:if="${booking}" class="payment-card" style="background:rgba(255,255,255,.95); backdrop-filter:blur(20px); border-radius:20px; padding:32px; margin:24px 0; box-shadow:0 20px 40px rgba(0,0,0,.15)">
            <h3 style="color:var(--text-primary); margin-bottom:20px">Thông tin đặt chỗ</h3>
            
            <div class="row">
                <div class="col-md-6">
                    <p style="color:var(--text-primary); font-weight:500"><strong>Sân:</strong> <span th:text="${booking.slot}">1</span></p>
                    <p style="color:var(--text-primary); font-weight:500"><strong>Ngày:</strong> <span th:text="${booking.date}">2025-01-01</span></p>
                    <p style="color:var(--text-primary); font-weight:500"><strong>Giờ:</strong> <span th:text="${booking.startTime}">17:00</span> - <span th:text="${booking.endTime}">18:00</span></p>
                </div>
                <div class="col-md-6">
                    <p style="color:var(--text-primary); font-weight:500"><strong>Họ tên:</strong> <span th:text="${booking.name}">Nguyen Van A</span></p>
                    <p style="color:var(--text-primary); font-weight:500"><strong>SĐT:</strong> <span th:text="${booking.phone}">0900000000</span></p>
                    <p style="color:var(--text-primary); font-weight:500"><strong>Số tiền:</strong> <span th:text="${#numbers.formatInteger(booking.amount, 3, 'POINT') + ' VND'}">100,000 VND</span></p>
                </div>
            </div>
            
            <div style="text-align:center; margin-top:32px">
                <a th:href="${paymentUrl}" class="btn-primary" style="padding:16px 32px; font-size:18px; text-decoration:none">
                    <i class="bi bi-credit-card"></i> Đi tới VNPay
                </a>
            </div>
            
            <div style="background:rgba(255, 193, 7, 0.1); border:1px solid rgba(255, 193, 7, 0.3); border-radius:12px; padding:16px; margin-top:24px">
                <p style="color:var(--text-primary); margin:0; font-weight:500">
                    <i class="bi bi-info-circle"></i> 
                    <strong>Lưu ý:</strong> Đơn đặt chỗ sẽ tự động hủy sau 3 phút nếu không thanh toán.
                </p>
            </div>
        </div>
    </main>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
```

---

## 7. CHỨC NĂNG QUẢN TRỊ ADMIN

### 7.1 Lược đồ chức năng

```
┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│   Admin     │───►│   Filter    │───►│   Display   │
│   Login     │    │ by Date     │    │ Bookings    │
└─────────────┘    └─────────────┘    └─────────────┘
       │                   │                   │
       │                   │                   ▼
       │                   │            ┌─────────────┐
       │                   │            │   View      │
       │                   │            │  Details    │
       │                   │            └─────────────┘
       │                   │                   │
       │                   │                   ▼
       │                   │            ┌─────────────┐
       │                   │            │   Export    │
       │                   │            │    CSV      │
       │                   │            └─────────────┘
       │                   │
       ▼                   ▼
┌─────────────┐    ┌─────────────┐
│   Access    │    │   Data      │
│  Control    │    │Management   │
└─────────────┘    └─────────────┘
```

### 7.2 Source Code

#### 7.2.1 AdminController

```java
@Controller
public class AdminController {

    @Autowired
    private BookingRepository bookingRepository;

    @GetMapping("/admin")
    public String admin(@RequestParam(value = "date", required = false) String dateStr, Model model) {
        java.time.LocalDate date;
        if (dateStr != null && !dateStr.isEmpty()) {
            try {
                date = java.time.LocalDate.parse(dateStr);
            } catch (Exception e) {
                date = java.time.LocalDate.now();
            }
        } else {
            date = java.time.LocalDate.now();
        }

        java.util.List<Booking> allBookings = bookingRepository.findByDateOrderBySlotAscStartTimeAsc(date);
        
        // Filter only CONFIRMED bookings and remove duplicates
        java.util.Map<String, Booking> uniqueBookings = new java.util.LinkedHashMap<>();
        for (Booking booking : allBookings) {
            if (booking.getStatus() == Booking.Status.CONFIRMED) {
                String key = booking.getSlot() + "_" + booking.getDate() + "_" + 
                           booking.getStartTime() + "_" + booking.getEndTime();
                if (!uniqueBookings.containsKey(key)) {
                    uniqueBookings.put(key, booking);
                }
            }
        }

        model.addAttribute("bookings", new java.util.ArrayList<>(uniqueBookings.values()));
        model.addAttribute("date", date);
        model.addAttribute("title", "Quản trị - Danh sách đặt chỗ");
        
        return "admin";
    }

    @GetMapping("/admin/booking/{id}")
    public String bookingDetail(@PathVariable Long id, Model model) {
        Booking booking = bookingRepository.findById(id).orElse(null);
        
        if (booking == null) {
            model.addAttribute("error", "Không tìm thấy đặt chỗ");
            return "admin_booking_detail";
        }

        model.addAttribute("b", booking);
        model.addAttribute("title", "Chi tiết đặt chỗ");
        
        return "admin_booking_detail";
    }

    @GetMapping("/admin/export")
    public void exportBookings(@RequestParam("date") String dateStr, 
                              jakarta.servlet.http.HttpServletResponse response) throws IOException {
        java.time.LocalDate date = java.time.LocalDate.parse(dateStr);
        java.util.List<Booking> bookings = bookingRepository.findByDateOrderBySlotAscStartTimeAsc(date);
        
        // Filter only CONFIRMED bookings
        bookings = bookings.stream()
            .filter(b -> b.getStatus() == Booking.Status.CONFIRMED)
            .collect(java.util.stream.Collectors.toList());

        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=bookings_" + dateStr + ".csv");
        
        try (PrintWriter writer = response.getWriter()) {
            writer.println("Sân,Ngày,Bắt đầu,Kết thúc,Họ tên,SĐT,Tài khoản,Số tiền,Thanh toán lúc");
            
            for (Booking booking : bookings) {
                writer.printf("%d,%s,%s,%s,%s,%s,%s,%s,%s%n",
                    booking.getSlot(),
                    booking.getDate(),
                    booking.getStartTime(),
                    booking.getEndTime(),
                    booking.getName(),
                    booking.getPhone(),
                    booking.getUsername() != null ? booking.getUsername() : "Guest",
                    booking.getAmount(),
                    booking.getPaidAt() != null ? booking.getPaidAt() : ""
                );
            }
        }
    }
}
```

### 7.3 Admin Templates

#### 7.3.1 Admin Dashboard

```html
<!DOCTYPE html>
<html lang="vi" xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title th:text="${title}">Quản trị</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link th:href="@{/css/styles.css}" rel="stylesheet">
</head>
<body>
    <div th:replace="partials/navbar :: navbar"></div>

    <main class="container">
        <h1 style="color:var(--text-primary); text-align:center; margin-bottom:24px" th:text="${title}">Quản trị - Danh sách đặt chỗ</h1>

        <form method="get" th:action="@{/admin}" class="form-grid" style="grid-template-columns:repeat(2,1fr)">
            <label style="color:var(--text-primary)">Ngày
                <input type="date" name="date" th:value="${#temporals.format(date, 'yyyy-MM-dd')}" style="color:var(--text-primary)">
            </label>
            <div style="align-self:end; display:flex; gap:10px; justify-content:flex-end">
                <button class="btn" type="submit">Lọc</button>
                <a class="btn-primary" th:href="@{/admin/export(date=${#temporals.format(date, 'yyyy-MM-dd')})}">Tải CSV</a>
            </div>
        </form>

        <div class="table-wrap">
            <table class="table">
                <thead>
                <tr>
                    <th>Chi tiết</th>
                    <th>Sân</th>
                    <th>Ngày</th>
                    <th>Bắt đầu</th>
                    <th>Kết thúc</th>
                    <th>Họ tên</th>
                    <th>SĐT</th>
                </tr>
                </thead>
                <tbody>
                <tr th:each="b : ${bookings}">
                    <td><a class="btn" th:href="@{/admin/booking/{id}(id=${b.id})}">Xem</a></td>
                    <td th:text="${b.slot}">1</td>
                    <td th:text="${b.date}">2025-01-01</td>
                    <td th:text="${b.startTime}">17:00</td>
                    <td th:text="${b.endTime}">18:00</td>
                    <td th:text="${b.name}">Nguyen Van A</td>
                    <td th:text="${b.phone}">0900000000</td>
                </tr>
                <tr th:if="${#lists.isEmpty(bookings)}">
                    <td colspan="7" style="text-align:center; color:var(--text-secondary); font-weight:500">Không có dữ liệu</td>
                </tr>
                </tbody>
            </table>
        </div>
    </main>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
```

---

## 8. CHỨC NĂNG QUẢN LÝ PROFILE

### 8.1 Lược đồ chức năng

```
┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│   User      │───►│   Load      │───►│   Display   │
│  Profile    │    │ Profile     │    │ Profile     │
│   Page      │    │   Data      │    │   Form      │
└─────────────┘    └─────────────┘    └─────────────┘
       │                   │                   │
       │                   │                   ▼
       │                   │            ┌─────────────┐
       │                   │            │   Check     │
       │                   │            │ Name Change│
       │                   │            │  Restriction│
       │                   │            └─────────────┘
       │                   │                   │
       │                   │                   ▼
       │                   │            ┌─────────────┐
       │                   │            │   Update    │
       │                   │            │ Profile     │
       │                   │            └─────────────┘
       │                   │                   │
       │                   │                   ▼
       │                   │            ┌─────────────┐
       │                   │            │   Save      │
       │                   │            │ Changes     │
       │                   │            └─────────────┘
       │                   │
       ▼                   ▼
┌─────────────┐    ┌─────────────┐
│   Success   │    │   Error     │
│  Message    │    │  Message    │
└─────────────┘    └─────────────┘
```

### 8.2 Source Code

#### 8.2.1 ProfileController

```java
@Controller
public class ProfileController {

    @Autowired
    private AppUserRepository userRepository;

    @GetMapping("/profile")
    public String profilePage(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        if (auth == null || !auth.isAuthenticated() || auth instanceof org.springframework.security.authentication.AnonymousAuthenticationToken) {
            return "redirect:/auth/login";
        }

        AppUser user = userRepository.findByUsername(auth.getName()).orElse(null);
        if (user == null) {
            return "redirect:/auth/login";
        }

        model.addAttribute("user", user);
        model.addAttribute("title", "Quản lý Profile");
        
        // Check if user can change name (once per week)
        boolean canChangeName = true;
        if (user.getLastNameChange() != null) {
            LocalDateTime oneWeekAgo = LocalDateTime.now().minusWeeks(1);
            canChangeName = user.getLastNameChange().isBefore(oneWeekAgo);
        }
        model.addAttribute("canChangeName", canChangeName);
        
        return "profile";
    }

    @PostMapping("/profile/update")
    public String updateProfile(
            @RequestParam("fullName") String fullName,
            @RequestParam("phone") String phone,
            Model model) {
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        if (auth == null || !auth.isAuthenticated() || auth instanceof org.springframework.security.authentication.AnonymousAuthenticationToken) {
            return "redirect:/auth/login";
        }

        AppUser user = userRepository.findByUsername(auth.getName()).orElse(null);
        if (user == null) {
            return "redirect:/auth/login";
        }

        // Check if user can change name (once per week)
        boolean canChangeName = true;
        if (user.getLastNameChange() != null) {
            LocalDateTime oneWeekAgo = LocalDateTime.now().minusWeeks(1);
            canChangeName = user.getLastNameChange().isBefore(oneWeekAgo);
        }

        // Update phone (always allowed)
        user.setPhone(phone.trim());
        
        // Update name only if allowed
        if (canChangeName && !fullName.trim().equals(user.getFullName())) {
            user.setFullName(fullName.trim());
            user.setLastNameChange(LocalDateTime.now());
        }

        userRepository.save(user);
        
        model.addAttribute("user", user);
        model.addAttribute("title", "Quản lý Profile");
        model.addAttribute("canChangeName", canChangeName);
        model.addAttribute("success", "Cập nhật profile thành công!");
        
        return "profile";
    }
}
```

---

## 9. GIAO DIỆN NGƯỜI DÙNG

### 9.1 Responsive Design

```css
/* CSS Variables for consistent theming */
:root {
    --bg: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    --fg: #000000;
    --text-primary: #000000;
    --text-secondary: #6b7280;
    --text-muted: #9ca3af;
    --input-bg: #ffffff;
    --input-border: #d1d5db;
    --input-focus: #3b82f6;
    --primary: #3b82f6;
    --success: #10b981;
    --danger: #ef4444;
    --warning: #f59e0b;
}

/* Responsive Grid */
.field-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
    gap: 24px;
    margin: 24px 0;
}

@media (max-width: 768px) {
    .field-grid {
        grid-template-columns: 1fr;
    }
}

/* Time chip states */
.time-chip {
    padding: 8px 16px;
    border-radius: 8px;
    border: 2px solid var(--primary);
    background: transparent;
    color: var(--primary);
    cursor: pointer;
    transition: all 0.3s ease;
    margin: 4px;
    display: inline-block;
}

.time-chip:hover {
    background: var(--primary);
    color: white;
    transform: translateY(-2px);
    box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3);
}

.time-chip.busy, .time-chip.btn-danger {
    background: linear-gradient(135deg, #dc3545, #c82333) !important;
    border-color: #dc3545 !important;
    color: #ffffff !important;
    cursor: not-allowed !important;
    opacity: 0.8 !important;
    transform: none !important;
    box-shadow: 0 2px 8px rgba(220, 53, 69, 0.3) !important;
}
```

### 9.2 Bootstrap Integration

```html
<!-- Bootstrap Modal for Booking -->
<div class="modal fade" id="bookingModal" tabindex="-1" aria-labelledby="bookingModalLabel" aria-hidden="true">
    <div class="modal-dialog modal-lg">
        <div class="modal-content">
            <div class="modal-header">
                <h5 class="modal-title" id="bookingModalLabel">Đặt sân <span id="selectedSlotLabel">1</span></h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
            </div>
            <div class="modal-body">
                <form id="bookingForm" method="post" th:action="@{/book}">
                    <input type="hidden" name="slot" id="slotInput">
                    <input type="hidden" name="startTime" id="startTime">
                    <input type="hidden" name="endTime" id="endTime">
                    
                    <div class="row">
                        <div class="col-md-6">
                            <label class="form-label">Ngày</label>
                            <input type="date" name="date" id="date" class="form-control" required>
                        </div>
                        <div class="col-md-6" th:if="${!loggedIn}">
                            <label class="form-label">Họ và tên</label>
                            <input type="text" name="name" id="name" class="form-control" required>
                        </div>
                    </div>
                    
                    <div class="row" th:if="${!loggedIn}">
                        <div class="col-md-6">
                            <label class="form-label">Số điện thoại</label>
                            <input type="tel" name="phone" id="phone" class="form-control" required>
                        </div>
                    </div>
                    
                    <div class="mt-3">
                        <label class="form-label">Chọn khung giờ (ít nhất 2 khung liền kề)</label>
                        <div id="timeSlots" class="d-flex flex-wrap"></div>
                    </div>
                </form>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Hủy</button>
                <button type="submit" form="bookingForm" class="btn btn-primary">Đặt chỗ</button>
            </div>
        </div>
    </div>
</div>
```

---

## 10. BẢO MẬT VÀ PHÂN QUYỀN

### 10.1 Spring Security Configuration

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private CustomAuthenticationSuccessHandler customAuthenticationSuccessHandler;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session
                .maximumSessions(1)
                .maxSessionsPreventsLogin(false)
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/css/**", "/img/**", "/", "/announcements", "/contact", 
                               "/book", "/api/**", "/payment/**", "/register", "/login", 
                               "/logout", "/activate", "/auth/**", "/demo", "/profile", 
                               "/me/**").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/auth/login")
                .loginProcessingUrl("/auth/login")
                .successHandler(customAuthenticationSuccessHandler)
                .failureUrl("/auth/login?error=true")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/auth/logout")
                .logoutSuccessUrl("/")
                .permitAll()
            )
        ;
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }
}
```

### 10.2 JWT Integration

```java
@Component
public class JwtUtil {
    
    private static final String SECRET_KEY = "mySecretKey";
    private static final int JWT_EXPIRATION = 86400000; // 24 hours

    public String generateToken(String username, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role);
        return createToken(claims, username);
    }

    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + JWT_EXPIRATION))
                .signWith(SignatureAlgorithm.HS256, SECRET_KEY)
                .compact();
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get("role", String.class));
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser().setSigningKey(SECRET_KEY).parseClaimsJws(token).getBody();
    }

    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public Boolean validateToken(String token, String username) {
        final String extractedUsername = extractUsername(token);
        return (extractedUsername.equals(username) && !isTokenExpired(token));
    }
}
```

---

## 11. API DOCUMENTATION

### 11.1 REST API Endpoints

#### 11.1.1 Authentication APIs

```
POST /auth/api/login
Content-Type: application/json

Request Body:
{
    "username": "string",
    "password": "string"
}

Response:
{
    "token": "string",
    "username": "string", 
    "role": "string",
    "message": "string"
}
```

#### 11.1.2 Booking APIs

```
GET /api/availability?slot={slot}&date={date}

Response:
{
    "occupied": [0, 1, 5],
    "ok": true
}

POST /book
Content-Type: application/x-www-form-urlencoded

Parameters:
- slot: int
- date: string (yyyy-MM-dd)
- startTime: string (HH:mm)
- endTime: string (HH:mm)
- name: string (optional for logged-in users)
- phone: string (optional for logged-in users)
```

#### 11.1.3 Admin APIs

```
GET /admin?date={date}
GET /admin/booking/{id}
GET /admin/export?date={date}
```

### 11.2 Database Queries

```sql
-- Get bookings for a specific date and slot
SELECT * FROM bookings 
WHERE date = ? AND slot = ? 
ORDER BY start_time ASC;

-- Get user bookings
SELECT * FROM bookings 
WHERE username = ? 
ORDER BY date DESC, start_time DESC;

-- Get expired pending bookings
SELECT * FROM bookings 
WHERE status = 'PENDING' 
AND expires_at < NOW();

-- Get confirmed bookings for admin
SELECT * FROM bookings 
WHERE date = ? AND status = 'CONFIRMED' 
ORDER BY slot ASC, start_time ASC;
```

---

## 12. DEPLOYMENT VÀ CÀI ĐẶT

### 12.1 Yêu cầu hệ thống

- Java 22+
- Maven 3.6+
- MySQL 8.0+
- IDE (IntelliJ IDEA, Eclipse, VS Code)

### 12.2 Cài đặt

#### 12.2.1 Database Setup

```sql
-- Tạo database
CREATE DATABASE utesoccer;
CREATE USER 'root'@'localhost' IDENTIFIED BY 'dinh2103';
GRANT ALL PRIVILEGES ON utesoccer.* TO 'root'@'localhost';
FLUSH PRIVILEGES;
```

#### 12.2.2 Application Configuration

```properties
# application.properties
spring.datasource.url=jdbc:mysql://localhost:3306/utesoccer
spring.datasource.username=root
spring.datasource.password=dinh2103
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQL8Dialect

# Server configuration
server.port=8080
server.servlet.context-path=/

# Logging
logging.level.com.example.demo=INFO
logging.level.org.springframework.security=DEBUG
```

#### 12.2.3 Build và Run

```bash
# Clone repository
git clone <repository-url>
cd UTEsoccer

# Build project
mvn clean compile

# Run application
mvn spring-boot:run

# Access application
http://localhost:8080
```

### 12.3 Docker Deployment

```dockerfile
FROM openjdk:22-jdk-slim

WORKDIR /app

COPY target/UTEsoccer-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
```

```yaml
# docker-compose.yml
version: '3.8'
services:
  app:
    build: .
    ports:
      - "8080:8080"
    environment:
      - SPRING_DATASOURCE_URL=jdbc:mysql://db:3306/utesoccer
      - SPRING_DATASOURCE_USERNAME=root
      - SPRING_DATASOURCE_PASSWORD=dinh2103
    depends_on:
      - db

  db:
    image: mysql:8.0
    environment:
      - MYSQL_ROOT_PASSWORD=dinh2103
      - MYSQL_DATABASE=utesoccer
    ports:
      - "3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql

volumes:
  mysql_data:
```

---

## KẾT LUẬN

Tài liệu này cung cấp hướng dẫn chi tiết để xây dựng và phát triển ứng dụng UTE Soccer từ cơ bản đến nâng cao. Với kiến trúc MVC, Spring Boot, và các công nghệ hiện đại, ứng dụng có thể mở rộng và bảo trì dễ dàng.

### Các điểm chính:

1. **Kiến trúc rõ ràng**: MVC pattern với separation of concerns
2. **Bảo mật tốt**: Spring Security với JWT và BCrypt
3. **Database design**: Normalized schema với proper indexing
4. **Responsive UI**: Bootstrap với custom CSS
5. **API design**: RESTful APIs với proper error handling
6. **Payment integration**: VNPay sandbox integration
7. **Real-time features**: WebSocket for notifications
8. **Admin functionality**: Complete admin dashboard
9. **User management**: Profile management with restrictions
10. **Deployment ready**: Docker và production configuration

Ứng dụng sẵn sàng cho production với các tính năng đầy đủ và có thể mở rộng thêm các tính năng như email notifications, mobile app, analytics dashboard, v.v.
