package com.revature.TienToDo.service;

import com.revature.TienToDo.dto.AuthResponse;
import com.revature.TienToDo.dto.LoginRequest;
import com.revature.TienToDo.dto.RegisterRequest;
import com.revature.TienToDo.entity.User;
import com.revature.TienToDo.repository.UserRepository;
import com.revature.TienToDo.utility.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AuthService {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Validate uniqueness
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }

        // Build and persist user entity
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user = userRepository.save(user);

        // Generate JWT and build response
        String token = jwtUtil.generateToken(user.getUsername());
        return new AuthResponse(token, user.getId(), user.getUsername(), user.getEmail());
    }

    public AuthResponse login(LoginRequest request) {
        // Spring Security verifies username + password against BCrypt hash
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(), request.getPassword()));

        // Authentication succeeded — load user and generate token
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String token = jwtUtil.generateToken(user.getUsername());
        return new AuthResponse(token, user.getId(), user.getUsername(), user.getEmail());
    }

    public AuthResponse refreshAccessToken(String refreshToken) {
        if (!jwtUtil.isTokenValid(refreshToken)) {
            throw new IllegalArgumentException("Invalid or expired refresh token");
        }
        if (!jwtUtil.isRefreshToken(refreshToken)) {
            throw new IllegalArgumentException("Token is not a refresh token");
        }

        String username = jwtUtil.extractUsername(refreshToken);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String newAccessToken = jwtUtil.generateToken(user.getUsername());
        return new AuthResponse(newAccessToken, user.getId(), user.getUsername(), user.getEmail());
    }

    @Transactional(readOnly = true)
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @Transactional(readOnly = true)
    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @Transactional
    public void updateEmail(Long userId, String email) {
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already exists");
        }
        userRepository.updateEmail(userId, email, LocalDateTime.now());
    }

    @Transactional
    public void updatePassword(Long userId, String newPassword) {
        String hash = passwordEncoder.encode(newPassword);
        userRepository.updatePassword(userId, hash, LocalDateTime.now());
    }

    @Transactional(readOnly = true)
    public long getTodoCount(Long userId) {
        return userRepository.countTodosByUserId(userId);
    }

    @Transactional(readOnly = true)
    public long getCompletedTodoCount(Long userId) {
        return userRepository.countCompletedTodosByUserId(userId);
    }
}
