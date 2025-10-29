package com.example.demo.controller;

import com.example.demo.dto.AuthResponse;
import com.example.demo.dto.LoginRequest;
import com.example.demo.model.AppUser;
import com.example.demo.repository.AppUserRepository;
import com.example.demo.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Controller
@RequestMapping("/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    // Serve login page
    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    // Serve register page
    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

    // Handle register form submission
    @PostMapping("/register")
    public String handleRegister(@RequestParam String username, @RequestParam String password,
                                @RequestParam String fullName, @RequestParam String phone,
                                org.springframework.ui.Model model) {
        try {
            if (userRepository.findByUsername(username).isPresent()) {
                model.addAttribute("error", "Tên đăng nhập đã tồn tại");
                return "register";
            }

            AppUser user = new AppUser();
            user.setUsername(username);
            user.setPasswordHash(passwordEncoder.encode(password));
            user.setFullName(fullName);
            user.setPhone(phone);
            user.setRole("ROLE_USER");
            user.setEnabled(true); // For demo purposes
            user.setActivationToken(null);

            userRepository.save(user);
            model.addAttribute("message", "Đăng ký thành công! Bạn có thể đăng nhập ngay.");
            return "register";
            
        } catch (Exception e) {
            model.addAttribute("error", "Lỗi đăng ký: " + e.getMessage());
            return "register";
        }
    }

    // Handle logout
    @PostMapping("/logout")
    public String handleLogout(jakarta.servlet.http.HttpServletRequest request) {
        request.getSession().invalidate();
        return "redirect:/";
    }

    // REST API for login with JWT integration
    @PostMapping("/api/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest, jakarta.servlet.http.HttpServletRequest request) {
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

            // Generate JWT token and store in session for hybrid approach
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

    // REST API for register with JWT integration
    @PostMapping("/api/register")
    public ResponseEntity<?> register(@RequestBody AppUser user, jakarta.servlet.http.HttpServletRequest request) {
        try {
            if (userRepository.findByUsername(user.getUsername()).isPresent()) {
                return ResponseEntity.badRequest()
                    .body(new AuthResponse(null, null, null, "Tên đăng nhập đã tồn tại"));
            }

            user.setPasswordHash(passwordEncoder.encode(user.getPasswordHash()));
            user.setRole("ROLE_USER");
            user.setEnabled(true); // For demo purposes
            user.setActivationToken(null);

            AppUser savedUser = userRepository.save(user);
            
            // Generate JWT token and store in session for hybrid approach
            String token = jwtUtil.generateToken(savedUser.getUsername(), savedUser.getRole());
            request.getSession().setAttribute("jwt_token", token);
            request.getSession().setAttribute("username", savedUser.getUsername());
            request.getSession().setAttribute("role", savedUser.getRole());
            
            return ResponseEntity.ok(new AuthResponse(token, savedUser.getUsername(), savedUser.getRole(), "Đăng ký thành công"));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new AuthResponse(null, null, null, "Lỗi đăng ký: " + e.getMessage()));
        }
    }

    // JWT token validation endpoint
    @GetMapping("/validate")
    public ResponseEntity<?> validateToken(@RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                String username = jwtUtil.extractUsername(token);
                String role = jwtUtil.extractRole(token);
                
                if (jwtUtil.validateToken(token, username)) {
                    return ResponseEntity.ok(new AuthResponse(token, username, role, "Token hợp lệ"));
                }
            }
            return ResponseEntity.badRequest()
                .body(new AuthResponse(null, null, null, "Token không hợp lệ"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new AuthResponse(null, null, null, "Lỗi xác thực token"));
        }
    }
}