package com.hireflow.hireflow.controller;

import com.hireflow.hireflow.data.model.User;
import com.hireflow.hireflow.data.repository.UserRepository;
import com.hireflow.hireflow.dto.response.CloudinarySignatureResponse;
import com.hireflow.hireflow.enums.Role;
import com.hireflow.hireflow.security.UserPrincipal;
import com.hireflow.hireflow.service.upload.CloudUploadService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CloudinaryControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @MockitoBean private CloudUploadService cloudUploadService;

    private User applicant;
    private User hManager;

    @BeforeEach
    void setUp() {
        applicant = userRepository.save(new User(
                "John", "Doe", "john@example.com",
                passwordEncoder.encode("password"), Role.APPLICANT, true));
        hManager = userRepository.save(new User(
                "Hannah", "Manager", "hannah@example.com",
                passwordEncoder.encode("password"), Role.HMANAGER, true));

        when(cloudUploadService.generatePdfUploadSignature()).thenReturn(
                new CloudinarySignatureResponse(
                        "my-cloud", "my-key", 1_234_567_890L, "resumes", "raw", "abc123"));
    }

    @AfterEach
    void cleanUp() {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("APPLICANT receives 200 with correctly shaped signature response")
    void getPdfSignature_applicant_ok() throws Exception {
        mockMvc.perform(get("/api/v1/uploads/pdf-signature")
                        .with(user(new UserPrincipal(applicant))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.cloudName").value("my-cloud"))
                .andExpect(jsonPath("$.data.apiKey").value("my-key"))
                .andExpect(jsonPath("$.data.folder").value("resumes"))
                .andExpect(jsonPath("$.data.resourceType").value("raw"))
                .andExpect(jsonPath("$.data.signature").value("abc123"))
                .andExpect(jsonPath("$.data.timestamp").value(1_234_567_890L));
    }

    @Test
    @DisplayName("HMANAGER receives 403")
    void getPdfSignature_hManager_forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/uploads/pdf-signature")
                        .with(user(new UserPrincipal(hManager))))
                .andExpect(status().isForbidden());
    }
}
