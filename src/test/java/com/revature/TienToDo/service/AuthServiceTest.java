package com.revature.TienToDo.service;

import com.revature.TienToDo.dto.AuthResponse;
import com.revature.TienToDo.dto.LoginRequest;
import com.revature.TienToDo.dto.RegisterRequest;
import com.revature.TienToDo.entity.User;
import com.revature.TienToDo.repository.UserRepository;
import com.revature.TienToDo.utility.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {
    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("john_doe");
        testUser.setEmail("john@example.com");
        testUser.setPasswordHash("$2a$10$encoded_hash");
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setUpdatedAt(LocalDateTime.now());
    }

    @Nested
    @DisplayName("register()")
    class RegisterTests {

        private RegisterRequest validRequest() {
            RegisterRequest req = new RegisterRequest();
            req.setUsername("john_doe");
            req.setEmail("john@example.com");
            req.setPassword("Secret123!");
            req.setConfirmPassword("Secret123!");
            return req;
        }

        @Test
        @DisplayName("should register user, hash password, generate JWT")
        void register_Success() {
            RegisterRequest request = validRequest();

            when(userRepository.existsByUsername(anyString())).thenReturn(false);
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(passwordEncoder.encode("Secret123!")).thenReturn("$2a$10$encoded_hash");
            when(userRepository.save(any(User.class))).thenReturn(testUser);
            when(jwtUtil.generateToken("john_doe")).thenReturn("access.token");

            AuthResponse response = authService.register(request);

            assertThat(response.getToken()).isEqualTo("access.token");
            assertThat(response.getUserId()).isEqualTo(1L);
            assertThat(response.getUsername()).isEqualTo("john_doe");
            assertThat(response.getEmail()).isEqualTo("john@example.com");

            verify(passwordEncoder).encode("Secret123!");
            verify(userRepository).save(any(User.class));
            verify(jwtUtil).generateToken("john_doe");
        }

        @Test
        @DisplayName("should throw when passwords don't match")
        void register_PasswordMismatch() {
            RegisterRequest request = validRequest();
            request.setConfirmPassword("Different456!");

            assertThatThrownBy(() -> authService.register(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Password and confirmation do not match");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw when username is taken")
        void register_UsernameTaken() {
            RegisterRequest request = validRequest();

            when(userRepository.existsByUsername(anyString())).thenReturn(true);

            assertThatThrownBy(() -> authService.register(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Username already exists");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw when email is taken")
        void register_EmailTaken() {
            RegisterRequest request = validRequest();

            when(userRepository.existsByUsername(anyString())).thenReturn(false);
            when(userRepository.existsByEmail(anyString())).thenReturn(true);

            assertThatThrownBy(() -> authService.register(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Email already exists");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("should normalize email to lowercase")
        void register_NormalizesEmail() {
            RegisterRequest request = validRequest();
            request.setEmail("  John@Example.COM  ");

            when(userRepository.existsByUsername(anyString())).thenReturn(false);
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("hash");
            when(userRepository.save(any(User.class))).thenReturn(testUser);
            when(jwtUtil.generateToken(anyString())).thenReturn("token");

            authService.register(request);

            verify(userRepository).existsByEmail("john@example.com");
        }
    }

    @Nested
    @DisplayName("login()")
    class LoginTests {

        @Test
        @DisplayName("should authenticate and return JWT")
        void login_Success() {
            LoginRequest request = new LoginRequest();
            request.setUsername("john_doe");
            request.setPassword("Secret123!");

            when(userRepository.findByUsername("john_doe")).thenReturn(Optional.of(testUser));
            when(jwtUtil.generateToken("john_doe")).thenReturn("access.token");

            AuthResponse response = authService.login(request);

            assertThat(response.getToken()).isEqualTo("access.token");
            assertThat(response.getUserId()).isEqualTo(1L);

            verify(authenticationManager).authenticate(
                    any(UsernamePasswordAuthenticationToken.class));
        }

        @Test
        @DisplayName("should throw when credentials are invalid")
        void login_BadCredentials() {
            LoginRequest request = new LoginRequest();
            request.setUsername("john_doe");
            request.setPassword("wrong");

            when(authenticationManager.authenticate(any()))
                    .thenThrow(new BadCredentialsException("Bad credentials"));

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(BadCredentialsException.class);
        }

        @Test
        @DisplayName("should throw when user not found after auth")
        void login_UserNotFound() {
            LoginRequest request = new LoginRequest();
            request.setUsername("ghost");
            request.setPassword("Secret123!");

            when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("User not found");
        }

    }

    @Nested
    @DisplayName("refreshAccessToken()")
    class RefreshTests {

        @Test
        @DisplayName("should generate new access token from valid refresh token")
        void refresh_Success() {
            when(jwtUtil.isTokenValid("refresh.token")).thenReturn(true);
            when(jwtUtil.isRefreshToken("refresh.token")).thenReturn(true);
            when(jwtUtil.extractUsername("refresh.token")).thenReturn("john_doe");
            when(userRepository.findByUsername("john_doe")).thenReturn(Optional.of(testUser));
            when(jwtUtil.generateToken("john_doe")).thenReturn("new.access.token");

            AuthResponse response = authService.refreshAccessToken("refresh.token");

            assertThat(response.getToken()).isEqualTo("new.access.token");
            assertThat(response.getUsername()).isEqualTo("john_doe");
        }

        @Test
        @DisplayName("should throw when refresh token is invalid")
        void refresh_InvalidToken() {
            when(jwtUtil.isTokenValid("bad.token")).thenReturn(false);

            assertThatThrownBy(() -> authService.refreshAccessToken("bad.token"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Invalid or expired refresh token");
        }

        @Test
        @DisplayName("should throw when token is not a refresh type")
        void refresh_NotRefreshToken() {
            when(jwtUtil.isTokenValid("access.token")).thenReturn(true);
            when(jwtUtil.isRefreshToken("access.token")).thenReturn(false);

            assertThatThrownBy(() -> authService.refreshAccessToken("access.token"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Token is not a refresh token");
        }
    }

    @Nested
    @DisplayName("getUserByUsername()")
    class GetUserTests {

        @Test
        @DisplayName("should return user when found")
        void getUserByUsername_Found() {
            when(userRepository.findByUsername("john_doe")).thenReturn(Optional.of(testUser));

            User result = authService.getUserByUsername("john_doe");

            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getUsername()).isEqualTo("john_doe");
        }

        @Test
        @DisplayName("should throw when user not found")
        void getUserByUsername_NotFound() {
            when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.getUserByUsername("ghost"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("User not found");
        }
    }

    @Nested
    @DisplayName("updateEmail()")
    class UpdateEmailTests {

        @Test
        @DisplayName("should update email when not taken")
        void updateEmail_Success() {
            when(userRepository.existsByEmail("new@example.com")).thenReturn(false);

            authService.updateEmail(1L, "new@example.com");

            verify(userRepository).updateEmail(eq(1L), eq("new@example.com"), any(LocalDateTime.class));
        }

        @Test
        @DisplayName("should throw when email already taken")
        void updateEmail_AlreadyTaken() {
            when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

            assertThatThrownBy(() -> authService.updateEmail(1L, "taken@example.com"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Email already exists");

            verify(userRepository, never()).updateEmail(anyLong(), anyString(), any());
        }
    }

    @Nested
    @DisplayName("updatePassword()")
    class UpdatePasswordTests {

        @Test
        @DisplayName("should encode and update password")
        void updatePassword_Success() {
            when(passwordEncoder.encode("NewSecret456!")).thenReturn("$2a$10$new_hash");

            authService.updatePassword(1L, "NewSecret456!");

            verify(passwordEncoder).encode("NewSecret456!");
            verify(userRepository).updatePassword(eq(1L), eq("$2a$10$new_hash"), any(LocalDateTime.class));
        }
    }

    @Nested
    @DisplayName("Statistics")
    class StatsTests {

        @Test
        @DisplayName("should return total todo count")
        void getTodoCount() {
            when(userRepository.countTodosByUserId(1L)).thenReturn(10L);

            assertThat(authService.getTodoCount(1L)).isEqualTo(10L);
        }

        @Test
        @DisplayName("should return completed todo count")
        void getCompletedTodoCount() {
            when(userRepository.countCompletedTodosByUserId(1L)).thenReturn(7L);

            assertThat(authService.getCompletedTodoCount(1L)).isEqualTo(7L);
        }
    }

}
