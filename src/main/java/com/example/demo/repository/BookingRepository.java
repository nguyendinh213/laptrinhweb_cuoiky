package com.example.demo.repository;

import com.example.demo.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    @Query("SELECT b FROM Booking b WHERE b.slot = :slot AND b.date = :date AND b.startTime < :end AND b.endTime > :start AND b.status <> 'CANCELLED'")
    List<Booking> findOverlapping(@Param("slot") int slot,
                                  @Param("date") LocalDate date,
                                  @Param("start") LocalTime start,
                                  @Param("end") LocalTime end);

    List<Booking> findByDateOrderBySlotAscStartTimeAsc(LocalDate date);

    List<Booking> findByDateAndSlotOrderByStartTimeAsc(LocalDate date, int slot);

    boolean existsByDateAndSlot(LocalDate date, int slot);

    Booking findByPaymentCode(String paymentCode);

    @Query("SELECT b FROM Booking b WHERE b.status='PENDING' AND b.expiresAt < CURRENT_TIMESTAMP")
    List<Booking> findExpiredPendings();

    List<Booking> findByUsernameOrderByDateDescStartTimeDesc(String username);

}


