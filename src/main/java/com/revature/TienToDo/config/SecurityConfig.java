package com.revature.TienToDo.config;


import com.revature.TienToDo.utility.JwtAccessDeniedHandler;
import com.revature.TienToDo.utility.JwtAuthEntryPoint;
import com.revature.TienToDo.utility.JwtAuthFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    @Autowired
    private JwtAuthFilter jwtAuthFilter;

    //@Autowired
    //private UserDetailsService userDetailsService;

    @Autowired
    private JwtAuthEntryPoint jwtAuthEntryPoint;

    @Autowired
    private JwtAccessDeniedHandler jwtAccessDeniedHandler;

    @Value("${app.cors.allowed-origins:http://localhost:3000}")
    private String allowedOrigins;


    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, AuthenticationProvider authenticationProvider)
            throws Exception {
        http
                // Disable CSRF — stateless JWT API doesn't use cookies
                .csrf(AbstractHttpConfigurer::disable)

                // Enable CORS with configured origins
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // Authorization rules
                .authorizeHttpRequests(auth -> auth
                        // Public authentication endpoints
                        .requestMatchers(
                                "/api/auth/register",
                                "/api/auth/login",
                                "/api/auth/refresh"
                        ).permitAll()

                        // Everything else requires authentication
                        .anyRequest().authenticated()
                )

                // Stateless session — no HttpSession, no JSESSIONID cookie
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Custom exception handlers for 401 and 403
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(jwtAuthEntryPoint)
                        .accessDeniedHandler(jwtAccessDeniedHandler)
                )

                // Security headers
                .headers(headers -> headers
                        // Prevent the browser from MIME-sniffing the content type
                        .contentTypeOptions(contentType -> {})
                        // Prevent the page from being embedded in iframes (clickjacking protection)
                        .frameOptions(frame -> frame.deny())
                )

                // Authentication provider — uses CustomUserDetailsService + BCrypt
                .authenticationProvider(authenticationProvider)

                // JWT filter — runs before Spring's default username/password filter
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // Parse comma-separated origins from config
        config.setAllowedOrigins(List.of(allowedOrigins.split(",")));

        // Allow standard REST methods
        config.setAllowedMethods(List.of(
                "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

        // Allow Authorization header (for JWT) and Content-Type
        config.setAllowedHeaders(List.of(
                "Authorization", "Content-Type", "Accept", "Origin", "X-Requested-With"));

        // Expose Authorization header in responses (useful for token refresh)
        config.setExposedHeaders(List.of("Authorization"));

        // Allow credentials (cookies, authorization headers)
        config.setAllowCredentials(true);

        // Cache preflight response for 1 hour (reduces OPTIONS requests)
        config.setMaxAge(3600L);

        // Apply CORS config to all API paths
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }


    @Bean
    public AuthenticationProvider authenticationProvider(UserDetailsService userDetailsService) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
