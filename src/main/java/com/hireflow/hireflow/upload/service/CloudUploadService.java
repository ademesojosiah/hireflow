package com.hireflow.hireflow.upload.service;

import com.hireflow.hireflow.upload.dto.response.CloudinarySignatureResponse;

public interface CloudUploadService {
    CloudinarySignatureResponse generatePdfUploadSignature();
}
