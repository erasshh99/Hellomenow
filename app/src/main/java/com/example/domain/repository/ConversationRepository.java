package com.example.domain.repository;

import androidx.lifecycle.LiveData;
import com.example.domain.model.Conversation;
import java.util.List;

public interface ConversationRepository {
    LiveData<List<Conversation>> getConversations();
    default LiveData<List<Conversation>> getAllConversations() { return getConversations(); }
    Conversation getConversationByIdSync(String id);
    void insertConversation(Conversation conversation);
    void updateConversation(Conversation conversation);
    void deleteConversation(String id);
    default void deleteConversation(Conversation conversation) {
        if (conversation != null) deleteConversation(conversation.id);
    }
}
