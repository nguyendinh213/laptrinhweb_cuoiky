package com.example.demo.controller;

import com.example.demo.model.Booking;
import com.example.demo.repository.BookingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class AdminController {

    @Autowired
    private BookingRepository bookingRepository;

    @GetMapping("/admin")
    public String admin(
            @RequestParam(value = "date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Model model
    ) {
        model.addAttribute("title", "Quản trị - Danh sách đặt chỗ");
        LocalDate effectiveDate = (date != null) ? date : LocalDate.now();
        model.addAttribute("date", effectiveDate);

        List<Booking> bookingsRaw = bookingRepository.findByDateOrderBySlotAscStartTimeAsc(effectiveDate);
        java.util.Map<String, Booking> unique = new java.util.LinkedHashMap<>();
        for (Booking b : bookingsRaw) {
            if (b.getStatus() != com.example.demo.model.Booking.Status.CONFIRMED) continue;
            String key = b.getSlot()+"|"+b.getDate()+"|"+b.getStartTime()+"|"+b.getEndTime();
            // keep the earliest id (or first encountered) for each unique time slot to avoid duplicates that differ only by id/name/phone
            unique.putIfAbsent(key, b);
        }
        List<Booking> bookings = new java.util.ArrayList<>(unique.values());

        model.addAttribute("bookings", bookings);
        return "admin";
    }

    @GetMapping(value = "/admin/export", produces = "text/csv")
    public ResponseEntity<byte[]> exportCsv(
            @RequestParam(value = "date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        List<Booking> bookingsRaw = bookingRepository.findByDateOrderBySlotAscStartTimeAsc(date);
        java.util.Map<String, Booking> unique = new java.util.LinkedHashMap<>();
        for (Booking b : bookingsRaw) {
            if (b.getStatus() != com.example.demo.model.Booking.Status.CONFIRMED) continue;
            String key = b.getSlot()+"|"+b.getDate()+"|"+b.getStartTime()+"|"+b.getEndTime();
            unique.putIfAbsent(key, b);
        }
        List<Booking> bookings = new java.util.ArrayList<>(unique.values());

        String header = "id,slot,date,startTime,endTime,name,phone\n";
        String body = bookings.stream().map(b -> String.join(
                ",",
                String.valueOf(b.getId()),
                String.valueOf(b.getSlot()),
                b.getDate().toString(),
                b.getStartTime().toString(),
                b.getEndTime().toString(),
                escapeCsv(b.getName()),
                escapeCsv(b.getPhone())
        )).collect(Collectors.joining("\n"));
        String csv = header + body + (body.isEmpty() ? "" : "\n");

        byte[] bytes = csv.getBytes(StandardCharsets.UTF_8);
        String filename = "bookings-" + date + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.TEXT_PLAIN)
                .body(bytes);
    }

    @GetMapping("/admin/booking/{id}")
    public String bookingDetail(@PathVariable("id") Long id, Model model) {
        Booking b = bookingRepository.findById(id).orElse(null);
        if (b == null) {
            model.addAttribute("title", "Chi tiết đặt chỗ");
            model.addAttribute("error", "Không tìm thấy đặt chỗ");
            return "admin_booking_detail";
        }
        model.addAttribute("title", "Chi tiết đặt chỗ");
        model.addAttribute("b", b);
        return "admin_booking_detail";
    }

    @GetMapping("/admin/booking")
    public String bookingDetailQuery(@RequestParam("id") Long id, Model model){
        return bookingDetail(id, model);
    }

    

    private String escapeCsv(String value){
        if (value == null) return "";
        boolean needQuote = value.contains(",") || value.contains("\"") || value.contains("\n");
        String escaped = value.replace("\"", "\"\"");
        return needQuote ? ("\"" + escaped + "\"") : escaped;
    }
}


