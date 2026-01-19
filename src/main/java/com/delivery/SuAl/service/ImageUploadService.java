package com.delivery.SuAl.service;

import org.springframework.web.multipart.MultipartFile;

public interface ImageUploadService {
    String uploadImageForProduct(MultipartFile file);

    String uploadImageForCampaign(MultipartFile file);

    void deleteImage(String imageUrl);
}
