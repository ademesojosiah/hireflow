package com.hireflow.hireflow.service.impl;

import com.hireflow.hireflow.data.model.User;
import com.hireflow.hireflow.dto.request.LoginRequest;
import com.hireflow.hireflow.dto.request.RegisterRequest;
import com.hireflow.hireflow.enums.Role;
import com.hireflow.hireflow.dto.request.VerifyOtpRequest;
import com.hireflow.hireflow.dto.response.AuthResponse;
import com.hireflow.hireflow.event.events.EmailNotificationEvent;
import com.hireflow.hireflow.exception.CustomException;
import com.hireflow.hireflow.exception.DuplicateResourceException;
import com.hireflow.hireflow.exception.EmailNotVerifiedException;
import com.hireflow.hireflow.exception.ResourceNotFoundException;
import com.hireflow.hireflow.mapper.UserMapper;
import com.hireflow.hireflow.security.util.JwtUtil;
import com.hireflow.hireflow.service.AuthService;
import com.hireflow.hireflow.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
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
    private final ApplicationEventPublisher applicationEventPublisher;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final UserMapper userMapper;

    @Override
    public void register(RegisterRequest request) {
        try {
            if (request.getRole() == Role.HMANAGER) {
                throw new CustomException("This role cannot self-register. Please contact your administrator.");
            }

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

            String email = request.getEmail();
            publishOtpEmailRequested(email, otp);

        } catch (DuplicateResourceException ex) {
            log.error(ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.error("User registration failed: {}", ex.getMessage());
            throw new CustomException("User registration failed: Internal Server Error");
        }
    }

    @Override
    @Transactional
    public void verifyOtp(VerifyOtpRequest request) {
        try {
            User user = userService.findByEmail(request.getEmail())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));

            if (user.isVerified()) {
                throw new CustomException("Account is already verified");
            }

            if (!request.getOtp().equals(user.getOtp())) {
                throw new CustomException("Invalid OTP");
            }

            if (Instant.now().isAfter(user.getOtpExpiry())) {
                throw new CustomException("OTP has expired. Please request a new one");
            }

            user.setOtp(null);
            user.setOtpExpiry(null);
            user.setVerified(true);

            userService.save(user);

        } catch (ResourceNotFoundException | CustomException ex) {
            log.error(ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.error("OTP verification failed: {}", ex.getMessage());
            throw new CustomException("OTP verification failed: Internal Server Error");
        }
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );

            User user = userService.findByEmail(request.getEmail())
                    .orElseThrow(() -> new CustomException("Invalid credentials"));

            if (!user.isVerified()) {
                handleUnverifiedLogin(user);
            }

            String token = jwtUtil.generateToken(user.getEmail());

            return userMapper.toAuthResponse(user, token);

        } catch (BadCredentialsException ex) {
            log.error("Invalid credentials for: {}", request.getEmail());
            throw new CustomException("Invalid credentials");
        } catch (EmailNotVerifiedException | CustomException ex) {
            log.warn(ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.error("Login failed: {}", ex.getMessage());
            throw new CustomException("Login failed: Internal Server Error");
        }
    }

    private void handleUnverifiedLogin(User user) {
        if (user.getOtpExpiry() == null || Instant.now().isAfter(user.getOtpExpiry())) {
            String otp = rotateOtp(user);
            String email = user.getEmail();
            publishOtpEmailRequested(email, otp);
            throw new EmailNotVerifiedException("Please verify your email. A new OTP has been sent to your email.");
        }
        throw new EmailNotVerifiedException("Please verify your email. Enter the OTP sent to your inbox.");
    }

    private void publishOtpEmailRequested(String email, String otp) {
        applicationEventPublisher.publishEvent(new EmailNotificationEvent(
                EmailNotificationEvent.OTP_VERIFICATION,
                email,
                otp,
                null,
                null
        ));
    }

    protected String rotateOtp(User user) {
        String otp = generateOtp();
        user.setOtp(otp);
        user.setOtpExpiry(Instant.now().plusSeconds(600));
        userService.save(user);
        return otp;
    }

    private String generateOtp() {
        int otp = 100000 + new SecureRandom().nextInt(900000);
        return String.valueOf(otp);
    }
}
