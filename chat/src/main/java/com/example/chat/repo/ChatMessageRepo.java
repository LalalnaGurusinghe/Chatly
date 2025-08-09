package com.example.chat.repo;

import com.example.chat.model.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatMessageRepo extends JpaRepository<ChatMessage, Long> {

    // Define methods for CRUD operations on ChatMessage entities
    // For example:
    // List<ChatMessage> findByUserId(Long userId);
    // Optional<ChatMessage> findById(Long id);
    // void deleteById(Long id);
    // ChatMessage save(ChatMessage chatMessage);
}
