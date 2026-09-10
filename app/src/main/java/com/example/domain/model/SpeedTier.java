package com.example.domain.model;

public enum SpeedTier {
    FAST("⚡ Fast"),
    BALANCED("⚖ Balanced"),
    POWERFUL("🚀 Powerful"),
    REASONING("🧠 Deep Reasoning");

    public final String label;

    SpeedTier(String label) {
        this.label = label;
    }
}
