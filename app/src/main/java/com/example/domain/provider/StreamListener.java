package com.example.domain.provider;

import com.example.domain.model.ErrorDescriptor;
import com.example.domain.model.TokenUsage;

public interface StreamListener {
    void onDelta(String deltaText);
    void onReasoningDelta(String reasoningDelta);
    void onComplete(TokenUsage finalUsage);
    void onError(ErrorDescriptor error);
}
