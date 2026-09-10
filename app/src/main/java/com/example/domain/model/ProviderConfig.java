package com.example.domain.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class ProviderConfig implements Serializable {
    public String id;
    public String name;
    public String type; // OPENAI, ANTHROPIC, GEMINI, OPENAI_COMPATIBLE, AZURE_OPENAI, AWS_BEDROCK, RAW_CUSTOM
    public String baseUrl;
    public String apiKey; // Encrypted in storage, masked in UI
    public String orgId;
    public Map<String, String> customHeaders = new HashMap<>();
    public int timeoutSeconds = 60;
    public boolean isEnabled = true;
    public boolean enabled = true;
    public boolean isLocal = false;
    public boolean useAiDetection = false;
    public String customBodyTemplate;
    public String customJsonPath;

    public ProviderConfig() {}

    public ProviderConfig(String id, String name, String type, String baseUrl) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.baseUrl = baseUrl;
    }

    public String getMaskedApiKey() {
        if (apiKey == null || apiKey.isEmpty()) return "None";
        if (apiKey.length() <= 8) return "••••••••";
        return apiKey.substring(0, 4) + "••••••••" + apiKey.substring(apiKey.length() - 4);
    }

    public String getMaskedKey() {
        return getMaskedApiKey();
    }
}
