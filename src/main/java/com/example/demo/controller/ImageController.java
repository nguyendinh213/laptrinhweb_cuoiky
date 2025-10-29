/*
package com.example.demo.controller;

import com.example.demo.model.FieldImage;
import com.example.demo.service.CloudinaryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/images")
@CrossOrigin(origins = "*")
public class ImageController {

    @Autowired
    private CloudinaryService cloudinaryService;

    @GetMapping("/field/{fieldSlot}")
    public ResponseEntity<List<FieldImage>> getFieldImages(@PathVariable Integer fieldSlot) {
        List<FieldImage> images = cloudinaryService.getFieldImages(fieldSlot);
        return ResponseEntity.ok(images);
    }

    @GetMapping("/type/{imageType}")
    public ResponseEntity<List<FieldImage>> getImagesByType(@PathVariable String imageType) {
        List<FieldImage> images = cloudinaryService.getImagesByType(imageType);
        return ResponseEntity.ok(images);
    }

    @GetMapping("/all")
    public ResponseEntity<List<FieldImage>> getAllImages() {
        List<FieldImage> images = cloudinaryService.getAllActiveImages();
        return ResponseEntity.ok(images);
    }

    @PostMapping("/upload")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam("fieldSlot") Integer fieldSlot,
            @RequestParam("imageType") String imageType,
            @RequestParam("uploadedBy") String uploadedBy) {
        try {
            FieldImage fieldImage = cloudinaryService.saveFieldImage(fieldSlot, file, imageType, uploadedBy);
            return ResponseEntity.ok(fieldImage);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi upload: " + e.getMessage());
        }
    }

    @DeleteMapping("/{imageId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteImage(@PathVariable Long imageId) {
        try {
            cloudinaryService.deleteFieldImage(imageId);
            return ResponseEntity.ok("Xóa hình ảnh thành công");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi xóa: " + e.getMessage());
        }
    }
}
*/
