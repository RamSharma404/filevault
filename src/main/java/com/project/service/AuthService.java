package com.project.service;

import com.project.dto.AuthResponse;
import com.project.model.User;
import com.project.repository.UserRepository;
import com.project.security.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final OtpService otpService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil,
                       OtpService otpService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.otpService = otpService;
    }

    public void requestOtp(String email) {
        otpService.generateAndSendOtp(email);
    }

    public AuthResponse verifyOtp(String email, String otp) {
        if (!otpService.verifyOtp(email, otp)) {
            throw new RuntimeException("Invalid or expired code");
        }

        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            // Register new user with a random unguessable password
            user = new User(email, passwordEncoder.encode(UUID.randomUUID().toString()));
            userRepository.save(user);
        }

        String token = jwtUtil.generateToken(user.getEmail());
        return new AuthResponse(token, user.getEmail());
    }
}
