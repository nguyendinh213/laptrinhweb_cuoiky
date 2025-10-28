package com.example.demo.service;

import com.example.demo.dto.NotificationMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    public void notifyNewBooking(String username, Object bookingData) {
        NotificationMessage message = new NotificationMessage(
            "NEW_BOOKING", 
            "Có đặt sân mới từ " + username, 
            bookingData
        );
        messagingTemplate.convertAndSend("/topic/admin", message);
    }

    public void notifyPaymentStatus(String username, String status, Object paymentData) {
        NotificationMessage message = new NotificationMessage(
            "PAYMENT_STATUS", 
            "Trạng thái thanh toán: " + status, 
            paymentData
        );
        messagingTemplate.convertAndSendToUser(username, "/queue/payment", message);
    }

    public void notifySlotAvailable(String slot, String date, String time) {
        NotificationMessage message = new NotificationMessage(
            "SLOT_AVAILABLE", 
            "Sân " + slot + " ngày " + date + " lúc " + time + " đã có sẵn", 
            null
        );
        messagingTemplate.convertAndSend("/topic/availability", message);
    }

    public void notifyBookingExpired(String username, Object bookingData) {
        NotificationMessage message = new NotificationMessage(
            "BOOKING_EXPIRED", 
            "Đặt sân của bạn đã hết hạn", 
            bookingData
        );
        messagingTemplate.convertAndSendToUser(username, "/queue/booking", message);
    }
}
