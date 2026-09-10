package com.example.domain.provider;

import com.example.domain.model.TokenUsage;
import java.io.Serializable;

public class ChatResult implements Serializable {
    public String content;
    public String reasoning;
    public TokenUsage usage;
    public String finishReason;

    public ChatResult() {}

    public ChatResult(String content, TokenUsage usage) {
        this.content = content;
        this.usage = usage;
    }

    public ChatResult(String content, String reasoning, TokenUsage usage) {
        this.content = content;
        this.reasoning = reasoning;
        this.usage = usage;
    }
}
