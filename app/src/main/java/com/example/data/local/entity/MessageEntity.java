package com.example.data.local.entity;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import com.example.domain.model.ContentPart;
import com.example.domain.model.Role;
import com.example.domain.model.TokenUsage;
import java.util.List;
import java.util.Map;

@Entity(
    tableName = "messages",
    indices = {@Index(value = {"conversationId", "createdAt"})}
)
public class MessageEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public String conversationId;
    public Role role;
    public List<ContentPart> parts;
    public String sourceModelId;
    public String sourceProviderId;
    public long createdAt;
    public TokenUsage usage;
    public int status;
    public Map<String, String> metadata;

    public MessageEntity() {
        this.createdAt = System.currentTimeMillis();
    }
}
