package com.example.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;
import com.example.data.local.dao.MessageDao;
import com.example.data.local.entity.MessageEntity;
import com.example.domain.model.UnifiedMessage;
import com.example.domain.repository.MessageRepository;
import com.example.util.AppExecutors;
import java.util.ArrayList;
import java.util.List;

public class MessageRepositoryImpl implements MessageRepository {
    private final MessageDao messageDao;
    private final AppExecutors executors;

    public MessageRepositoryImpl(MessageDao messageDao, AppExecutors executors) {
        this.messageDao = messageDao;
        this.executors = executors;
    }

    @Override
    public LiveData<List<UnifiedMessage>> getMessagesForConversation(String conversationId) {
        return Transformations.map(messageDao.getMessagesForConversation(conversationId), entities -> {
            List<UnifiedMessage> list = new ArrayList<>();
            if (entities != null) {
                for (MessageEntity e : entities) {
                    list.add(toDomain(e));
                }
            }
            return list;
        });
    }

    @Override
    public List<UnifiedMessage> getMessagesForConversationSync(String conversationId) {
        List<MessageEntity> entities = messageDao.getMessagesForConversationSync(conversationId);
        List<UnifiedMessage> list = new ArrayList<>();
        if (entities != null) {
            for (MessageEntity e : entities) {
                list.add(toDomain(e));
            }
        }
        return list;
    }

    @Override
    public long insertMessage(UnifiedMessage message) {
        MessageEntity entity = toEntity(message);
        long id = messageDao.insert(entity);
        message.id = id;
        return id;
    }

    @Override
    public void updateMessage(UnifiedMessage message) {
        executors.diskIO().execute(() -> messageDao.update(toEntity(message)));
    }

    @Override
    public void deleteMessage(long messageId) {
        executors.diskIO().execute(() -> messageDao.deleteById(messageId));
    }

    @Override
    public void clearConversation(String conversationId) {
        executors.diskIO().execute(() -> messageDao.deleteByConversationId(conversationId));
    }

    private UnifiedMessage toDomain(MessageEntity e) {
        UnifiedMessage m = new UnifiedMessage();
        m.id = e.id;
        m.conversationId = e.conversationId;
        m.role = e.role;
        m.parts = e.parts != null ? e.parts : new ArrayList<>();
        m.sourceModelId = e.sourceModelId;
        m.sourceProviderId = e.sourceProviderId;
        m.createdAt = e.createdAt;
        m.usage = e.usage;
        m.status = e.status;
        m.metadata = e.metadata;
        return m;
    }

    private MessageEntity toEntity(UnifiedMessage m) {
        MessageEntity e = new MessageEntity();
        e.id = m.id;
        e.conversationId = m.conversationId;
        e.role = m.role;
        e.parts = m.parts;
        e.sourceModelId = m.sourceModelId;
        e.sourceProviderId = m.sourceProviderId;
        e.createdAt = m.createdAt;
        e.usage = m.usage;
        e.status = m.status;
        e.metadata = m.metadata;
        return e;
    }
}
