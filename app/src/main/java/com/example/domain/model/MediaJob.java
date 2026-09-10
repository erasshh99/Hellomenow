package com.example.domain.model;

import java.io.Serializable;

public class MediaJob implements Serializable {
    public static final String STATUS_QUEUED = "QUEUED";
    public static final String STATUS_PROCESSING = "PROCESSING";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_CANCELLED = "CANCELLED";

    public String jobId;
    public String conversationId;
    public long messageId;
    public String type; // IMAGE or VIDEO
    public String providerId;
    public String modelId;
    public String prompt;
    public String status = STATUS_QUEUED;
    public int progressPercent = 0;
    public String pollUrl;
    public String resultUrl;
    public String localFilePath;
    public String errorMessage;
    public long createdAt;
    public long updatedAt;

    public MediaJob() {
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    public MediaJob(String jobId, String conversationId, long messageId, String modelId, String providerId, String prompt) {
        this.jobId = jobId;
        this.conversationId = conversationId;
        this.messageId = messageId;
        this.modelId = modelId;
        this.providerId = providerId;
        this.prompt = prompt;
        this.type = "VIDEO";
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }
}
