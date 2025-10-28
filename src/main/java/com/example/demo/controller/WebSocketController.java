package com.example.demo.controller;

import com.example.demo.dto.NotificationMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class WebSocketController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/admin/notifications")
    @SendTo("/topic/admin")
    public NotificationMessage handleAdminNotification(NotificationMessage message) {
        return message;
    }

    @MessageMapping("/user/notifications")
    @SendTo("/queue/user")
    public NotificationMessage handleUserNotification(NotificationMessage message) {
        return message;
    }
}
