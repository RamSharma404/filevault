package com.project.controller;

import com.project.dto.AuthResponse;
import com.project.dto.OtpRequest;
import com.project.dto.VerifyOtpRequest;
import com.project.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/request-otp")
    public ResponseEntity<Void> requestOtp(@Valid @RequestBody OtpRequest request) {
        authService.requestOtp(request.email());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<AuthResponse> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        return ResponseEntity.ok(authService.verifyOtp(request.email(), request.otp()));
    }
}
