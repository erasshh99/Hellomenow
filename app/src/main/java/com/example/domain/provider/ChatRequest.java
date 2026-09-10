package com.example.domain.provider;

import com.example.domain.model.UnifiedMessage;
import java.io.Serializable;
import java.util.List;

public class ChatRequest implements Serializable {
    public String modelId;
    public List<UnifiedMessage> messages;
    public String systemPrompt;
    public double temperature = 0.7;
    public double topP = 1.0;
    public int maxTokens = 4096;
    public boolean stream = true;

    public ChatRequest() {}

    public ChatRequest(String modelId, List<UnifiedMessage> messages, String systemPrompt, double temperature, double topP, int maxTokens, boolean stream) {
        this.modelId = modelId;
        this.messages = messages;
        this.systemPrompt = systemPrompt;
        this.temperature = temperature;
        this.topP = topP;
        this.maxTokens = maxTokens;
        this.stream = stream;
    }
}
