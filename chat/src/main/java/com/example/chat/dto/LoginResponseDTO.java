package com.example.chat.dto;

import lombok.Data;

@Data
public class LoginResponseDTO {
    private String token;
    private UserDTO userDTO;
}
