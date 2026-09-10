package com.example.domain.provider;

import java.io.Serializable;

public class VideoRequest implements Serializable {
    public String modelId;
    public String prompt;
    public int durationSeconds = 5;
    public String resolution = "720p";
    public String aspectRatio = "16:9";

    public VideoRequest() {}

    public VideoRequest(String prompt) {
        this.prompt = prompt;
    }

    public VideoRequest(String modelId, String prompt) {
        this.modelId = modelId;
        this.prompt = prompt;
    }
}
