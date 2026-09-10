package com.example.domain.repository;

import androidx.lifecycle.LiveData;
import com.example.domain.model.UnifiedMessage;
import java.util.List;

public interface MessageRepository {
    LiveData<List<UnifiedMessage>> getMessagesForConversation(String conversationId);
    List<UnifiedMessage> getMessagesForConversationSync(String conversationId);
    long insertMessage(UnifiedMessage message);
    void updateMessage(UnifiedMessage message);
    void deleteMessage(long messageId);
    void clearConversation(String conversationId);
}
