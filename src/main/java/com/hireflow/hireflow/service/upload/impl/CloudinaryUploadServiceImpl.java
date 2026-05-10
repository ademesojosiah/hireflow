package com.hireflow.hireflow.service.upload.impl;

import com.cloudinary.Cloudinary;
import com.hireflow.hireflow.dto.response.CloudinarySignatureResponse;
import com.hireflow.hireflow.exception.CustomException;
import com.hireflow.hireflow.service.upload.CloudUploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CloudinaryUploadServiceImpl implements CloudUploadService {

    private final Cloudinary cloudinary;

    private static final String PDF_FOLDER = "resumes";
    private static final String RESOURCE_TYPE = "raw";

    @Override
    public CloudinarySignatureResponse generatePdfUploadSignature() {
        try {
            long timestamp = Instant.now().getEpochSecond();

            Map<String, Object> paramsToSign = new HashMap<>();
            paramsToSign.put("timestamp", timestamp);
            paramsToSign.put("folder", PDF_FOLDER);

            String signature = cloudinary.apiSignRequest(paramsToSign, cloudinary.config.apiSecret);

            log.info("Generated Cloudinary PDF upload signature for folder '{}'", PDF_FOLDER);

            return new CloudinarySignatureResponse(
                    cloudinary.config.cloudName,
                    cloudinary.config.apiKey,
                    timestamp,
                    PDF_FOLDER,
                    RESOURCE_TYPE,
                    signature
            );
        } catch (CustomException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to generate Cloudinary upload signature: {}", ex.getMessage());
            throw new CustomException("Failed to generate upload signature");
        }
    }
}
