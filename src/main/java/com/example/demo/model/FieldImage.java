package com.example.demo.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "field_images")
public class FieldImage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "field_slot")
    private Integer fieldSlot;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "cloudinary_public_id")
    private String cloudinaryPublicId;

    @Column(name = "image_type")
    private String imageType; // FIELD_IMAGE, BANNER, PROMOTIONAL

    @Column(name = "uploaded_at")
    private LocalDateTime uploadedAt;

    @Column(name = "uploaded_by")
    private String uploadedBy;

    @Column(name = "is_active")
    private Boolean isActive = true;

    // Constructors
    public FieldImage() {}

    public FieldImage(Integer fieldSlot, String imageUrl, String cloudinaryPublicId, String imageType, String uploadedBy) {
        this.fieldSlot = fieldSlot;
        this.imageUrl = imageUrl;
        this.cloudinaryPublicId = cloudinaryPublicId;
        this.imageType = imageType;
        this.uploadedBy = uploadedBy;
        this.uploadedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getFieldSlot() {
        return fieldSlot;
    }

    public void setFieldSlot(Integer fieldSlot) {
        this.fieldSlot = fieldSlot;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getCloudinaryPublicId() {
        return cloudinaryPublicId;
    }

    public void setCloudinaryPublicId(String cloudinaryPublicId) {
        this.cloudinaryPublicId = cloudinaryPublicId;
    }

    public String getImageType() {
        return imageType;
    }

    public void setImageType(String imageType) {
        this.imageType = imageType;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    public String getUploadedBy() {
        return uploadedBy;
    }

    public void setUploadedBy(String uploadedBy) {
        this.uploadedBy = uploadedBy;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
}
