package com.example.chat.controller;

import com.example.chat.dto.LoginRequestDTO;
import com.example.chat.dto.LoginResponseDTO;
import com.example.chat.dto.RegisterRequestDTO;
import com.example.chat.dto.UserDTO;
import com.example.chat.model.User;
import com.example.chat.repo.UserRepo;
import com.example.chat.service.AuthenticationService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthenticationService authenticationService;

    private final UserRepo userRepo;

    public AuthController(AuthenticationService authenticationService, UserRepo userRepo) {
        this.authenticationService = authenticationService;
        this.userRepo = userRepo;
    }

    @PostMapping("/signup")
    public ResponseEntity<UserDTO> signup(@RequestBody RegisterRequestDTO registerRequestDTO){
           return ResponseEntity.ok(authenticationService.signup(registerRequestDTO));
    }

    @PostMapping("/login")
    public ResponseEntity<UserDTO> login(@RequestBody LoginRequestDTO loginRequestDTO) {

        LoginResponseDTO loginResponseDTO = authenticationService.login(loginRequestDTO);
        ResponseCookie responseCookie = ResponseCookie.from("JWT", loginResponseDTO.getToken())
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(60 * 60 * 24)
                .sameSite("Strict")
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, responseCookie.toString())
                .body(loginResponseDTO.getUserDTO());
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(){
        return authenticationService.logout();
    }

    @GetMapping("/current-user")
    public ResponseEntity<?> getCurrentUser(Authentication authentication){
        if(authentication == null){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("User is not authenticated");
        }

        String username = authentication.getName();
        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));


        return ResponseEntity.ok(convertToUserDTO(user));
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
