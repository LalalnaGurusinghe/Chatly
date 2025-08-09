package com.example.chat.service;

import com.example.chat.repo.UserRepo;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    private final UserRepo userRepo;

    public UserService(UserRepo userRepo) {
        this.userRepo = userRepo;
    }

    public boolean userExists(String username) {
        return userRepo.existsByUsername(username);
    }

    public void setUserOnlineStatus(String username, boolean isOnline){
        userRepo.updateUserOnlineStatus(username, isOnline);
    }
}
