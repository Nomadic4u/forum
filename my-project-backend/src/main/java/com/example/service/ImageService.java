package com.example.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.entity.dto.StoreImage;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.OutputStream;

public interface ImageService extends IService<StoreImage> {
    String uploadAvatar(int id, MultipartFile file) throws IOException;
    String uploadImage(int id, MultipartFile file) throws IOException;
    void fetchImageFromMinio(OutputStream stream, String imagePath) throws Exception;
}
