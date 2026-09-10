package com.example.domain.model;

import java.io.Serializable;

public class Conversation implements Serializable {
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

    public Conversation() {
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    public Conversation(String id, String title, String modelId, String providerId) {
        this.id = id;
        this.title = title;
        this.currentModelId = modelId;
        this.currentProviderId = providerId;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }
}
