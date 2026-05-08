package com.hireflow.hireflow.service.impl;

import com.hireflow.hireflow.data.model.User;
import com.hireflow.hireflow.dto.request.LoginRequest;
import com.hireflow.hireflow.dto.request.RegisterRequest;
import com.hireflow.hireflow.dto.request.VerifyOtpRequest;
import com.hireflow.hireflow.dto.response.AuthResponse;
import com.hireflow.hireflow.exception.BusinessException;
import com.hireflow.hireflow.exception.DuplicateResourceException;
import com.hireflow.hireflow.exception.EmailNotVerifiedException;
import com.hireflow.hireflow.exception.ResourceNotFoundException;
import com.hireflow.hireflow.mapper.UserMapper;
import com.hireflow.hireflow.security.util.JwtUtil;
import com.hireflow.hireflow.service.AuthService;
import com.hireflow.hireflow.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public void register(RegisterRequest request) {
        try {
            if (userService.existsByEmail(request.getEmail())) {
                throw new DuplicateResourceException("An account with this email already exists");
            }

            String otp = generateOtp();

            User user = new User();
            user.setEmail(request.getEmail());
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            user.setFirstName(request.getFirstName());
            user.setLastName(request.getLastName());
            user.setRole(request.getRole());
            user.setOtp(otp);
            user.setVerified(false);
            user.setOtpExpiry(Instant.now().plusSeconds(600));

            userService.save(user);

            // TODO: replace with EmailService.sendOtp(request.getEmail(), otp)
            log.info("OTP for {}: {}", request.getEmail(), otp);

        } catch (DuplicateResourceException ex) {
            log.error(ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.error("User registration failed: {}", ex.getMessage());
            throw new BusinessException("User registration failed: Internal Server Error");
        }
    }

    @Override
    @Transactional
    public void verifyOtp(VerifyOtpRequest request) {
        try {
            User user = userService.findByEmail(request.getEmail())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));

            if (user.isVerified()) {
                throw new BusinessException("Account is already verified");
            }

            if (!request.getOtp().equals(user.getOtp())) {
                throw new BusinessException("Invalid OTP");
            }

            if (Instant.now().isAfter(user.getOtpExpiry())) {
                throw new BusinessException("OTP has expired. Please request a new one");
            }

            user.setOtp(null);
            user.setOtpExpiry(null);
            user.setVerified(true);

            userService.save(user);

        } catch (ResourceNotFoundException | BusinessException ex) {
            log.error(ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.error("OTP verification failed: {}", ex.getMessage());
            throw new BusinessException("OTP verification failed: Internal Server Error");
        }
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );

            User user = userService.findByEmail(request.getEmail())
                    .orElseThrow(() -> new BusinessException("Invalid credentials"));

            if (!user.isVerified()) {
                requireEmailVerification(user);
            }

            String token = jwtUtil.generateToken(user.getEmail());

            return userMapper.toAuthResponse(user, token);

        } catch (BadCredentialsException ex) {
            log.error("Invalid credentials for: {}", request.getEmail());
            throw new BusinessException("Invalid credentials");
        } catch (EmailNotVerifiedException | BusinessException ex) {
            log.warn(ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.error("Login failed: {}", ex.getMessage());
            throw new BusinessException("Login failed: Internal Server Error");
        }
    }

    private void requireEmailVerification(User user) {
        if (user.getOtpExpiry() == null || Instant.now().isAfter(user.getOtpExpiry())) {
            String otp = generateOtp();
            user.setOtp(otp);
            user.setOtpExpiry(Instant.now().plusSeconds(600));
            userService.save(user);
            log.info("New OTP for {}: {}", user.getEmail(), otp);
            throw new EmailNotVerifiedException("Your OTP has expired. A new OTP has been sent to your email.");
        }
        throw new EmailNotVerifiedException("Please verify your email. Enter the OTP sent to your inbox.");
    }

    private String generateOtp() {
        int otp = 100000 + new SecureRandom().nextInt(900000);
        return String.valueOf(otp);
    }
}
