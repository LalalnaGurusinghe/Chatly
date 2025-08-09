package com.example.chat.controller;

import com.example.chat.model.ChatMessage;
import com.example.chat.repo.ChatMessageRepo;
import com.example.chat.service.UserService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;

@Controller
public class ChatController {

    private final UserService userService;
    private final ChatMessageRepo chatMessageRepo;

    public ChatController(UserService userService, ChatMessageRepo chatMessageRepo) {
        this.userService = userService;
        this.chatMessageRepo = chatMessageRepo;
    }

    @MessageMapping("/chat.addUser")//websocket destination for adding a user
    @SendTo("/topic/public")// This will broadcast the message to all subscribers of /topic/public {channel}
    public ChatMessage addUser(@Payload ChatMessage chatMessage , SimpMessageHeaderAccessor headAccessor){
        if(userService.userExists(chatMessage.getSender())){
            //store user in session
            headAccessor.getSessionAttributes().put("username", chatMessage.getSender());
            userService.setUserOnlineStatus(chatMessage.getSender(), true);

            System.out.println("User added: " + chatMessage.getSender() + " is online"
                    + " with Session ID: " + headAccessor.getSessionId());

            chatMessage.setTimestamp(LocalDateTime.now());
            if(chatMessage.getContent()==null || chatMessage.getContent().isEmpty()){
                chatMessage.setContent(" ");
            }

            return chatMessageRepo.save(chatMessage);

        }

        return null;
    }

    @MessageMapping("/chat.sendMessage")
    @SendTo("/topic/public")
    public ChatMessage sendMessage(@Payload ChatMessage chatMessage) {
        if(userService.userExists(chatMessage.getSender())){
           if(chatMessage.getTimestamp()==null){
               chatMessage.setTimestamp(LocalDateTime.now());
           }
           if(chatMessage.getContent()==null){
               chatMessage.setContent(" ");
           }
           return chatMessageRepo.save(chatMessage);
        }
        return null;
    }

    @MessageMapping("/chat.sendPrivateMessage")
    public void sendPrivateMessage(@Payload ChatMessage chatMessage, SimpMessageHeaderAccessor headerAccessor) {

    }
}
