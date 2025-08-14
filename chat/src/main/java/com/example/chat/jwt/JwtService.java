package com.example.chat.jwt;

import com.example.chat.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import jakarta.annotation.PostConstruct;

@Service
public class JwtService {

    private static final Logger logger = LoggerFactory.getLogger(JwtService.class);

    @Value("${jwt.secret-key}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private Long jwtExpiration;

    // Add a method to log configuration values
    @PostConstruct
    public void logConfiguration() {
        logger.info("JWT Service initialized with:");
        logger.info("Secret key length: {}", secretKey != null ? secretKey.length() : "null");
        logger.info("JWT expiration: {} ms ({} hours)", jwtExpiration, jwtExpiration / (1000 * 60 * 60));
    }

    public Long extractUserId(String jwtToken) {
        try {
            if (jwtToken == null || jwtToken.trim().isEmpty()) {
                throw new IllegalArgumentException("JWT token is null or empty");
            }

            return extractClaims(jwtToken, claims -> {
                Object userIdClaim = claims.get("userId");
                logger.debug("Extracted userId claim: {} (type: {})", userIdClaim,
                        userIdClaim != null ? userIdClaim.getClass().getSimpleName() : "null");

                if (userIdClaim == null) {
                    throw new IllegalArgumentException("userId claim is null");
                }

                // Handle different numeric types
                if (userIdClaim instanceof Number) {
                    return ((Number) userIdClaim).longValue();
                } else if (userIdClaim instanceof String) {
                    try {
                        return Long.parseLong((String) userIdClaim);
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException(
                                "userId claim string cannot be parsed as Long: " + userIdClaim);
                    }
                }

                throw new IllegalArgumentException(
                        "Invalid userId claim type: " + userIdClaim.getClass() + ", value: " + userIdClaim);
            });
        } catch (Exception e) {
            logger.error("Error extracting userId from JWT token: {}", e.getMessage(), e);
            throw e;
        }
    }

    private <T> T extractClaims(String jwtToken, Function<Claims, T> claimsResolver) {
        try {
            final Claims claims = extractAllClaims(jwtToken);
            return claimsResolver.apply(claims);
        } catch (Exception e) {
            logger.error("Error extracting claims from JWT token: {}", e.getMessage(), e);
            throw e;
        }
    }

    public Claims extractAllClaims(String jwtToken) {
        try {
            return Jwts.parser()
                    .verifyWith(getSignInKey())
                    .build()
                    .parseSignedClaims(jwtToken)
                    .getBody();
        } catch (Exception e) {
            logger.error("Error parsing JWT token: {}", e.getMessage(), e);
            throw e;
        }
    }

    public SecretKey getSignInKey() {
        if (secretKey == null || secretKey.trim().isEmpty()) {
            throw new IllegalArgumentException("JWT secret key is null or empty");
        }

        try {
            return Keys.hmacShaKeyFor(secretKey.getBytes());
        } catch (Exception e) {
            logger.error("Error creating signing key from secret: {}", e.getMessage(), e);
            throw new IllegalArgumentException("Invalid JWT secret key", e);
        }
    }

    public String generateToken(User user) {
        return generateToken(new HashMap<>(), user);
    }

    public String generateToken(Map<String, Object> extraClaims, User user) {
        try {
            Map<String, Object> claims = new HashMap<>(extraClaims);
            // Explicitly ensure userId is stored as Long
            claims.put("userId", Long.valueOf(user.getId()));

            logger.info("Generating JWT token for user: {}, userId: {} (type: {})",
                    user.getUsername(), user.getId(), user.getId().getClass().getSimpleName());
            logger.debug("JWT claims map: {}", claims);
            logger.debug("JWT expiration: {} ms", jwtExpiration);

            String token = Jwts.builder()
                    .claims(claims)
                    .subject(user.getUsername())
                    .issuedAt(new Date(System.currentTimeMillis()))
                    .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
                    .signWith(getSignInKey())
                    .compact();

            logger.info("Generated JWT token successfully for user: {}", user.getUsername());
            logger.debug("Generated JWT token: {}", token);

            // Verify the token can be parsed back
            try {
                Claims parsedClaims = extractAllClaims(token);
                logger.debug("Successfully parsed generated token, claims: {}", parsedClaims);
            } catch (Exception e) {
                logger.error("Error parsing generated token: {}", e.getMessage(), e);
            }

            return token;
        } catch (Exception e) {
            logger.error("Error generating JWT token for user {}: {}", user.getUsername(), e.getMessage(), e);
            throw e;
        }
    }

    public boolean isTokenValid(String jwtToken, User user) {
        try {
            logger.info("Validating JWT token for user: {}", user.getUsername());

            final Long userIdFromToken = extractUserId(jwtToken);
            final Long userId = user.getId();

            logger.info("UserId from token: {}, UserId from user: {}", userIdFromToken, userId);

            boolean isExpired = isTokenExpired(jwtToken);
            logger.info("Token expired: {}", isExpired);

            boolean isValid = (userIdFromToken != null && userIdFromToken.equals(userId) && !isExpired);
            logger.info("Token validation result: {}", isValid);

            if (!isValid) {
                if (userIdFromToken == null) {
                    logger.warn("UserId from token is null");
                } else if (!userIdFromToken.equals(userId)) {
                    logger.warn("UserId mismatch: token={}, user={}", userIdFromToken, userId);
                } else if (isExpired) {
                    logger.warn("Token is expired");
                }
            }

            return isValid;
        } catch (Exception e) {
            logger.error("Error validating JWT token for user {}: {}", user.getUsername(), e.getMessage(), e);
            return false;
        }
    }

    private boolean isTokenExpired(String jwtToken) {
        try {
            Date expiration = extractExpiration(jwtToken);
            Date now = new Date();
            boolean expired = expiration.before(now);

            logger.info("Token expiration check - Expiration: {}, Now: {}, Expired: {}",
                    expiration, now, expired);

            if (expired) {
                long diffInMillis = now.getTime() - expiration.getTime();
                long diffInMinutes = diffInMillis / (1000 * 60);
                logger.warn("Token expired {} minutes ago", diffInMinutes);
            } else {
                long diffInMillis = expiration.getTime() - now.getTime();
                long diffInMinutes = diffInMillis / (1000 * 60);
                logger.info("Token will expire in {} minutes", diffInMinutes);
            }

            return expired;
        } catch (Exception e) {
            logger.error("Error checking token expiration: {}", e.getMessage(), e);
            return true;
        }
    }

    private Date extractExpiration(String jwtToken) {
        return extractClaims(jwtToken, Claims::getExpiration);
    }
}
