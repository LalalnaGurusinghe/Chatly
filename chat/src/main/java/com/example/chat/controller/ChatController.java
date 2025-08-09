package com.example.chat.controller;

import com.example.chat.model.ChatMessage;
import com.example.chat.repo.ChatMessageRepo;
import com.example.chat.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;

@Controller
public class ChatController {

    private final UserService userService;
    private final ChatMessageRepo chatMessageRepo;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    public ChatController(UserService userService, ChatMessageRepo chatMessageRepo) {
        this.userService = userService;
        this.chatMessageRepo = chatMessageRepo;
    }

    @MessageMapping("/chat.addUser")//websocket destination for adding a user
    @SendTo("/topic/public")// This will broadcast the message to all subscribers of /topic/public {channel}
    public ChatMessage addUser(@Payload ChatMessage chatMessage, SimpMessageHeaderAccessor headAccessor) {
        if (userService.userExists(chatMessage.getSender())) {
            //store user in session
            headAccessor.getSessionAttributes().put("username", chatMessage.getSender());
            userService.setUserOnlineStatus(chatMessage.getSender(), true);

            System.out.println("User added: " + chatMessage.getSender() + " is online"
                    + " with Session ID: " + headAccessor.getSessionId());

            chatMessage.setTimestamp(LocalDateTime.now());
            if (chatMessage.getContent() == null || chatMessage.getContent().isEmpty()) {
                chatMessage.setContent(" ");
            }

            return chatMessageRepo.save(chatMessage);

        }

        return null;
    }

    @MessageMapping("/chat.sendMessage")
    @SendTo("/topic/public")
    public ChatMessage sendMessage(@Payload ChatMessage chatMessage) {
        if (userService.userExists(chatMessage.getSender())) {
            if (chatMessage.getTimestamp() == null) {
                chatMessage.setTimestamp(LocalDateTime.now());
            }
            if (chatMessage.getContent() == null) {
                chatMessage.setContent(" ");
            }
            return chatMessageRepo.save(chatMessage);
        }
        return null;
    }

    @MessageMapping("/chat.sendPrivateMessage")
    public void sendPrivateMessage(@Payload ChatMessage chatMessage, SimpMessageHeaderAccessor headerAccessor) {
        if (userService.userExists(chatMessage.getSender()) && userService.userExists(chatMessage.getReceiver())) {
            if (chatMessage.getTimestamp() == null) {
                chatMessage.setTimestamp(LocalDateTime.now());
            }
            if (chatMessage.getContent() == null) {
                chatMessage.setContent(" ");
            }

            ChatMessage savedMessage = chatMessageRepo.save(chatMessage);
            System.out.println("Message save successfully with id "+ savedMessage.getId());

            try{
                // Send the private message to the specific user
                String recepientDestination = "/user/" + chatMessage.getReceiver() + "/queue/private";
                System.out.println("Sending private message to: " + recepientDestination + " for user: " + chatMessage.getReceiver()
                        + " with content: " + chatMessage.getContent());
                messagingTemplate.convertAndSend(recepientDestination, savedMessage);


                String senderDestination = "/user/" + chatMessage.getSender() + "/queue/private";
                System.out.println("Sending private message to: " + senderDestination + " for user: " + chatMessage.getSender()
                        + " with content: " + chatMessage.getContent());
                messagingTemplate.convertAndSend(senderDestination, savedMessage);
            }
            catch (Exception e) {
                System.err.println("Error sending private message: " + e.getMessage());
            }
        }
        else{
            System.err.println("Error: User does not exist or is offline. Sender: " + chatMessage.getSender() + ", Receiver: " + chatMessage.getReceiver());
        }
    }
}
