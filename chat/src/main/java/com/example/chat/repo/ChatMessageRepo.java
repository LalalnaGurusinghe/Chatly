package com.example.chat.repo;

import com.example.chat.model.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepo extends JpaRepository<ChatMessage, Long> {

    @Query("SELECT cm FROM ChatMessage cm WHERE cm.messageType = 'PRIVATE_MESSAGE' AND ((cm.sender = :user1 AND cm.receiver = :user2) OR (cm.sender = :user2 AND cm.receiver = :user1)) ORDER BY cm.timestamp ASC")
    List<ChatMessage> findPrivateMessagesBetweenTwoUsers(@Param("user1") String user1, @Param("user2") String user2);


}
