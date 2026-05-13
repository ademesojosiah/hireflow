package com.hireflow.hireflow.controller;

import com.hireflow.hireflow.dto.request.AcceptInviteRequest;
import com.hireflow.hireflow.dto.request.LoginRequest;
import com.hireflow.hireflow.dto.request.RegisterRequest;
import com.hireflow.hireflow.dto.request.VerifyOtpRequest;
import com.hireflow.hireflow.dto.response.ApiResponse;
import com.hireflow.hireflow.dto.response.AuthResponse;
import com.hireflow.hireflow.service.AuthService;
import com.hireflow.hireflow.service.InvitationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final InvitationService invitationService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Void>> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Registration successful. Check your email for the OTP"));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<Void>> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        authService.verifyOtp(request);
        return ResponseEntity.ok(ApiResponse.success("Email verified successfully"));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Login successful", authService.login(request)));
    }

    @PostMapping("/accept-invite")
    public ResponseEntity<ApiResponse<AuthResponse>> acceptInvite(@Valid @RequestBody AcceptInviteRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Account created successfully", invitationService.acceptInvite(request)));
    }
}
