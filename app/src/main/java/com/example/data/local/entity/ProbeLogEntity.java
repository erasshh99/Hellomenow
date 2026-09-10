package com.example.data.local.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "probe_logs")
public class ProbeLogEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public String modelId;
    public String providerId;
    public String probeType;
    public boolean success;
    public String evidence;
    public long durationMs;
    public long timestamp;

    public ProbeLogEntity() {
        this.timestamp = System.currentTimeMillis();
    }
}
