package com.hireflow.hireflow.service.impl;

import com.hireflow.hireflow.dto.response.UploadSignatureResponse;
import com.hireflow.hireflow.service.UploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
@RequiredArgsConstructor
public class UploadServiceImpl implements UploadService {

    @Value("${cloudinary.cloud-name}")
    private String cloudName;

    @Value("${cloudinary.api-key}")
    private String apiKey;

    @Value("${cloudinary.api-secret}")
    private String apiSecret;

    private static final String UPLOAD_FOLDER = "pdfs";
    private static final String RESOURCE_TYPE = "raw";

    @Override
    public UploadSignatureResponse generatePdfUploadSignature() {

        long timestamp = System.currentTimeMillis() / 1000;

        Map<String, Object> paramsToSign = new LinkedHashMap<>();
        paramsToSign.put("folder", UPLOAD_FOLDER);
        paramsToSign.put("timestamp", timestamp);

        String signature = generateSignature(paramsToSign);

        return UploadSignatureResponse.builder()
                .cloudName(cloudName)
                .apiKey(apiKey)
                .timestamp(timestamp)
                .signature(signature)
                .folder(UPLOAD_FOLDER)
                .resourceType(RESOURCE_TYPE)
                .build();
    }

    private String generateSignature(Map<String, Object> params) {
        try {

            List<String> keys = new ArrayList<>(params.keySet());
            Collections.sort(keys);

            StringBuilder queryString = new StringBuilder();
            for (int i = 0; i < keys.size(); i++) {
                String key = keys.get(i);
                queryString.append(key).append("=").append(params.get(key));
                if (i < keys.size() - 1) {
                    queryString.append("&");
                }
            }

            Mac sha256Mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    apiSecret.getBytes(StandardCharsets.UTF_8),
                    0,
                    apiSecret.getBytes(StandardCharsets.UTF_8).length,
                    "HmacSHA256"
            );
            sha256Mac.init(secretKeySpec);

            byte[] hash = sha256Mac.doFinal(queryString.toString().getBytes(StandardCharsets.UTF_8));


            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            return hexString.toString();

        } catch (Exception ex) {
            throw new RuntimeException("Failed to generate signature: " + ex.getMessage());
        }
    }

}

