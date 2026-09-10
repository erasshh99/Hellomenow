package com.example.domain.model;

import java.io.Serializable;

public class TokenUsage implements Serializable {
    public int promptTokens;
    public int completionTokens;
    public int totalTokens;
    public int reasoningTokens;

    public TokenUsage() {}

    public TokenUsage(int promptTokens, int completionTokens, int totalTokens) {
        this.promptTokens = promptTokens;
        this.completionTokens = completionTokens;
        this.totalTokens = totalTokens;
    }

    public TokenUsage(int promptTokens, int completionTokens, int totalTokens, int reasoningTokens) {
        this.promptTokens = promptTokens;
        this.completionTokens = completionTokens;
        this.totalTokens = totalTokens;
        this.reasoningTokens = reasoningTokens;
    }
}
