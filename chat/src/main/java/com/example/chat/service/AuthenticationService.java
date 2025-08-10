package com.example.chat.service;

import com.example.chat.dto.LoginRequestDTO;
import com.example.chat.dto.LoginResponseDTO;
import com.example.chat.dto.RegisterRequestDTO;
import com.example.chat.dto.UserDTO;
import com.example.chat.jwt.JwtService;
import com.example.chat.model.User;
import com.example.chat.repo.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class AuthenticationService {

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
        if(userRepo.findByUsername(registerRequestDTO.getUsername()).isPresent()) {
            throw new IllegalArgumentException("Username already exists");
        }

        User user = new User();
        user.setUsername(registerRequestDTO.getUsername());
        user.setPassword(passwordEncoder.encode(registerRequestDTO.getPassword()));
        user.setEmail(registerRequestDTO.getEmail());

        User savedUser = userRepo.save(user);
        return convertToUserDTO(savedUser);
    }

    public LoginResponseDTO login(LoginRequestDTO loginRequestDTO){
        User user = userRepo.findByUsername(loginRequestDTO.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("Username not found"));

        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(loginRequestDTO.getUsername(), loginRequestDTO.getPassword()));
        String jwtToken = jwtService.generateToken(user);

        return LoginResponseDTO.builder()
                .token(jwtToken)
                .userDTO(convertToUserDTO(user))
                .build();
    }

     public ResponseEntity<String> logout(){
         ResponseCookie responseCookie = ResponseCookie.from("JWT", "")
                 .httpOnly(true)
                 .secure(true)
                 .path("/")
                 .maxAge(0) // Set max age to 0 to delete the cookie
                 .sameSite("Strict")
                 .build();

         return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, responseCookie.toString())
                 .body("Logged out successfully");
     }

    public Map<String , Object> getOnlineUsers(){
        List<User> onlineUsers = userRepo.findAllByIsOnline(true);
        return Map.of("onlineUsers", onlineUsers.stream().map(this::convertToUserDTO).toList());
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
