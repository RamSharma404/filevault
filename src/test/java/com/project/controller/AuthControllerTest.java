package com.project.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.dto.AuthResponse;
import com.project.dto.GoogleAuthRequest;
import com.project.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldAuthenticateWithGoogle() throws Exception {
        GoogleAuthRequest request = new GoogleAuthRequest("valid-google-jwt");
        AuthResponse response = new AuthResponse("our-jwt-token", "test@example.com");

        when(authService.verifyGoogleToken(anyString())).thenReturn(response);

        mockMvc.perform(post("/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("our-jwt-token"))
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }
}
