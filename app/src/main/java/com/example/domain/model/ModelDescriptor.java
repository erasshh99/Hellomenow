package com.example.domain.model;

import java.io.Serializable;

public class ModelDescriptor implements Serializable {
    public String id;
    public String displayName;
    public String providerId;
    public String family;
    public String version;
    public int contextWindow = 8192;
    public int maxOutputTokens = 4096;
    public CapabilitySet capabilities = new CapabilitySet();
    public PricingInfo pricing = new PricingInfo(0.0, 0.0, "USD");
    public SpeedTier tier = SpeedTier.BALANCED;
    public boolean deprecated = false;
    public String rawJson = "";
    public long detectedAt = 0L;
    public int confidence = 0; // 0..100
    public CapabilitySource source = CapabilitySource.HEURISTIC;
    public boolean isFavorite = false;

    public ModelDescriptor() {}

    public ModelDescriptor(String id, String displayName, String providerId) {
        this.id = id;
        this.displayName = displayName != null ? displayName : id;
        this.providerId = providerId;
    }

    public String getFriendlyName() {
        return displayName != null && !displayName.isEmpty() ? displayName : id;
    }
}
