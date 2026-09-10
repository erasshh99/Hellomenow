package com.example.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;
import com.example.data.local.dao.ConversationDao;
import com.example.data.local.entity.ConversationEntity;
import com.example.domain.model.Conversation;
import com.example.domain.repository.ConversationRepository;
import com.example.util.AppExecutors;
import java.util.ArrayList;
import java.util.List;

public class ConversationRepositoryImpl implements ConversationRepository {
    private final ConversationDao conversationDao;
    private final AppExecutors executors;

    public ConversationRepositoryImpl(ConversationDao conversationDao, AppExecutors executors) {
        this.conversationDao = conversationDao;
        this.executors = executors;
    }

    @Override
    public LiveData<List<Conversation>> getConversations() {
        return Transformations.map(conversationDao.getAllConversations(), entities -> {
            List<Conversation> list = new ArrayList<>();
            if (entities != null) {
                for (ConversationEntity e : entities) {
                    list.add(toDomain(e));
                }
            }
            return list;
        });
    }

    @Override
    public Conversation getConversationByIdSync(String id) {
        ConversationEntity entity = conversationDao.getConversationByIdSync(id);
        return entity != null ? toDomain(entity) : null;
    }

    @Override
    public void insertConversation(Conversation conversation) {
        executors.diskIO().execute(() -> conversationDao.insert(toEntity(conversation)));
    }

    @Override
    public void updateConversation(Conversation conversation) {
        executors.diskIO().execute(() -> conversationDao.update(toEntity(conversation)));
    }

    @Override
    public void deleteConversation(String id) {
        executors.diskIO().execute(() -> conversationDao.deleteById(id));
    }

    private Conversation toDomain(ConversationEntity e) {
        Conversation c = new Conversation(e.id, e.title, e.currentModelId, e.currentProviderId);
        c.systemPrompt = e.systemPrompt;
        c.temperature = e.temperature;
        c.topP = e.topP;
        c.maxTokens = e.maxTokens;
        c.createdAt = e.createdAt;
        c.updatedAt = e.updatedAt;
        c.isPinned = e.isPinned;
        c.isArchived = e.isArchived;
        return c;
    }

    private ConversationEntity toEntity(Conversation c) {
        ConversationEntity e = new ConversationEntity(c.id, c.title, c.currentModelId, c.currentProviderId);
        e.systemPrompt = c.systemPrompt;
        e.temperature = c.temperature;
        e.topP = c.topP;
        e.maxTokens = c.maxTokens;
        e.createdAt = c.createdAt;
        e.updatedAt = c.updatedAt;
        e.isPinned = c.isPinned;
        e.isArchived = c.isArchived;
        return e;
    }
}
