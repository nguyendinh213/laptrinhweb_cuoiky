package com.example.demo.model;

import jakarta.persistence.*;

@Entity
@Table(name = "app_users", indexes = {
        @Index(name = "idx_app_users_username", columnList = "username", unique = true)
})
public class AppUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String username;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private String role; // ROLE_ADMIN or ROLE_USER

    @Column(nullable = false)
    private boolean enabled = false;

    @Column(length = 64)
    private String activationToken;

    @Column(length = 120)
    private String fullName;

    @Column(length = 20)
    private String phone;

    @Column(name = "last_name_change")
    private java.time.LocalDateTime lastNameChange;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getActivationToken() { return activationToken; }
    public void setActivationToken(String activationToken) { this.activationToken = activationToken; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public java.time.LocalDateTime getLastNameChange() { return lastNameChange; }
    public void setLastNameChange(java.time.LocalDateTime lastNameChange) { this.lastNameChange = lastNameChange; }
}


