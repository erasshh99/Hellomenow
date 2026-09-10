package com.example.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import java.util.Map;

@Entity(tableName = "providers")
public class ProviderEntity {
    @PrimaryKey
    @NonNull
    public String id;
    public String name;
    public String type;
    public String baseUrl;
    public String orgId;
    public Map<String, String> customHeaders;
    public int timeoutSeconds = 60;
    public boolean isEnabled = true;
    public boolean isLocal = false;
    public boolean useAiDetection = false;
    public String customBodyTemplate;
    public String customJsonPath;

    public ProviderEntity(@NonNull String id, String name, String type, String baseUrl) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.baseUrl = baseUrl;
    }
}
