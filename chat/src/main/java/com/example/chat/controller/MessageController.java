package com.example.chat.controller;

import com.example.chat.model.ChatMessage;
import com.example.chat.repo.ChatMessageRepo;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    private final ChatMessageRepo chatMessageRepo;

    public MessageController(ChatMessageRepo chatMessageRepo) {
        this.chatMessageRepo = chatMessageRepo;
    }

    @GetMapping("/private")
    public ResponseEntity<List<ChatMessage>> getPrivateMessages(@RequestParam String user1 , @RequestParam String user2){
        List<ChatMessage> chatMessages = chatMessageRepo.findPrivateMessagesBetweenTwoUsers(user1,user2);
        return ResponseEntity.ok(chatMessages);
    }


}

