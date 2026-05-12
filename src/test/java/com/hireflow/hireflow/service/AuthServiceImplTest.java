package com.hireflow.hireflow.service;

import com.hireflow.hireflow.data.model.User;
import com.hireflow.hireflow.dto.request.LoginRequest;
import com.hireflow.hireflow.dto.request.RegisterRequest;
import com.hireflow.hireflow.dto.request.VerifyOtpRequest;
import com.hireflow.hireflow.dto.response.AuthResponse;
import com.hireflow.hireflow.enums.Role;
import com.hireflow.hireflow.event.events.EmailNotificationEvent;
import com.hireflow.hireflow.exception.CustomException;
import com.hireflow.hireflow.exception.DuplicateResourceException;
import com.hireflow.hireflow.exception.EmailNotVerifiedException;
import com.hireflow.hireflow.exception.ResourceNotFoundException;
import com.hireflow.hireflow.mapper.UserMapper;
import com.hireflow.hireflow.security.util.JwtUtil;
import com.hireflow.hireflow.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserService userService;
    @Mock
    private ApplicationEventPublisher applicationEventPublisher;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private UserMapper userMapper;
    @InjectMocks
    private AuthServiceImpl authService;

    private User verifiedUser;
    private User unverifiedUser;

    @BeforeEach
    void setUp() {
        verifiedUser = new User();
        verifiedUser.setEmail("john@example.com");
        verifiedUser.setPassword("encoded_password");
        verifiedUser.setFirstName("John");
        verifiedUser.setLastName("Doe");
        verifiedUser.setRole(Role.APPLICANT);
        verifiedUser.setVerified(true);
        verifiedUser.setId("user-uuid-123");

        unverifiedUser = new User();
        unverifiedUser.setEmail("jane@example.com");
        unverifiedUser.setPassword("encoded_password");
        unverifiedUser.setFirstName("Jane");
        unverifiedUser.setLastName("Doe");
        unverifiedUser.setRole(Role.APPLICANT);
        unverifiedUser.setOtp("123456");
        unverifiedUser.setOtpExpiry(Instant.now().plusSeconds(300));
        unverifiedUser.setVerified(false);
    }


    @Test
    @DisplayName("Should register user, encode password, and persist to database")
    void register_success() {
        RegisterRequest request = new RegisterRequest("John", "Doe", "john@example.com", "password123", Role.APPLICANT);
        when(userService.existsByEmail(request.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encoded_password");

        authService.register(request);

        verify(passwordEncoder).encode("password123");
        verify(userService).save(any(User.class));
        verify(applicationEventPublisher).publishEvent(argThat((Object event) ->
                event instanceof EmailNotificationEvent emailEvent
                        && EmailNotificationEvent.OTP_VERIFICATION.equals(emailEvent.getType())
                        && "john@example.com".equals(emailEvent.getTo())
                        && emailEvent.getOtp() != null
        ));
    }

    @Test
    @DisplayName("Should throw DuplicateResourceException when email is already registered")
    void register_duplicateEmail() {
        RegisterRequest request = new RegisterRequest("John", "Doe", "john@example.com", "password123", Role.APPLICANT);
        when(userService.existsByEmail(request.getEmail())).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("An account with this email already exists");

        verify(userService, never()).save(any());
    }

    @Test
    @DisplayName("Should wrap unexpected error during registration as CustomException")
    void register_unexpectedException() {
        RegisterRequest request = new RegisterRequest("John", "Doe", "john@example.com", "password123", Role.APPLICANT);
        when(userService.existsByEmail(anyString())).thenThrow(new RuntimeException("DB down"));

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("Internal Server Error");
    }


    @Test
    @DisplayName("Should mark user as verified and clear OTP fields on valid OTP")
    void verifyOtp_success() {
        VerifyOtpRequest request = new VerifyOtpRequest("jane@example.com", "123456");
        when(userService.findByEmail(request.getEmail())).thenReturn(Optional.of(unverifiedUser));

        authService.verifyOtp(request);

        assertThat(unverifiedUser.isVerified()).isTrue();
        assertThat(unverifiedUser.getOtp()).isNull();
        assertThat(unverifiedUser.getOtpExpiry()).isNull();
        verify(userService).save(unverifiedUser);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when user email does not exist")
    void verifyOtp_userNotFound() {
        VerifyOtpRequest request = new VerifyOtpRequest("ghost@example.com", "123456");
        when(userService.findByEmail(request.getEmail())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.verifyOtp(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found");
    }

    @Test
    @DisplayName("Should throw CustomException when account is already verified")
    void verifyOtp_alreadyVerified() {
        VerifyOtpRequest request = new VerifyOtpRequest("john@example.com", "123456");
        when(userService.findByEmail(request.getEmail())).thenReturn(Optional.of(verifiedUser));

        assertThatThrownBy(() -> authService.verifyOtp(request))
                .isInstanceOf(CustomException.class)
                .hasMessage("Account is already verified");
    }

    @Test
    @DisplayName("Should throw CustomException when OTP does not match")
    void verifyOtp_invalidOtp() {
        VerifyOtpRequest request = new VerifyOtpRequest("jane@example.com", "999999");
        when(userService.findByEmail(request.getEmail())).thenReturn(Optional.of(unverifiedUser));

        assertThatThrownBy(() -> authService.verifyOtp(request))
                .isInstanceOf(CustomException.class)
                .hasMessage("Invalid OTP");
    }

    @Test
    @DisplayName("Should throw CustomException when OTP has expired")
    void verifyOtp_expiredOtp() {
        unverifiedUser.setOtpExpiry(Instant.now().minusSeconds(60));
        VerifyOtpRequest request = new VerifyOtpRequest("jane@example.com", "123456");
        when(userService.findByEmail(request.getEmail())).thenReturn(Optional.of(unverifiedUser));

        assertThatThrownBy(() -> authService.verifyOtp(request))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("expired");
    }


    @Test
    @DisplayName("Should return AuthResponse with JWT token on successful login")
    void login_success() {
        LoginRequest request = new LoginRequest("john@example.com", "password123");
        AuthResponse expectedResponse = new AuthResponse("jwt-token", "user-uuid-123", "john@example.com", "APPLICANT");
        when(userService.findByEmail(request.getEmail())).thenReturn(Optional.of(verifiedUser));
        when(jwtUtil.generateToken(verifiedUser.getEmail())).thenReturn("jwt-token");
        when(userMapper.toAuthResponse(verifiedUser, "jwt-token")).thenReturn(expectedResponse);

        AuthResponse response = authService.login(request);

        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(response.getEmail()).isEqualTo("john@example.com");
        assertThat(response.getRole()).isEqualTo("APPLICANT");
    }

    @Test
    @DisplayName("Should throw CustomException when email is not registered")
    void login_userNotFound() {
        LoginRequest request = new LoginRequest("ghost@example.com", "password123");
        when(userService.findByEmail(request.getEmail())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(CustomException.class)
                .hasMessage("Invalid credentials");
    }

    @Test
    @DisplayName("Should throw EmailNotVerifiedException when account has a valid unexpired OTP")
    void login_notVerified_otpActive() {
        LoginRequest request = new LoginRequest("jane@example.com", "password123");
        when(userService.findByEmail(request.getEmail())).thenReturn(Optional.of(unverifiedUser));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(EmailNotVerifiedException.class)
                .hasMessageContaining("Enter the OTP");

        verify(userService, never()).save(any());
    }

    @Test
    @DisplayName("Should regenerate OTP and throw EmailNotVerifiedException when OTP has expired")
    void login_notVerified_otpExpired() {
        unverifiedUser.setOtpExpiry(Instant.now().minusSeconds(60));
        LoginRequest request = new LoginRequest("jane@example.com", "password123");
        when(userService.findByEmail(request.getEmail())).thenReturn(Optional.of(unverifiedUser));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(EmailNotVerifiedException.class)
                .hasMessageContaining("new OTP has been sent");

        assertThat(unverifiedUser.getOtp()).isNotNull();
        assertThat(unverifiedUser.getOtpExpiry()).isAfter(Instant.now());
        verify(userService).save(unverifiedUser);
        verify(applicationEventPublisher).publishEvent(argThat((Object event) ->
                event instanceof EmailNotificationEvent emailEvent
                        && EmailNotificationEvent.OTP_VERIFICATION.equals(emailEvent.getType())
                        && "jane@example.com".equals(emailEvent.getTo())
                        && emailEvent.getOtp() != null
        ));
    }

    @Test
    @DisplayName("Should throw CustomException when password is incorrect")
    void login_wrongPassword() {
        LoginRequest request = new LoginRequest("john@example.com", "wrongpass");
        doThrow(new BadCredentialsException("Bad credentials")).when(authenticationManager).authenticate(any());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(CustomException.class)
                .hasMessage("Invalid credentials");
    }

    @Test
    @DisplayName("Should wrap unexpected error during login as CustomException")
    void login_unexpectedException() {
        LoginRequest request = new LoginRequest("john@example.com", "password123");
        when(userService.findByEmail(anyString())).thenThrow(new RuntimeException("DB down"));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("Internal Server Error");
    }
}
