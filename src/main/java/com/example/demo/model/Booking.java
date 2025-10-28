package com.example.demo.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "bookings", indexes = {
        @Index(name = "idx_bookings_date_slot", columnList = "date,slot")
})
public class Booking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private int slot; // 1..6

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private LocalTime startTime; // inclusive

    @Column(nullable = false)
    private LocalTime endTime; // exclusive

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.PENDING; // PENDING, CONFIRMED, CANCELLED

    @Column(length = 64, unique = true)
    private String paymentCode; // show to user, used to track payment

    private LocalDateTime expiresAt; // pending hold expiry

    private Long amount; // in VND

    private LocalDateTime paidAt;

    public enum Status { PENDING, CONFIRMED, CANCELLED }

    @Column(length = 100)
    private String username; // owner username for user bookings

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public int getSlot() { return slot; }
    public void setSlot(int slot) { this.slot = slot; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }

    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public String getPaymentCode() { return paymentCode; }
    public void setPaymentCode(String paymentCode) { this.paymentCode = paymentCode; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

    public Long getAmount() { return amount; }
    public void setAmount(Long amount) { this.amount = amount; }

    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
}


