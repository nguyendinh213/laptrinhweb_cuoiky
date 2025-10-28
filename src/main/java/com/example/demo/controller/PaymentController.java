package com.example.demo.controller;

import com.example.demo.model.Booking;
import com.example.demo.repository.BookingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.LocalDateTime;

@Controller
public class PaymentController {

    @Autowired
    private BookingRepository bookingRepository;

    // VNPay return URL handler
    @GetMapping("/payment/return")
    public String paymentReturn(@RequestParam(value = "vnp_TxnRef", required = false) String code,
                                @RequestParam(value = "vnp_ResponseCode", required = false) String respCode,
                                Model model){
        model.addAttribute("title", "Kết quả thanh toán");
        if (code == null){
            model.addAttribute("message", "Thiếu mã giao dịch.");
            return "index";
        }
        Booking b = bookingRepository.findByPaymentCode(code);
        if (b == null){
            model.addAttribute("message", "Không tìm thấy đơn đặt chỗ.");
            return "index";
        }
        if ("00".equals(respCode)){
            // success
            b.setStatus(Booking.Status.CONFIRMED);
            b.setPaidAt(LocalDateTime.now());
            bookingRepository.save(b);
            model.addAttribute("message", "Thanh toán thành công. Đặt chỗ đã được xác nhận.");
        } else {
            model.addAttribute("message", "Thanh toán thất bại hoặc bị hủy.");
        }
        return "index";
    }

    @GetMapping("/api/payment/status")
    @ResponseBody
    public java.util.Map<String,Object> paymentStatus(@RequestParam("code") String code){
        java.util.Map<String,Object> resp = new java.util.HashMap<>();
        Booking b = bookingRepository.findByPaymentCode(code);
        if (b == null){
            resp.put("ok", false);
            resp.put("error", "not_found");
            return resp;
        }
        resp.put("ok", true);
        resp.put("status", b.getStatus().name());
        resp.put("expiresAt", b.getExpiresAt());
        return resp;
    }
}


