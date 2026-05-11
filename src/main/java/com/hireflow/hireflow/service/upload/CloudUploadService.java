package com.hireflow.hireflow.service.upload;

import com.hireflow.hireflow.dto.response.CloudinarySignatureResponse;

public interface CloudUploadService {
    CloudinarySignatureResponse generatePdfUploadSignature();
}
