package com.example.domain.provider;

import java.io.Serializable;

public class ImageRequest implements Serializable {
    public String modelId;
    public String prompt;
    public int width = 1024;
    public int height = 1024;
    public int numImages = 1;
    public String quality = "standard";

    public ImageRequest() {}

    public ImageRequest(String prompt) {
        this.prompt = prompt;
    }

    public ImageRequest(String modelId, String prompt) {
        this.modelId = modelId;
        this.prompt = prompt;
    }
}
