package com.example.chat.jwt;

import com.example.chat.repo.UserRepo;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtService jwtService;

    private final UserRepo userRepo;

    public JwtAuthenticationFilter(JwtService jwtService, UserRepo userRepo) {
        this.jwtService = jwtService;
        this.userRepo = userRepo;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String requestURI = request.getRequestURI();
        String requestMethod = request.getMethod();
        logger.info("Processing request: {} {}", requestMethod, requestURI);

        // Skip JWT processing for OPTIONS requests, public endpoints, and error pages
        if (requestMethod.equals("OPTIONS") ||
                requestURI.equals("/auth/login") ||
                requestURI.equals("/auth/signup") ||
                requestURI.equals("/error") ||
                requestURI.equals("/health") ||
                requestURI.equals("/test") ||
                requestURI.startsWith("/h2-console/") ||
                requestURI.startsWith("/api/") ||
                requestURI.startsWith("/ws/")) {
            logger.debug("Skipping JWT authentication for public endpoint or OPTIONS request: {} {}", requestMethod,
                    requestURI);
            filterChain.doFilter(request, response);
            return;
        }

        String jwtToken = null;

        String authHeader = request.getHeader("Authorization");
        logger.debug("Authorization header: {}", authHeader);

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            jwtToken = authHeader.substring(7);
            logger.info("Found JWT token in Authorization header, length: {}", jwtToken.length());
        }

        if (jwtToken == null) {
            logger.info("No JWT token found, continuing as anonymous user");
            filterChain.doFilter(request, response);
            return;
        }

        Long userId;
        try {
            userId = jwtService.extractUserId(jwtToken);
            logger.info("Extracted userId: {} from JWT token", userId);
        } catch (Exception e) {
            logger.error("Error extracting userId from JWT token: {}", e.getMessage(), e);
            filterChain.doFilter(request, response);
            return;
        }

        if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                var userDetails = userRepo.findById(userId)
                        .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));

                logger.info("Found user details: {} (ID: {}, isOnline: {})",
                        userDetails.getUsername(), userDetails.getId(), userDetails.getIsOnline());

                if (jwtService.isTokenValid(jwtToken, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            Collections.emptyList());

                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    logger.info("Successfully authenticated user: {} and set in SecurityContext",
                            userDetails.getUsername());

                    // Verify the authentication was set
                    Authentication currentAuth = SecurityContextHolder.getContext().getAuthentication();
                    logger.info("Current authentication in SecurityContext: {} (authenticated: {})",
                            currentAuth != null ? currentAuth.getName() : "null",
                            currentAuth != null ? currentAuth.isAuthenticated() : false);
                } else {
                    logger.warn("JWT token validation failed for user: {}", userDetails.getUsername());
                }
            } catch (Exception e) {
                logger.error("Error processing JWT authentication for userId {}: {}", userId, e.getMessage(), e);
            }
        } else {
            if (userId == null) {
                logger.warn("UserId is null, cannot authenticate");
            } else {
                logger.info("User already authenticated: {}",
                        SecurityContextHolder.getContext().getAuthentication().getName());
            }
        }

        filterChain.doFilter(request, response);
    }
}
