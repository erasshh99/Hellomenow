package com.example.presentation.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import com.example.di.AppContainer;
import com.example.domain.model.Conversation;
import java.util.List;
import java.util.UUID;

public class ConversationsViewModel extends ViewModel {
    private final AppContainer container;
    private final LiveData<List<Conversation>> conversations;

    public ConversationsViewModel(@NonNull AppContainer container) {
        this.container = container;
        this.conversations = container.getConversationRepository().getAllConversations();
    }

    public LiveData<List<Conversation>> getConversations() {
        return conversations;
    }

    public void createConversation(String title, String modelId, String providerId, OnCreatedCallback callback) {
        container.getExecutors().diskIO().execute(() -> {
            String id = UUID.randomUUID().toString();
            Conversation c = new Conversation(id, title, modelId, providerId);
            container.getConversationRepository().insertConversation(c);
            container.getExecutors().mainThread().execute(() -> callback.onCreated(c));
        });
    }

    public void deleteConversation(Conversation c) {
        container.getConversationRepository().deleteConversation(c);
    }

    public interface OnCreatedCallback {
        void onCreated(Conversation conversation);
    }
}
