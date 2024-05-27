package com.example.controller;

import com.example.entity.RestBean;
import com.example.service.ImageService;
import io.minio.errors.ErrorResponseException;
import jakarta.annotation.Resource;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ObjectController {

    @Resource
    ImageService service;

    @GetMapping("/images/**")
    public void imagesFetch(HttpServletRequest request, HttpServletResponse response) throws Exception {
        response.setHeader("Cache-Control", "max-age=2592000");
        response.setHeader("Content-Type", "image/jpg");
        this.fetchImage(request, response);
    }

    private void fetchImage(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String imagePath = request.getServletPath().substring(7);
        ServletOutputStream stream = response.getOutputStream();
        if(imagePath.isBlank() || imagePath.isEmpty()) {
            stream.println(RestBean.failure(404, "image name must be a non-empty string").toString());
        } else {
            try {
                service.fetchImageFromMinio(stream, imagePath);
            } catch (ErrorResponseException e) {
                if(e.response().code() == 404) {
                    response.setStatus(404);
                    stream.println(RestBean.failure(404, "Not found").toString());
                } else {
                    throw e;
                }
            }
        }
    }
}
