package com.example.chat.listner;

import com.example.chat.model.ChatMessage;
import com.example.chat.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
public class WebSocketListener {
    private final UserService userService;
    @Autowired
    private SimpMessageSendingOperations messagingTemplate;

    private static final Logger logger = LoggerFactory.getLogger(WebSocketListener.class);

    public WebSocketListener(UserService userService) {
        this.userService = userService;
    }

    @EventListener
    public void handleWebsocketConnectListener(SessionConnectedEvent event) {
        logger.info("Connected to websocket");
    }

    @EventListener
    public void handleWebsocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String username = (String) headerAccessor.getSessionAttributes().get("username");

        if (username != null) {
            System.out.println("User disconnected: " + username + " with Session ID: " + headerAccessor.getSessionId());
            userService.setUserOnlineStatus(username, false);

            ChatMessage chatMessage = new ChatMessage();
            chatMessage.setMessageType(ChatMessage.MessageType.LEAVE);
            chatMessage.setSender(username);
            chatMessage.setContent(" ");
            chatMessage.setTimestamp(java.time.LocalDateTime.now());

            messagingTemplate.convertAndSend("/topic/group", chatMessage);
        }
    }
}
