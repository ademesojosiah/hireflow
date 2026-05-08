package com.hireflow.hireflow.service;

import com.hireflow.hireflow.dto.request.LoginRequest;
import com.hireflow.hireflow.dto.request.RegisterRequest;
import com.hireflow.hireflow.dto.request.VerifyOtpRequest;
import com.hireflow.hireflow.dto.response.AuthResponse;

public interface AuthService {

    void register(RegisterRequest request);

    void verifyOtp(VerifyOtpRequest request);

    AuthResponse login(LoginRequest request);
}
