package com.example.demo.controller;

import com.example.demo.model.Booking;
import com.example.demo.model.AppUser;
import com.example.demo.repository.AppUserRepository;
import com.example.demo.repository.BookingRepository;
import com.example.demo.service.PaymentService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

@Controller
public class HomeController {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private AppUserRepository appUserRepository;

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("title", "Đặt chỗ theo giờ - 6 chỗ");
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        boolean loggedIn = auth != null && auth.isAuthenticated() && !(auth instanceof org.springframework.security.authentication.AnonymousAuthenticationToken);
        model.addAttribute("loggedIn", loggedIn);
        return "index";
    }

    @GetMapping("/api/availability")
    @ResponseBody
    public java.util.Map<String, Object> availability(
            @RequestParam("slot") int slot,
            @RequestParam("date") String date
    ) {
        java.util.Map<String, Object> resp = new java.util.HashMap<>();
        try {
            LocalDate d = LocalDate.parse(date);
            java.util.List<com.example.demo.model.Booking> list = bookingRepository.findByDateAndSlotOrderByStartTimeAsc(d, slot);
            java.util.Set<Integer> occupied = new java.util.HashSet<>();
            int startMinutes = 17 * 60; // 17:00
            for (com.example.demo.model.Booking b : list) {
                if (b.getStatus() == Booking.Status.CANCELLED) continue;
                if (b.getStatus() == Booking.Status.PENDING && b.getExpiresAt() != null && b.getExpiresAt().isBefore(java.time.LocalDateTime.now())) continue;
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

    @GetMapping("/announcements")
    public String announcements(Model model) {
        model.addAttribute("title", "Thông báo");
        return "announcements";
    }

    @GetMapping("/contact")
    public String contact(Model model) {
        model.addAttribute("title", "Liên hệ");
        return "contact";
    }

    @PostMapping("/book")
    public String book(
            @RequestParam("slot") int slot,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "phone", required = false) String phone,
            @RequestParam("date") String date,
            @RequestParam("startTime") String startTime,
            @RequestParam("endTime") String endTime,
            Model model
    ) {
        model.addAttribute("title", "Đặt chỗ theo giờ - 6 chỗ");

        org.springframework.security.core.Authentication authC = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        boolean isLoggedIn = authC != null && authC.isAuthenticated() && !(authC instanceof org.springframework.security.authentication.AnonymousAuthenticationToken);

        // Fill name/phone from profile if missing and logged in
        if (isLoggedIn && (name == null || name.isBlank() || phone == null || phone.isBlank())) {
            AppUser user = appUserRepository.findByUsername(authC.getName());
            if (user != null) {
                if (name == null || name.isBlank()) name = user.getFullName();
                if (phone == null || phone.isBlank()) phone = user.getPhone();
            }
        }

        // Basic validations
        if (slot < 1 || slot > 6 || date == null || date.isBlank() || name == null || name.isBlank() || phone == null || phone.isBlank()) {
            model.addAttribute("message", "Dữ liệu không hợp lệ. Vui lòng kiểm tra lại.");
            model.addAttribute("loggedIn", isLoggedIn);
            return "index";
        }
        try {
            LocalDate chosen = LocalDate.parse(date);
            if (chosen.isBefore(LocalDate.now())) {
                model.addAttribute("message", "Ngày không được trong quá khứ.");
                model.addAttribute("loggedIn", isLoggedIn);
                return "index";
            }
        } catch (DateTimeParseException ex) {
            model.addAttribute("message", "Định dạng ngày không hợp lệ.");
            model.addAttribute("loggedIn", isLoggedIn);
            return "index";
        }

        // Validate start/end time (HH:mm)
        if (startTime == null || endTime == null || startTime.isBlank() || endTime.isBlank()) {
            model.addAttribute("message", "Vui lòng chọn thời gian hợp lệ.");
            model.addAttribute("loggedIn", isLoggedIn);
            return "index";
        }

        LocalTime start;
        LocalTime end;
        try {
            start = LocalTime.parse(startTime);
            end = LocalTime.parse(endTime);
        } catch (Exception ex) {
            model.addAttribute("message", "Định dạng thời gian không hợp lệ.");
            model.addAttribute("loggedIn", isLoggedIn);
            return "index";
        }
        if (!end.isAfter(start)){
            model.addAttribute("message", "Thời gian kết thúc phải sau thời gian bắt đầu.");
            model.addAttribute("loggedIn", isLoggedIn);
            return "index";
        }

        // Check overlap
        List<Booking> overlaps = bookingRepository.findOverlapping(slot, LocalDate.parse(date), start, end);
        // Exclude pending that are already expired (cleanup might be slightly delayed)
        overlaps.removeIf(b -> b.getStatus() == Booking.Status.PENDING && b.getExpiresAt() != null && b.getExpiresAt().isBefore(java.time.LocalDateTime.now()));
        if (!overlaps.isEmpty()){
            model.addAttribute("message", "Khung giờ đã được đặt. Vui lòng chọn khung khác.");
            model.addAttribute("loggedIn", isLoggedIn);
            return "index";
        }

        // Create PENDING booking with 3-minute hold
        Booking b = new Booking();
        b.setSlot(slot);
        b.setDate(LocalDate.parse(date));
        b.setStartTime(start);
        b.setEndTime(end);
        b.setName(name);
        b.setPhone(phone);
        // attach username if logged in
        if (isLoggedIn){
            b.setUsername(authC.getName());
        }
        b.setStatus(Booking.Status.PENDING);
        b.setAmount(100000L); // ví dụ: 100,000 VND, có thể tính theo thời lượng
        b.setExpiresAt(java.time.LocalDateTime.now().plusMinutes(3));
        b.setPaymentCode(java.util.UUID.randomUUID().toString().replace("-",""));
        bookingRepository.save(b);

        String payUrl = paymentService.buildVNPayUrl(b.getPaymentCode(), b.getAmount(), "Thanh toan dat cho slot " + slot);
        model.addAttribute("paymentCode", b.getPaymentCode());
        model.addAttribute("paymentUrl", payUrl);
        model.addAttribute("expiresAt", b.getExpiresAt());
        return "payment";
    }
}


