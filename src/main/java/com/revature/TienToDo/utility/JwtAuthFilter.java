package com.revature.TienToDo.utility;

import com.revature.TienToDo.dto.ApiError;
import com.revature.TienToDo.service.CustomUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
//import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Map;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthFilter.class);

    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    //@Autowired
    //private ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        // Step 1: Extract Authorization header
        final String authHeader = request.getHeader(AUTH_HEADER);

        // Step 2: Skip if no Bearer token present
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Step 3: Extract the JWT string
        final String jwt = authHeader.substring(BEARER_PREFIX.length());

        // Step 4: Check for token-level errors before extracting claims
        String tokenError = jwtUtil.getTokenError(jwt);
        if (tokenError != null) {
            logger.warn("JWT rejected for {} {}: {}", request.getMethod(), request.getRequestURI(), tokenError);
            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, tokenError);
            return;
        }

        // Step 4b: Reject refresh tokens used as access tokens
        if (jwtUtil.isRefreshToken(jwt)) {
            logger.warn("Refresh token used as access token for {} {}",
                    request.getMethod(), request.getRequestURI());
            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "Refresh tokens cannot be used for API access. Use an access token instead.");
            return;
        }

        try {
            // Step 5: Extract username and authenticate
            final String username = jwtUtil.extractUsername(jwt);

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                // Step 5a: Load user from database
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                // Step 5b: Validate token against user details
                if (jwtUtil.isTokenValid(jwt, userDetails)) {

                    // Step 5c: Set authentication in SecurityContext
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities());

                    authToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authToken);

                    logger.debug("Authenticated user '{}' for {} {}",
                            username, request.getMethod(), request.getRequestURI());
                } else {
                    logger.warn("Token validation failed for user '{}' on {} {}",
                            username, request.getMethod(), request.getRequestURI());
                    sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED,
                            "Token validation failed");
                    return;
                }
            }

        } catch (UsernameNotFoundException e) {
            // User was deleted after the token was issued
            logger.warn("JWT references non-existent user: {}", e.getMessage());
            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "User account no longer exists");
            return;

        } catch (Exception e) {
            logger.error("Unexpected error during JWT authentication: {}", e.getMessage());
            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "Authentication failed");
            return;
        }

        // Step 6: Continue filter chain
        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = request.getServletPath();
        return path.equals("/api/auth/register")
                || path.equals("/api/auth/login")
                || path.equals("/api/auth/refresh");
    }

    private void sendErrorResponse(HttpServletResponse response, int status, String message)
            throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(
                "{\"status\":" + status + ",\"message\":\"" + message + "\"}"
        );
    }
}
