package com.example.demo.service;

import com.example.demo.model.Booking;
import com.example.demo.repository.BookingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BookingCleanupJob {

    @Autowired
    private BookingRepository bookingRepository;

    // Every 30 seconds, cancel expired pending bookings
    @Scheduled(fixedDelay = 30000)
    public void cancelExpired() {
        List<Booking> expired = bookingRepository.findExpiredPendings();
        for (Booking b : expired) {
            b.setStatus(Booking.Status.CANCELLED);
            bookingRepository.save(b);
        }
    }
}


