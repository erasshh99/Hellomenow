package com.example.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
    tableName = "media_jobs",
    indices = {@Index(value = {"type", "status"})}
)
public class MediaJobEntity {
    @PrimaryKey
    @NonNull
    public String jobId;
    public String conversationId;
    public long messageId;
    public String type;
    public String providerId;
    public String modelId;
    public String prompt;
    public String status;
    public int progressPercent;
    public String pollUrl;
    public String resultUrl;
    public String localFilePath;
    public String errorMessage;
    public long createdAt;
    public long updatedAt;

    public MediaJobEntity(@NonNull String jobId) {
        this.jobId = jobId;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }
}
