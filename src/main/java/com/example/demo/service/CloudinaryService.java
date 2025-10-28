package com.example.demo.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.example.demo.model.FieldImage;
import com.example.demo.repository.FieldImageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Service
public class CloudinaryService {

    @Autowired
    private Cloudinary cloudinary;

    @Autowired
    private FieldImageRepository fieldImageRepository;

    public String uploadImage(MultipartFile file, String folder) throws IOException {
        Map<String, Object> params = ObjectUtils.asMap(
            "folder", folder,
            "resource_type", "auto"
        );
        
        Map<?, ?> uploadResult = cloudinary.uploader().upload(file.getBytes(), params);
        return (String) uploadResult.get("secure_url");
    }

    public String uploadImage(MultipartFile file, String folder, String publicId) throws IOException {
        Map<String, Object> params = ObjectUtils.asMap(
            "folder", folder,
            "public_id", publicId,
            "resource_type", "auto"
        );
        
        Map<?, ?> uploadResult = cloudinary.uploader().upload(file.getBytes(), params);
        return (String) uploadResult.get("secure_url");
    }

    public void deleteImage(String publicId) throws IOException {
        cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
    }

    public FieldImage saveFieldImage(Integer fieldSlot, MultipartFile file, String imageType, String uploadedBy) {
        try {
            String folder = "utesoccer/" + imageType.toLowerCase();
            String publicId = imageType.toLowerCase() + "_" + fieldSlot + "_" + System.currentTimeMillis();
            
            String imageUrl = uploadImage(file, folder, publicId);
            
            FieldImage fieldImage = new FieldImage(fieldSlot, imageUrl, publicId, imageType, uploadedBy);
            return fieldImageRepository.save(fieldImage);
            
        } catch (IOException e) {
            throw new RuntimeException("Lỗi upload hình ảnh: " + e.getMessage());
        }
    }

    public List<FieldImage> getFieldImages(Integer fieldSlot) {
        return fieldImageRepository.findByFieldSlotAndIsActiveTrue(fieldSlot);
    }

    public List<FieldImage> getImagesByType(String imageType) {
        return fieldImageRepository.findByImageTypeAndIsActiveTrue(imageType);
    }

    public List<FieldImage> getAllActiveImages() {
        return fieldImageRepository.findByIsActiveTrueOrderByUploadedAtDesc();
    }

    public void deleteFieldImage(Long imageId) {
        FieldImage image = fieldImageRepository.findById(imageId).orElse(null);
        if (image != null) {
            try {
                deleteImage(image.getCloudinaryPublicId());
                image.setIsActive(false);
                fieldImageRepository.save(image);
            } catch (IOException e) {
                throw new RuntimeException("Lỗi xóa hình ảnh: " + e.getMessage());
            }
        }
    }
}
