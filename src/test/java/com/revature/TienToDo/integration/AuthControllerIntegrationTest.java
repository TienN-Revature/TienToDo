package com.revature.TienToDo.integration;

import com.revature.TienToDo.entity.User;
import com.revature.TienToDo.repository.UserRepository;
import com.revature.TienToDo.utility.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
public class AuthControllerIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    private User existingUser;
    private String accessToken;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        existingUser = new User();
        existingUser.setUsername("existing_user");
        existingUser.setEmail("existing@example.com");
        existingUser.setPasswordHash(passwordEncoder.encode("Secret123!"));
        existingUser.setCreatedAt(LocalDateTime.now());
        existingUser.setUpdatedAt(LocalDateTime.now());
        existingUser = userRepository.save(existingUser);

        accessToken = jwtUtil.generateToken("existing_user");
    }

    @Nested
    @DisplayName("POST /api/auth/register")
    class RegisterTests {

        @Test
        @DisplayName("201 — successful registration returns tokens and user info")
        void register_Success() throws Exception {
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                {
                                    "username": "new_user",
                                    "email": "new@example.com",
                                    "password": "Secret123!",
                                    "confirmPassword": "Secret123!"
                                }
                                """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.token").isNotEmpty())
                    .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                    .andExpect(jsonPath("$.type").value("Bearer"))
                    .andExpect(jsonPath("$.username").value("new_user"))
                    .andExpect(jsonPath("$.email").value("new@example.com"))
                    .andExpect(jsonPath("$.userId").isNumber());
        }

        @Test
        @DisplayName("400 — duplicate username is rejected")
        void register_DuplicateUsername() throws Exception {
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                {
                                    "username": "existing_user",
                                    "email": "different@example.com",
                                    "password": "Secret123!",
                                    "confirmPassword": "Secret123!"
                                }
                                """))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("400 — duplicate email is rejected")
        void register_DuplicateEmail() throws Exception {
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                {
                                    "username": "another_user",
                                    "email": "existing@example.com",
                                    "password": "Secret123!",
                                    "confirmPassword": "Secret123!"
                                }
                                """))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("400 — password mismatch is rejected")
        void register_PasswordMismatch() throws Exception {
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                {
                                    "username": "new_user",
                                    "email": "new@example.com",
                                    "password": "Secret123!",
                                    "confirmPassword": "Different456!"
                                }
                                """))
                    .andExpect(status().isBadRequest());
        }
    }

    // ==================== LOGIN ====================

    @Nested
    @DisplayName("POST /api/auth/login")
    class LoginTests {

        @Test
        @DisplayName("200 — valid credentials return tokens")
        void login_Success() throws Exception {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                {
                                    "username": "existing_user",
                                    "password": "Secret123!"
                                }
                                """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").isNotEmpty())
                    .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                    .andExpect(jsonPath("$.type").value("Bearer"))
                    .andExpect(jsonPath("$.username").value("existing_user"))
                    .andExpect(jsonPath("$.email").value("existing@example.com"));
        }

        @Test
        @DisplayName("401 — wrong password is rejected")
        void login_WrongPassword() throws Exception {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                {
                                    "username": "existing_user",
                                    "password": "WrongPassword!"
                                }
                                """))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("401 — non-existent user is rejected")
        void login_UserNotFound() throws Exception {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                {
                                    "username": "ghost",
                                    "password": "Secret123!"
                                }
                                """))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ==================== REFRESH TOKEN ====================

    @Nested
    @DisplayName("POST /api/auth/refresh")
    class RefreshTests {

        @Test
        @DisplayName("200 — valid refresh token returns new access token")
        void refresh_Success() throws Exception {
            String refreshToken = jwtUtil.generateRefreshToken("existing_user");

            mockMvc.perform(post("/api/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"refreshToken\": \"" + refreshToken + "\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").isNotEmpty())
                    .andExpect(jsonPath("$.username").value("existing_user"));
        }

        @Test
        @DisplayName("400 — access token used as refresh is rejected")
        void refresh_AccessTokenRejected() throws Exception {
            mockMvc.perform(post("/api/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"refreshToken\": \"" + accessToken + "\"}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("400 — invalid token is rejected")
        void refresh_InvalidToken() throws Exception {
            mockMvc.perform(post("/api/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"refreshToken\": \"invalid.token.here\"}"))
                    .andExpect(status().isBadRequest());
        }
    }

    // ==================== GET PROFILE ====================

    @Nested
    @DisplayName("GET /api/auth/me")
    class GetProfileTests {

        @Test
        @DisplayName("200 — returns user profile with valid token")
        void getProfile_Success() throws Exception {
            mockMvc.perform(get("/api/auth/me")
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.userId").value(existingUser.getId()))
                    .andExpect(jsonPath("$.username").value("existing_user"))
                    .andExpect(jsonPath("$.email").value("existing@example.com"))
                    .andExpect(jsonPath("$.createdAt").isNotEmpty());
        }

        @Test
        @DisplayName("401 — no token is rejected")
        void getProfile_NoToken() throws Exception {
            mockMvc.perform(get("/api/auth/me"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("401 — invalid token is rejected")
        void getProfile_InvalidToken() throws Exception {
            mockMvc.perform(get("/api/auth/me")
                            .header("Authorization", "Bearer invalid.token.here"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ==================== CHANGE PASSWORD ====================

    @Nested
    @DisplayName("PUT /api/auth/me/password")
    class ChangePasswordTests {

        @Test
        @DisplayName("200 — password changed successfully")
        void changePassword_Success() throws Exception {
            mockMvc.perform(put("/api/auth/me/password")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                {
                                    "currentPassword": "Secret123!",
                                    "newPassword": "NewSecret456!",
                                    "confirmNewPassword": "NewSecret456!"
                                }
                                """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Password updated successfully"));

            // Verify can login with new password
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                {
                                    "username": "existing_user",
                                    "password": "NewSecret456!"
                                }
                                """))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("401 — wrong current password is rejected")
        void changePassword_WrongCurrent() throws Exception {
            mockMvc.perform(put("/api/auth/me/password")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                {
                                    "currentPassword": "WrongPassword!",
                                    "newPassword": "NewSecret456!",
                                    "confirmNewPassword": "NewSecret456!"
                                }
                                """))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ==================== CHANGE EMAIL ====================

    @Nested
    @DisplayName("PUT /api/auth/me/email")
    class ChangeEmailTests {

        @Test
        @DisplayName("200 — email changed successfully")
        void changeEmail_Success() throws Exception {
            mockMvc.perform(put("/api/auth/me/email")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                {
                                    "password": "Secret123!",
                                    "newEmail": "newemail@example.com"
                                }
                                """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Email updated successfully"))
                    .andExpect(jsonPath("$.email").value("newemail@example.com"));
        }

        @Test
        @DisplayName("400 — same email is rejected")
        void changeEmail_SameEmail() throws Exception {
            mockMvc.perform(put("/api/auth/me/email")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                {
                                    "password": "Secret123!",
                                    "newEmail": "existing@example.com"
                                }
                                """))
                    .andExpect(status().isBadRequest());
        }
    }

    // ==================== DELETE ACCOUNT ====================

    @Nested
    @DisplayName("DELETE /api/auth/me")
    class DeleteAccountTests {

        @Test
        @DisplayName("200 — account deleted, can't login after")
        void deleteAccount_Success() throws Exception {
            mockMvc.perform(delete("/api/auth/me")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"password\": \"Secret123!\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Account deleted successfully"));

            // Verify user is gone
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                {
                                    "username": "existing_user",
                                    "password": "Secret123!"
                                }
                                """))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("401 — wrong password is rejected")
        void deleteAccount_WrongPassword() throws Exception {
            mockMvc.perform(delete("/api/auth/me")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"password\": \"WrongPassword!\"}"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ==================== SECURITY FILTER CHAIN ====================

    @Nested
    @DisplayName("Security Filter Chain")
    class SecurityTests {

        @Test
        @DisplayName("public endpoints are accessible without token")
        void publicEndpoints_NoAuth() throws Exception {
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                {
                                    "username": "public_test",
                                    "email": "public@example.com",
                                    "password": "Secret123!",
                                    "confirmPassword": "Secret123!"
                                }
                                """))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("protected endpoints reject missing token")
        void protectedEndpoints_NoAuth() throws Exception {
            mockMvc.perform(get("/api/auth/me"))
                    .andExpect(status().isUnauthorized());

            mockMvc.perform(get("/api/auth/me/stats"))
                    .andExpect(status().isUnauthorized());

            mockMvc.perform(put("/api/auth/me/password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("refresh token used as access token is rejected")
        void refreshTokenAsAccess_Rejected() throws Exception {
            String refreshToken = jwtUtil.generateRefreshToken("existing_user");

            mockMvc.perform(get("/api/auth/me")
                            .header("Authorization", "Bearer " + refreshToken))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ==================== FULL FLOW ====================

    @Nested
    @DisplayName("End-to-End Flows")
    class E2ETests {

        @Test
        @DisplayName("register → login → get profile → change password → login with new password")
        void fullAuthFlow() throws Exception {
            // 1. Register
            MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                {
                                    "username": "flow_user",
                                    "email": "flow@example.com",
                                    "password": "Secret123!",
                                    "confirmPassword": "Secret123!"
                                }
                                """))
                    .andExpect(status().isCreated())
                    .andReturn();

            // Extract token from register response
            String body = registerResult.getResponse().getContentAsString();
            String token = extractJsonValue(body, "token");

            // 2. Get profile with register token
            mockMvc.perform(get("/api/auth/me")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.username").value("flow_user"));

            // 3. Change password
            mockMvc.perform(put("/api/auth/me/password")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                {
                                    "currentPassword": "Secret123!",
                                    "newPassword": "Changed456!",
                                    "confirmNewPassword": "Changed456!"
                                }
                                """))
                    .andExpect(status().isOk());

            // 4. Login with new password
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                {
                                    "username": "flow_user",
                                    "password": "Changed456!"
                                }
                                """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").isNotEmpty());

            // 5. Old password should fail
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                {
                                    "username": "flow_user",
                                    "password": "Secret123!"
                                }
                                """))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ==================== HELPER ====================

    private String extractJsonValue(String json, String key) {
        // Simple extraction — works for flat JSON
        int start = json.indexOf("\"" + key + "\":\"") + key.length() + 4;
        int end = json.indexOf("\"", start);
        return json.substring(start, end);
    }
}
