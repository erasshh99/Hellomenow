package com.example.domain.model;

import java.io.Serializable;

public class MediaAsset implements Serializable {
    public String id;
    public String conversationId;
    public long messageId;
    public String type; // IMAGE or VIDEO
    public String prompt;
    public String modelId;
    public String providerId;
    public String localUri;
    public String localPath;
    public String remoteUrl;
    public String mimeType;
    public int width;
    public int height;
    public long durationMs;
    public long sizeBytes;
    public long createdAt;

    public MediaAsset() {
        this.createdAt = System.currentTimeMillis();
    }
}
