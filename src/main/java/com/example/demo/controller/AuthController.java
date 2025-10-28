package com.example.demo.controller;

import com.example.demo.dto.AuthResponse;
import com.example.demo.dto.LoginRequest;
import com.example.demo.model.AppUser;
import com.example.demo.repository.AppUserRepository;
import com.example.demo.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
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

            String token = jwtUtil.generateToken(user.getUsername(), user.getRole());
            
            return ResponseEntity.ok(new AuthResponse(token, user.getUsername(), user.getRole(), "Đăng nhập thành công"));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new AuthResponse(null, null, null, "Lỗi đăng nhập: " + e.getMessage()));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody AppUser user) {
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
            String token = jwtUtil.generateToken(savedUser.getUsername(), savedUser.getRole());
            
            return ResponseEntity.ok(new AuthResponse(token, savedUser.getUsername(), savedUser.getRole(), "Đăng ký thành công"));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new AuthResponse(null, null, null, "Lỗi đăng ký: " + e.getMessage()));
        }
    }

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