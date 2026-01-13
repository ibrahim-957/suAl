package com.delivery.SuAl.service;

import org.springframework.web.multipart.MultipartFile;

public interface ImageUploadService {
    String uploadImage(MultipartFile file);

    void deleteImage(String imageUrl);
}
