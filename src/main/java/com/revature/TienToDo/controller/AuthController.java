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

import java.util.LinkedHashMap;
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

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("token", response.getToken());
        body.put("refreshToken", refreshToken);
        body.put("type", response.getType());
        body.put("userId", response.getUserId());
        body.put("username", response.getUsername());
        body.put("email", response.getEmail());

        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);

        // Generate a refresh token alongside the access token
        String refreshToken = jwtUtil.generateRefreshToken(response.getUsername());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("token", response.getToken());
        body.put("refreshToken", refreshToken);
        body.put("type", response.getType());
        body.put("userId", response.getUserId());
        body.put("username", response.getUsername());
        body.put("email", response.getEmail());

        return ResponseEntity.ok(body);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = authService.refreshAccessToken(request.getRefreshToken());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser(Authentication auth) {
        User user = authService.getUserByUsername(auth.getName());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("userId", user.getId());
        body.put("username", user.getUsername());
        body.put("email", user.getEmail());
        body.put("createdAt", user.getCreatedAt().toString());

        return ResponseEntity.ok(body);
    }

    @GetMapping("/me/stats")
    public ResponseEntity<Map<String, Object>> getUserStats(Authentication auth) {
        User user = authService.getUserByUsername(auth.getName());
        long total = authService.getTodoCount(user.getId());
        long completed = authService.getCompletedTodoCount(user.getId());
        long active = total - completed;
        double completionRate = total > 0 ? Math.round((double) completed / total * 1000.0) / 10.0 : 0.0;

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("totalTodos", total);
        body.put("completedTodos", completed);
        body.put("activeTodos", active);
        body.put("completionRate", completionRate);

        return ResponseEntity.ok(body);
    }

}
