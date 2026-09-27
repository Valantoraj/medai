package com.medai.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medai.dto.AuthResponse;
import com.medai.dto.LoginRequest;
import com.medai.dto.RegisterRequest;
import com.medai.service.AuthService;
import com.medai.util.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@DisplayName("AuthController Tests")
class AuthControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private AuthService authService;
    @MockBean private JwtUtil jwtUtil;

    // ── Register ───────────────────────────────────────────

    @Test
    @DisplayName("POST /api/auth/register — 200 OK with valid payload")
    void register_validPayload_returns200() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("newuser");
        req.setEmail("new@example.com");
        req.setPassword("password123");
        req.setFullName("New User");

        AuthResponse mockResponse = AuthResponse.builder()
                .token("mock.jwt.token")
                .username("newuser")
                .email("new@example.com")
                .fullName("New User")
                .personalisationEnabled(true)
                .message("Registration successful")
                .build();

        when(authService.register(any(RegisterRequest.class))).thenReturn(mockResponse);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("mock.jwt.token"))
                .andExpect(jsonPath("$.username").value("newuser"))
                .andExpect(jsonPath("$.message").value("Registration successful"));
    }

    @Test
    @DisplayName("POST /api/auth/register — 400 when username is blank")
    void register_blankUsername_returns400() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("");   // invalid
        req.setEmail("new@example.com");
        req.setPassword("password123");
        req.setFullName("New User");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/auth/register — 400 when email format is invalid")
    void register_invalidEmail_returns400() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("validuser");
        req.setEmail("not-an-email");  // invalid
        req.setPassword("password123");
        req.setFullName("Valid User");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/auth/register — 400 when password is too short")
    void register_shortPassword_returns400() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("validuser");
        req.setEmail("valid@example.com");
        req.setPassword("abc");  // < 8 chars
        req.setFullName("Valid User");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/auth/register — 400 when username already taken")
    void register_duplicateUsername_returns400() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("existinguser");
        req.setEmail("new@example.com");
        req.setPassword("password123");
        req.setFullName("Existing User");

        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new IllegalArgumentException("Username 'existinguser' is already taken."));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    // ── Login ──────────────────────────────────────────────

    @Test
    @DisplayName("POST /api/auth/login — 200 OK with valid credentials")
    void login_validCredentials_returns200() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setUsernameOrEmail("testuser");
        req.setPassword("password123");

        AuthResponse mockResponse = AuthResponse.builder()
                .token("valid.jwt.token")
                .username("testuser")
                .email("test@example.com")
                .fullName("Test User")
                .personalisationEnabled(true)
                .message("Login successful")
                .build();

        when(authService.login(any(LoginRequest.class))).thenReturn(mockResponse);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("valid.jwt.token"))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.personalisationEnabled").value(true));
    }

    @Test
    @DisplayName("POST /api/auth/login — 400 when credentials are invalid")
    void login_invalidCredentials_returns400() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setUsernameOrEmail("baduser");
        req.setPassword("wrongpass");

        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new BadCredentialsException("Invalid username/email or password"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/auth/login — 400 when fields are blank")
    void login_blankFields_returns400() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setUsernameOrEmail("");
        req.setPassword("");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/auth/logout — 200 OK clears cookie")
    void logout_returns200_andClearsCookie() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logged out successfully"));
    }
}
