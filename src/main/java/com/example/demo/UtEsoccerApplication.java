package com.example.demo;

import com.example.demo.model.AppUser;
import com.example.demo.repository.AppUserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication
public class UtEsoccerApplication {

    public static void main(String[] args) {
        SpringApplication.run(UtEsoccerApplication.class, args);
    }

    @Bean
    CommandLineRunner seedAdmin(AppUserRepository repo, PasswordEncoder encoder){
        return args -> {
            if (repo.findByUsername("admin") == null){
                AppUser admin = new AppUser();
                admin.setUsername("admin");
                admin.setPasswordHash(encoder.encode("admin123"));
                admin.setRole("ROLE_ADMIN");
                admin.setEnabled(true);
                admin.setFullName("Quản trị viên");
                admin.setPhone("0900000000");
                repo.save(admin);
            }
            if (repo.findByUsername("user") == null){
                AppUser user = new AppUser();
                user.setUsername("user");
                user.setPasswordHash(encoder.encode("user123"));
                user.setRole("ROLE_USER");
                user.setEnabled(true); // enabled sẵn để demo
                user.setFullName("Khách hàng demo");
                user.setPhone("0911111111");
                repo.save(user);
            }
        };
    }

}
