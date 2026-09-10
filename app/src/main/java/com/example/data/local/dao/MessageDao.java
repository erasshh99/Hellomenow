package com.example.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import com.example.data.local.entity.MessageEntity;
import java.util.List;

@Dao
public interface MessageDao {
    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY createdAt ASC")
    LiveData<List<MessageEntity>> getMessagesForConversation(String conversationId);

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY createdAt ASC")
    List<MessageEntity> getMessagesForConversationSync(String conversationId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(MessageEntity message);

    @Update
    void update(MessageEntity message);

    @Query("DELETE FROM messages WHERE id = :id")
    void deleteById(long id);

    @Query("DELETE FROM messages WHERE conversationId = :conversationId")
    void deleteByConversationId(String conversationId);
}
