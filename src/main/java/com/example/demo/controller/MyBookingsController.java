package com.example.demo.controller;

import com.example.demo.model.Booking;
import com.example.demo.repository.BookingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class MyBookingsController {

    @Autowired
    private BookingRepository bookingRepository;

    @GetMapping("/me/bookings")
    public String myBookings(Model model){
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof org.springframework.security.authentication.AnonymousAuthenticationToken){
            return "redirect:/login";
        }
        String username = auth.getName();
        List<Booking> list = bookingRepository.findByUsernameOrderByDateDescStartTimeDesc(username);
        model.addAttribute("title", "Đặt chỗ của tôi");
        model.addAttribute("bookings", list);
        return "my_bookings";
    }
}


