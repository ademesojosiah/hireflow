package com.hireflow.hireflow.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hireflow.hireflow.data.model.User;
import com.hireflow.hireflow.data.repository.UserRepository;
import com.hireflow.hireflow.dto.request.LoginRequest;
import com.hireflow.hireflow.dto.request.RegisterRequest;
import com.hireflow.hireflow.dto.request.VerifyOtpRequest;
import com.hireflow.hireflow.enums.Role;
import com.hireflow.hireflow.service.EmailService;
import java.time.Instant;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @MockitoBean private EmailService emailService;

    @AfterEach
    void cleanUp() {
        userRepository.deleteAll();
    }


    @Test
    @DisplayName("Should register user and persist to database with 201 response")
    void register_success() throws Exception {
        RegisterRequest request = new RegisterRequest("John", "Doe", "john@example.com", "password123", Role.APPLICANT);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Registration successful. Check your email for the OTP"));

        assertThat(userRepository.existsByEmail("john@example.com")).isTrue();
    }

    @Test
    @DisplayName("Should return 400 when registration fields are invalid")
    void register_validationFailure() throws Exception {
        RegisterRequest request = new RegisterRequest("", "", "not-an-email", "short", null);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Should return 409 when registering with an already used email")
    void register_duplicateEmail() throws Exception {

        userRepository.save(new User("Existing","User","existing@example.com",passwordEncoder.encode("password123"),Role.APPLICANT,true));


        RegisterRequest request = new RegisterRequest("New", "User", "existing@example.com", "password123", Role.APPLICANT);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("An account with this email already exists"));
    }


    @Test
    @DisplayName("Should verify OTP, activate account, and clear OTP fields in database")
    void verifyOtp_success() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest("Jane", "Doe", "jane@example.com", "password123", Role.APPLICANT))))
                .andExpect(status().isCreated());

        User registered = userRepository.findByEmail("jane@example.com").orElseThrow();
        String otp = registered.getOtp();

        mockMvc.perform(post("/api/v1/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new VerifyOtpRequest("jane@example.com", otp))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Email verified successfully"));

        User verified = userRepository.findByEmail("jane@example.com").orElseThrow();
        assertThat(verified.isVerified()).isTrue();
        assertThat(verified.getOtp()).isNull();
        assertThat(verified.getOtpExpiry()).isNull();
    }

    @Test
    @DisplayName("Should return 400 when OTP is incorrect")
    void verifyOtp_invalidOtp() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest("Jane", "Doe", "jane2@example.com", "password123", Role.APPLICANT))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new VerifyOtpRequest("jane2@example.com", "000000"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid OTP"));
    }


    @Test
    @DisplayName("Should return JWT token on successful login")
    void login_success() throws Exception {
        userRepository.save(new User("Bob","Smith","bob@example.com",passwordEncoder.encode("password123"),Role.APPLICANT,true));

        LoginRequest request = new LoginRequest("bob@example.com", "password123");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.email").value("bob@example.com"))
                .andExpect(jsonPath("$.data.role").value("APPLICANT"));
    }

    @Test
    @DisplayName("Should return 400 when login credentials are invalid")
    void login_invalidCredentials() throws Exception {
        LoginRequest request = new LoginRequest("nobody@example.com", "wrongpass");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid credentials"));
    }

    @Test
    @DisplayName("Should return 403 with OTP prompt when account is registered but not yet verified")
    void login_unverified_otpActive() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest("Unverified", "User", "unverified@example.com", "password123", Role.APPLICANT))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("unverified@example.com", "password123"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Please verify your email. Enter the OTP sent to your inbox."));
    }

    @Test
    @DisplayName("Should regenerate OTP and return 403 when login is attempted with an expired OTP")
    void login_unverified_otpExpired() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest("Expired", "User", "expired@example.com", "password123", Role.APPLICANT))))
                .andExpect(status().isCreated());

        User user = userRepository.findByEmail("expired@example.com").orElseThrow();
        String originalOtp = user.getOtp();
        user.setOtpExpiry(Instant.now().minusSeconds(60));
        userRepository.save(user);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("expired@example.com", "password123"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Please verify your email. A new OTP has been sent to your email."));

        User updated = userRepository.findByEmail("expired@example.com").orElseThrow();
        assertThat(updated.getOtp()).isNotEqualTo(originalOtp);
        assertThat(updated.getOtpExpiry()).isAfter(Instant.now());
    }
}
