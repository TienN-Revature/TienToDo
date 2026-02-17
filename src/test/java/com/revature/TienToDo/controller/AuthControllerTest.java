package com.revature.TienToDo.controller;


import com.revature.TienToDo.dto.AuthResponse;
import com.revature.TienToDo.dto.LoginRequest;
import com.revature.TienToDo.dto.RefreshTokenRequest;
import com.revature.TienToDo.dto.RegisterRequest;
import com.revature.TienToDo.entity.User;
import com.revature.TienToDo.service.AuthService;
import com.revature.TienToDo.utility.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AuthControllerTest {

    @Mock
    private AuthService authService;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AuthController authController;

    private User testUser;
    private AuthResponse testAuthResponse;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("john_doe");
        testUser.setEmail("john@example.com");
        testUser.setPasswordHash("$2a$10$hashedpassword");
        testUser.setCreatedAt(LocalDateTime.of(2026, 2, 15, 10, 0, 0));
        testUser.setUpdatedAt(LocalDateTime.of(2026, 2, 15, 10, 0, 0));

        testAuthResponse = new AuthResponse("access.token.here", 1L, "john_doe", "john@example.com");
    }

    @Nested
    @DisplayName("POST /api/auth/register")
    class RegisterTests {

        @Test
        @DisplayName("should register successfully and return 201 with tokens")
        void register_Success() {
            RegisterRequest request = new RegisterRequest();
            request.setUsername("john_doe");
            request.setEmail("john@example.com");
            request.setPassword("Secret123!");
            request.setConfirmPassword("Secret123!");

            when(authService.register(any(RegisterRequest.class))).thenReturn(testAuthResponse);
            when(jwtUtil.generateRefreshToken("john_doe")).thenReturn("refresh.token.here");

            ResponseEntity<Map<String, Object>> response = authController.register(request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().get("token")).isEqualTo("access.token.here");
            assertThat(response.getBody().get("refreshToken")).isEqualTo("refresh.token.here");
            assertThat(response.getBody().get("type")).isEqualTo("Bearer");
            assertThat(response.getBody().get("userId")).isEqualTo(1L);
            assertThat(response.getBody().get("username")).isEqualTo("john_doe");
            assertThat(response.getBody().get("email")).isEqualTo("john@example.com");

            verify(authService).register(any(RegisterRequest.class));
            verify(jwtUtil).generateRefreshToken("john_doe");
        }

        @Test
        @DisplayName("should propagate exception when username already exists")
        void register_UsernameExists() {
            RegisterRequest request = new RegisterRequest();
            request.setUsername("existing_user");

            when(authService.register(any(RegisterRequest.class)))
                    .thenThrow(new IllegalArgumentException("Username already exists"));

            org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                    () -> authController.register(request));
        }

        @Test
        @DisplayName("should propagate exception when email already exists")
        void register_EmailExists() {
            RegisterRequest request = new RegisterRequest();
            request.setEmail("existing@example.com");

            when(authService.register(any(RegisterRequest.class)))
                    .thenThrow(new IllegalArgumentException("Email already exists"));

            org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                    () -> authController.register(request));
        }

        @Test
        @DisplayName("should propagate exception when passwords don't match")
        void register_PasswordMismatch() {
            RegisterRequest request = new RegisterRequest();
            request.setPassword("Secret123!");
            request.setConfirmPassword("Different456!");

            when(authService.register(any(RegisterRequest.class)))
                    .thenThrow(new IllegalArgumentException("Password and confirmation do not match"));

            org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                    () -> authController.register(request));
        }
    }

    @Nested
    @DisplayName("POST /api/auth/login")
    class LoginTests {

        @Test
        @DisplayName("should login successfully and return 200 with tokens")
        void login_Success() {
            LoginRequest request = new LoginRequest();
            request.setUsername("john_doe");
            request.setPassword("Secret123!");

            when(authService.login(any(LoginRequest.class))).thenReturn(testAuthResponse);
            when(jwtUtil.generateRefreshToken("john_doe")).thenReturn("refresh.token.here");

            ResponseEntity<Map<String, Object>> response = authController.login(request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().get("token")).isEqualTo("access.token.here");
            assertThat(response.getBody().get("refreshToken")).isEqualTo("refresh.token.here");
            assertThat(response.getBody().get("type")).isEqualTo("Bearer");
            assertThat(response.getBody().get("userId")).isEqualTo(1L);
            assertThat(response.getBody().get("username")).isEqualTo("john_doe");
            assertThat(response.getBody().get("email")).isEqualTo("john@example.com");

            verify(authService).login(any(LoginRequest.class));
            verify(jwtUtil).generateRefreshToken("john_doe");
        }

        @Test
        @DisplayName("should propagate exception for invalid credentials")
        void login_InvalidCredentials() {
            LoginRequest request = new LoginRequest();
            request.setUsername("john_doe");
            request.setPassword("wrong_password");

            when(authService.login(any(LoginRequest.class)))
                    .thenThrow(new org.springframework.security.authentication.BadCredentialsException("Bad credentials"));

            org.junit.jupiter.api.Assertions.assertThrows(
                    org.springframework.security.authentication.BadCredentialsException.class,
                    () -> authController.login(request));
        }
    }

    @Nested
    @DisplayName("POST /api/auth/refresh")
    class RefreshTests {

        @Test
        @DisplayName("should refresh token successfully and return 200")
        void refresh_Success() {
            RefreshTokenRequest request = new RefreshTokenRequest();
            request.setRefreshToken("valid.refresh.token");

            when(authService.refreshAccessToken("valid.refresh.token")).thenReturn(testAuthResponse);

            ResponseEntity<AuthResponse> response = authController.refreshToken(request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getToken()).isEqualTo("access.token.here");
            assertThat(response.getBody().getUsername()).isEqualTo("john_doe");

            verify(authService).refreshAccessToken("valid.refresh.token");
        }

        @Test
        @DisplayName("should propagate exception for invalid refresh token")
        void refresh_InvalidToken() {
            RefreshTokenRequest request = new RefreshTokenRequest();
            request.setRefreshToken("invalid.token");

            when(authService.refreshAccessToken("invalid.token"))
                    .thenThrow(new IllegalArgumentException("Invalid or expired refresh token"));

            org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                    () -> authController.refreshToken(request));
        }
    }

    @Nested
    @DisplayName("GET /api/auth/me")
    class GetCurrentUserTests {

        @Test
        @DisplayName("should return current user profile")
        void getCurrentUser_Success() {
            when(authentication.getName()).thenReturn("john_doe");
            when(authService.getUserByUsername("john_doe")).thenReturn(testUser);

            ResponseEntity<Map<String, Object>> response = authController.getCurrentUser(authentication);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().get("userId")).isEqualTo(1L);
            assertThat(response.getBody().get("username")).isEqualTo("john_doe");
            assertThat(response.getBody().get("email")).isEqualTo("john@example.com");
            assertThat(response.getBody().get("createdAt")).isNotNull();

            verify(authService).getUserByUsername("john_doe");
        }
    }
}
