package com.hireflow.hireflow.service;

import com.cloudinary.Cloudinary;
import com.hireflow.hireflow.dto.response.CloudinarySignatureResponse;
import com.hireflow.hireflow.exception.CustomException;
import com.hireflow.hireflow.service.upload.impl.CloudinaryUploadServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;

@ExtendWith(MockitoExtension.class)
@DisplayName("CloudinaryUploadServiceImpl")
class CloudinaryUploadServiceImplTest {

    private static final Map<String, Object> TEST_CONFIG = Map.of(
            "cloud_name", "test-cloud",
            "api_key", "test-key",
            "api_secret", "test-secret"
    );

    @Test
    @DisplayName("generatePdfUploadSignature returns correctly shaped response")
    void generatePdfUploadSignature_success() {
        Cloudinary cloudinary = new Cloudinary(TEST_CONFIG);
        CloudinaryUploadServiceImpl service = new CloudinaryUploadServiceImpl(cloudinary);

        CloudinarySignatureResponse response = service.generatePdfUploadSignature();

        assertThat(response.getCloudName()).isEqualTo("test-cloud");
        assertThat(response.getApiKey()).isEqualTo("test-key");
        assertThat(response.getFolder()).isEqualTo("resumes");
        assertThat(response.getResourceType()).isEqualTo("raw");
        assertThat(response.getSignature()).isNotBlank();
        assertThat(response.getTimestamp()).isPositive();
    }

    @Test
    @DisplayName("generatePdfUploadSignature wraps SDK exceptions in CustomException")
    void generatePdfUploadSignature_sdkFailure_throwsCustomException() {
        Cloudinary cloudinary = spy(new Cloudinary(TEST_CONFIG));
        doThrow(new RuntimeException("SDK error"))
                .when(cloudinary).apiSignRequest(anyMap(), anyString());

        CloudinaryUploadServiceImpl service = new CloudinaryUploadServiceImpl(cloudinary);

        assertThatThrownBy(service::generatePdfUploadSignature)
                .isInstanceOf(CustomException.class)
                .hasMessage("Failed to generate upload signature");
    }
}
