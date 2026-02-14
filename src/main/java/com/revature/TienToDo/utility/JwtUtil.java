package com.revature.TienToDo.utility;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtUtil {

    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration-ms}")
    private long jwtExpirationMs;

    @Value("${app.jwt.refresh-expiration-ms:604800000}")
    private long refreshExpirationMs;

    @Value("${app.jwt.issuer:todo-api}")
    private String issuer;

    private SecretKey signingKey;

    @PostConstruct
    public void init() {
        if (jwtSecret == null || jwtSecret.length() < 32) {
            throw new IllegalArgumentException(
                    "JWT secret must be at least 32 characters. " + "Current length: "
                    + (jwtSecret == null ? 0 : jwtSecret.length()));
        }
        this.signingKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        logger.info("JWT signing key initialized (issuer: {}, access TTL: {}ms, refresh TTL: {}ms)",
                issuer, jwtExpirationMs, refreshExpirationMs);
    }

    public String generateToken(String username) {
        return generateToken(username, new HashMap<>());
    }

    public String generateToken(String username, Map<String, Object> extraClaims) {
        return buildToken(username, extraClaims, jwtExpirationMs);
    }

    public String generateRefreshToken(String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "refresh");
        return buildToken(username, claims, refreshExpirationMs);
    }

    private String buildToken(String username, Map<String, Object> extraClaims, long expirationMs) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .claims(extraClaims)
                .subject(username)
                .issuer(issuer)
                .issuedAt(new Date(now))
                .expiration(new Date(now + expirationMs))
                .signWith(signingKey)
                .compact();
    }


    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public Date extractIssuedAt(String token) {
        return extractClaim(token, Claims::getIssuedAt);
    }

    public String extractIssuer(String token) {
        return extractClaim(token, Claims::getIssuer);
    }

    public <T> T extractCustomClaim(String token, String key, Class<T> type) {
        Claims claims = extractAllClaims(token);
        return claims.get(key, type);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }


    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .requireIssuer(issuer)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }


    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            final String username = extractUsername(token);
            return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException e) {
            logger.warn("JWT validation failed: {}", e.getMessage());
            return false;
        }
    }

    public boolean isTokenValid(String token) {
        try {
            extractAllClaims(token);
            return !isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException e) {
            logger.warn("JWT validation failed: {}", e.getMessage());
            return false;
        }
    }

    public boolean isTokenExpired(String token) {
        try {
            return extractExpiration(token).before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        }
    }

    public boolean isRefreshToken(String token) {
        try {
            String type = extractCustomClaim(token, "type", String.class);
            return "refresh".equals(type);
        } catch (JwtException e) {
            return false;
        }
    }


    public String getTokenError(String token) {
        try {
            extractAllClaims(token);
            if (isTokenExpired(token)) {
                return "Token has expired";
            }
            return null;
        } catch (ExpiredJwtException e) {
            return "Token has expired";
        } catch (SecurityException e) {
            return "Invalid token signature";
        } catch (MalformedJwtException e) {
            return "Malformed token";
        } catch (UnsupportedJwtException e) {
            return "Unsupported token format";
        } catch (IllegalArgumentException e) {
            return "Token claims string is empty";
        } catch (JwtException e) {
            return "Invalid token: " + e.getMessage();
        }
    }

    // ==================== TOKEN METADATA ====================

    /**
     * Get the remaining time-to-live for a token in milliseconds.
     * Returns 0 if the token is already expired.
     *
     * @param token the JWT string
     * @return remaining TTL in milliseconds, or 0 if expired
     */
    public long getTimeToLive(String token) {
        try {
            Date expiration = extractExpiration(token);
            long remaining = expiration.getTime() - System.currentTimeMillis();
            return Math.max(remaining, 0);
        } catch (ExpiredJwtException e) {
            return 0;
        }
    }

    public long getAccessTokenExpirationMs() {
        return jwtExpirationMs;
    }

    public long getRefreshTokenExpirationMs() {
        return refreshExpirationMs;
    }
}
