package com.example.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "media_assets")
public class MediaAssetEntity {
    @PrimaryKey
    @NonNull
    public String id;
    public String conversationId;
    public long messageId;
    public String type; // IMAGE or VIDEO
    public String prompt;
    public String modelId;
    public String providerId;
    public String localUri;
    public String remoteUrl;
    public String mimeType;
    public int width;
    public int height;
    public long durationMs;
    public long sizeBytes;
    public long createdAt;

    public MediaAssetEntity(@NonNull String id) {
        this.id = id;
        this.createdAt = System.currentTimeMillis();
    }
}
