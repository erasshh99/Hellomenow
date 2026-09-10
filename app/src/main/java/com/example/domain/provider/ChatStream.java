package com.example.domain.provider;

public interface ChatStream {
    void start(StreamListener listener);
    void cancel();
    boolean isCancelled();
}
