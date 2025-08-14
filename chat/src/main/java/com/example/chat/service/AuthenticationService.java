package com.example.chat.service;

import com.example.chat.dto.LoginRequestDTO;
import com.example.chat.dto.LoginResponseDTO;
import com.example.chat.dto.RegisterRequestDTO;
import com.example.chat.dto.UserDTO;
import com.example.chat.jwt.JwtService;
import com.example.chat.model.User;
import com.example.chat.repo.UserRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class AuthenticationService {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationService.class);

    private final UserRepo userRepo;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtService jwtService;

    public AuthenticationService(UserRepo userRepo) {
        this.userRepo = userRepo;
    }

    public UserDTO signup(RegisterRequestDTO registerRequestDTO) {
        if (userRepo.findByUsername(registerRequestDTO.getUsername()).isPresent()) {
            throw new IllegalArgumentException("Username already exists");
        }

        User user = new User();
        user.setUsername(registerRequestDTO.getUsername());
        user.setPassword(passwordEncoder.encode(registerRequestDTO.getPassword()));
        user.setEmail(registerRequestDTO.getEmail());

        User savedUser = userRepo.save(user);
        return convertToUserDTO(savedUser);
    }

    public LoginResponseDTO login(LoginRequestDTO loginRequestDTO) {
        logger.info("Attempting login for user: {}", loginRequestDTO.getUsername());

        User user = userRepo.findByUsername(loginRequestDTO.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("Username not found"));

        logger.info("User found: {}, attempting authentication", user.getUsername());

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequestDTO.getUsername(), loginRequestDTO.getPassword()));

        logger.info("Authentication successful, setting user as online");

        // Set user as online
        user.setIsOnline(true);
        userRepo.save(user);

        logger.info("User {} is now online", user.getUsername());

        logger.info("Generating JWT token");
        String jwtToken = jwtService.generateToken(user);

        logger.info("JWT token generated successfully for user: {}", user.getUsername());

        LoginResponseDTO response = LoginResponseDTO.builder()
                .token(jwtToken)
                .userDTO(convertToUserDTO(user))
                .build();

        logger.info("Login response prepared for user: {}", user.getUsername());
        return response;
    }

    public void logout(String username) {
        logger.info("Logging out user: {}", username);

        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Set user as offline
        user.setIsOnline(false);
        userRepo.save(user);

        logger.info("User {} is now offline", username);
    }

    public List<String> getOnlineUsers() {
        logger.info("Retrieving online users");

        List<User> allUsers = userRepo.findAll();
        logger.info("Total users in database: {}", allUsers.size());

        List<User> onlineUsers = userRepo.findAllByIsOnline(true);
        logger.info("Online users found: {}", onlineUsers.size());

        for (User user : onlineUsers) {
            logger.info("Online user: {} (ID: {}, isOnline: {})",
                    user.getUsername(), user.getId(), user.getIsOnline());
        }

        List<String> onlineUsernames = onlineUsers.stream()
                .map(User::getUsername)
                .toList();

        logger.info("Returning online usernames: {}", onlineUsernames);
        return onlineUsernames;
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
