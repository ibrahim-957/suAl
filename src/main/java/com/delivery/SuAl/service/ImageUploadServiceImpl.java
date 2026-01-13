package com.delivery.SuAl.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.delivery.SuAl.exception.ImageUploadException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImageUploadServiceImpl implements ImageUploadService {
    private final Cloudinary cloudinary;

    @Override
    public String uploadImage(MultipartFile file) {
        try{
            validateFile(file);

            Map uploadResult = cloudinary.uploader().upload(file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", "products",
                            "resource_type", "image"
                    ));

            String imageUrl = (String) uploadResult.get("secure_url");
            log.info("Image uploaded successfully: {}", imageUrl);

            return imageUrl;

        } catch (IOException e){
            log.error("Failed to upload image: {}", e.getMessage(), e);
            throw new ImageUploadException("Failed to upload image: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteImage(String imageUrl) {
        try{
            if (imageUrl == null || imageUrl.isEmpty()){
                return;
            }

            String publicId = extractPublicId(imageUrl);

            Map result = cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            log.info("Image deleted successfully: {}", publicId);

        } catch (IOException e){
            log.error("Failed to delete image: {}", e.getMessage(), e);
            throw new ImageUploadException("Failed to delete image: " + e.getMessage(), e);
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ImageUploadException("File is empty or null");
        }

        long maxSize = 5 * 1024 * 1024;
        if (file.getSize() > maxSize) {
            throw new ImageUploadException("File size exceed maximum limit of 5MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new ImageUploadException("File must be an image");
        }
    }

    private String extractPublicId(String imageUrl) {
        try{
            String[] parts = imageUrl.split("/upload/");
            if (parts.length < 2) {
                throw new ImageUploadException("Invalid Cloudinary URL format");
            }

            String pathAfterUpload = parts[1];

            String publicIdWithExtension = pathAfterUpload.replace("v\\d+/", "");

            int lastDotIndex = publicIdWithExtension.lastIndexOf(".");

            return lastDotIndex > 0
                    ? publicIdWithExtension.substring(0, lastDotIndex)
                    : publicIdWithExtension;
        } catch (Exception e){
            log.error("Failed to extract public_id from URL: {}", imageUrl, e);
            throw new ImageUploadException("Failed to extract public_id from URL", e);
        }
    }
}
