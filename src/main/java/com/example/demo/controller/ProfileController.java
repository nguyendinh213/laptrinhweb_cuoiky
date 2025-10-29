package com.example.demo.controller;

import com.example.demo.model.AppUser;
import com.example.demo.repository.AppUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;

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
