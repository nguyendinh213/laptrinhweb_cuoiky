package com.example.demo.repository;

import com.example.demo.model.FieldImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FieldImageRepository extends JpaRepository<FieldImage, Long> {
    List<FieldImage> findByFieldSlotAndIsActiveTrue(Integer fieldSlot);
    List<FieldImage> findByImageTypeAndIsActiveTrue(String imageType);
    List<FieldImage> findByIsActiveTrueOrderByUploadedAtDesc();
}
