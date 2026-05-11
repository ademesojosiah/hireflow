package com.hireflow.hireflow.controller;

import com.hireflow.hireflow.dto.response.ApiResponse;
import com.hireflow.hireflow.dto.response.UploadSignatureResponse;
import com.hireflow.hireflow.service.UploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/uploads")
@RequiredArgsConstructor
public class UploadController {

    private final UploadService uploadService;

    @GetMapping("/pdf-signature")
    public ResponseEntity<ApiResponse<UploadSignatureResponse>> getPdfUploadSignature() {
        UploadSignatureResponse signature = uploadService.generatePdfUploadSignature();
        return ResponseEntity.ok(
                ApiResponse.success("Upload signature generated successfully", signature)
        );
    }

}

