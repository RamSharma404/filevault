package com.project.service;

import com.project.dto.AuthResponse;
import com.project.model.User;
import com.project.repository.UserRepository;
import com.project.security.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private OtpService otpService;

    @InjectMocks
    private AuthService authService;

    @Test
    void shouldRequestOtp() {
        doNothing().when(otpService).generateAndSendOtp(anyString());
        authService.requestOtp("test@example.com");
        verify(otpService).generateAndSendOtp("test@example.com");
    }

    @Test
    void shouldVerifyOtpNewUser() {
        when(otpService.verifyOtp(anyString(), anyString())).thenReturn(true);
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(jwtUtil.generateToken(anyString())).thenReturn("token");

        AuthResponse response = authService.verifyOtp("test@example.com", "123456");

        assertNotNull(response);
        assertEquals("test@example.com", response.email());
        assertEquals("token", response.token());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void shouldThrowWhenInvalidOtp() {
        when(otpService.verifyOtp(anyString(), anyString())).thenReturn(false);

        assertThrows(RuntimeException.class, () -> authService.verifyOtp("test@example.com", "wrong"));
    }
}
