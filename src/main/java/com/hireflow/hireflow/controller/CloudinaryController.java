package com.hireflow.hireflow.controller;

import com.hireflow.hireflow.dto.response.ApiResponse;
import com.hireflow.hireflow.service.upload.CloudUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/uploads")
@RequiredArgsConstructor
public class CloudinaryController {

    private final CloudUploadService cloudUploadService;

    @GetMapping("/pdf-signature")
    public ResponseEntity<?> getPdfUploadSignature() {
        return ResponseEntity.ok(ApiResponse.success(
                "Upload signature generated",
                cloudUploadService.generatePdfUploadSignature()
        ));
    }
}