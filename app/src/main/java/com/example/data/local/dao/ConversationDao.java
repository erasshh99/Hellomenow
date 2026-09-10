package com.example.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import com.example.data.local.entity.ConversationEntity;
import java.util.List;

@Dao
public interface ConversationDao {
    @Query("SELECT * FROM conversations ORDER BY isPinned DESC, updatedAt DESC")
    LiveData<List<ConversationEntity>> getAllConversations();

    @Query("SELECT * FROM conversations WHERE id = :id LIMIT 1")
    ConversationEntity getConversationByIdSync(String id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(ConversationEntity conversation);

    @Update
    void update(ConversationEntity conversation);

    @Query("DELETE FROM conversations WHERE id = :id")
    void deleteById(String id);
}
