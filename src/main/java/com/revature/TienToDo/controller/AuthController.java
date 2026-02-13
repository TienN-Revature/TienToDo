package com.revature.TienToDo.controller;
import com.revature.TienToDo.dto.AuthResponse;
import com.revature.TienToDo.dto.LoginRequest;
import com.revature.TienToDo.dto.RefreshTokenRequest;
import com.revature.TienToDo.dto.RegisterRequest;
import com.revature.TienToDo.entity.User;
import com.revature.TienToDo.service.AuthService;
import com.revature.TienToDo.utility.JwtUtil;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @Autowired
    private AuthService authService;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);

        // Generate a refresh token alongside the access token
        String refreshToken = jwtUtil.generateRefreshToken(response.getUsername());

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "token", response.getToken(),
                "refreshToken", refreshToken,
                "type", response.getType(),
                "userId", response.getUserId(),
                "username", response.getUsername(),
                "email", response.getEmail()
        ));
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);

        // Generate a refresh token alongside the access token
        String refreshToken = jwtUtil.generateRefreshToken(response.getUsername());

        return ResponseEntity.ok(Map.of(
                "token", response.getToken(),
                "refreshToken", refreshToken,
                "type", response.getType(),
                "userId", response.getUserId(),
                "username", response.getUsername(),
                "email", response.getEmail()
        ));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = authService.refreshAccessToken(request.getRefreshToken());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser(Authentication auth) {
        User user = authService.getUserByUsername(auth.getName());
        return ResponseEntity.ok(Map.of(
                "userId", user.getId(),
                "username", user.getUsername(),
                "email", user.getEmail(),
                "createdAt", user.getCreatedAt().toString()
        ));
    }

    @GetMapping("/me/stats")
    public ResponseEntity<Map<String, Object>> getUserStats(Authentication auth) {
        User user = authService.getUserByUsername(auth.getName());
        long total = authService.getTodoCount(user.getId());
        long completed = authService.getCompletedTodoCount(user.getId());
        long active = total - completed;
        double completionRate = total > 0 ? Math.round((double) completed / total * 1000.0) / 10.0 : 0.0;

        return ResponseEntity.ok(Map.of(
                "totalTodos", total,
                "completedTodos", completed,
                "activeTodos", active,
                "completionRate", completionRate
        ));
    }

}
