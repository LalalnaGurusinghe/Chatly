package com.example.chat.controller;

import com.example.chat.dto.LoginRequestDTO;
import com.example.chat.dto.LoginResponseDTO;
import com.example.chat.dto.RegisterRequestDTO;
import com.example.chat.dto.UserDTO;
import com.example.chat.model.User;
import com.example.chat.repo.UserRepo;
import com.example.chat.service.AuthenticationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final AuthenticationService authenticationService;

    private final UserRepo userRepo;

    public AuthController(AuthenticationService authenticationService, UserRepo userRepo) {
        this.authenticationService = authenticationService;
        this.userRepo = userRepo;
    }

    @PostMapping("/signup")
    public ResponseEntity<UserDTO> signup(@RequestBody RegisterRequestDTO registerRequestDTO) {
        logger.info("Processing signup request for user: {}", registerRequestDTO.getUsername());
        try {
            UserDTO userDTO = authenticationService.signup(registerRequestDTO);
            logger.info("Signup successful for user: {}", userDTO.getUsername());
            return ResponseEntity.ok()
                    .header("Access-Control-Allow-Origin", "*")
                    .header("Access-Control-Allow-Methods", "POST, OPTIONS")
                    .header("Access-Control-Allow-Headers", "*")
                    .body(userDTO);
        } catch (Exception e) {
            logger.error("Signup failed for user {}: {}", registerRequestDTO.getUsername(), e.getMessage());
            throw e;
        }
    }

    @RequestMapping(value = "/signup", method = RequestMethod.OPTIONS)
    public ResponseEntity<?> signupOptions() {
        logger.info("Handling OPTIONS request for /auth/signup");
        return ResponseEntity.ok()
                .header("Access-Control-Allow-Origin", "*")
                .header("Access-Control-Allow-Methods", "POST, OPTIONS")
                .header("Access-Control-Allow-Headers", "*")
                .build();
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@RequestBody LoginRequestDTO loginRequestDTO) {
        logger.info("Processing login request for user: {}", loginRequestDTO.getUsername());
        try {
            LoginResponseDTO loginResponseDTO = authenticationService.login(loginRequestDTO);
            logger.info("Login successful for user: {}", loginRequestDTO.getUsername());
            return ResponseEntity.ok()
                    .header("Access-Control-Allow-Origin", "*")
                    .header("Access-Control-Allow-Methods", "POST, OPTIONS")
                    .header("Access-Control-Allow-Headers", "*")
                    .body(loginResponseDTO);
        } catch (Exception e) {
            logger.error("Login failed for user {}: {}", loginRequestDTO.getUsername(), e.getMessage());
            throw e;
        }
    }

    @RequestMapping(value = "/login", method = RequestMethod.OPTIONS)
    public ResponseEntity<?> loginOptions() {
        logger.info("Handling OPTIONS request for /auth/login");
        return ResponseEntity.ok()
                .header("Access-Control-Allow-Origin", "*")
                .header("Access-Control-Allow-Methods", "POST, OPTIONS")
                .header("Access-Control-Allow-Headers", "*")
                .build();
    }

    @RequestMapping(value = "/error", method = RequestMethod.OPTIONS)
    public ResponseEntity<?> errorOptions() {
        return ResponseEntity.ok()
                .header("Access-Control-Allow-Origin", "*")
                .header("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
                .header("Access-Control-Allow-Headers", "*")
                .build();
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        logger.info("Health check endpoint called");
        return ResponseEntity.ok("OK");
    }

    @GetMapping("/test")
    public ResponseEntity<String> test() {
        logger.info("Test endpoint called");
        return ResponseEntity.ok()
                .header("Access-Control-Allow-Origin", "*")
                .header("Access-Control-Allow-Methods", "GET, OPTIONS")
                .header("Access-Control-Allow-Headers", "*")
                .body("Test endpoint working!");
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            String username = authentication.getName();
            authenticationService.logout(username);
            return ResponseEntity.ok("Logged out successfully");
        }
        return ResponseEntity.ok("Logged out successfully");
    }

    @GetMapping("/current-user")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        logger.info("Getting current user, authentication: {}", authentication);

        if (authentication == null) {
            logger.warn("Authentication is null, user not authenticated");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("User is not authenticated");
        }

        String username = authentication.getName();
        logger.info("Username from authentication: {}", username);

        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        logger.info("Found user: {}", user.getUsername());
        return ResponseEntity.ok(convertToUserDTO(user));
    }

    @GetMapping("/online-users")
    public ResponseEntity<?> getOnlineUsers() {
        logger.info("Getting online users");
        try {
            var result = authenticationService.getOnlineUsers();
            logger.info("Online users retrieved successfully: {}", result);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error getting online users: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error retrieving online users: " + e.getMessage());
        }
    }

    private UserDTO convertToUserDTO(User user) {
        UserDTO userDTO = new UserDTO();
        userDTO.setId(user.getId());
        userDTO.setUsername(user.getUsername());
        userDTO.setEmail(user.getEmail());
        userDTO.setIsOnline(user.getIsOnline());

        return userDTO;

    }

}
