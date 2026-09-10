package com.example.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "conversations")
public class ConversationEntity {
    @PrimaryKey
    @NonNull
    public String id;
    public String title;
    public String currentModelId;
    public String currentProviderId;
    public String systemPrompt;
    public double temperature = 0.7;
    public double topP = 1.0;
    public int maxTokens = 4096;
    public long createdAt;
    public long updatedAt;
    public boolean isPinned = false;
    public boolean isArchived = false;

    public ConversationEntity(@NonNull String id, String title, String currentModelId, String currentProviderId) {
        this.id = id;
        this.title = title;
        this.currentModelId = currentModelId;
        this.currentProviderId = currentProviderId;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }
}
