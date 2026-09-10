package com.example.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import com.example.domain.model.CapabilitySet;
import com.example.domain.model.CapabilitySource;
import com.example.domain.model.SpeedTier;

@Entity(
    tableName = "models",
    indices = {@Index(value = {"providerId"})}
)
public class ModelEntity {
    @PrimaryKey
    @NonNull
    public String id;
    public String displayName;
    public String providerId;
    public String family;
    public String version;
    public int contextWindow = 8192;
    public int maxOutputTokens = 4096;
    public CapabilitySet capabilities;
    public double promptPricePerMillion;
    public double completionPricePerMillion;
    public String currency = "USD";
    public SpeedTier tier = SpeedTier.BALANCED;
    public boolean deprecated = false;
    public String rawJson = "";
    public long detectedAt = 0L;
    public int confidence = 0;
    public CapabilitySource source = CapabilitySource.HEURISTIC;
    public boolean isFavorite = false;

    public ModelEntity(@NonNull String id, String displayName, String providerId) {
        this.id = id;
        this.displayName = displayName;
        this.providerId = providerId;
    }
}
