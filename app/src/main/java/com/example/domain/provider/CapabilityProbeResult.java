package com.example.domain.provider;

import com.example.domain.model.CapabilitySet;
import java.io.Serializable;

public class CapabilityProbeResult implements Serializable {
    public String modelId;
    public boolean success;
    public CapabilitySet updatedCapabilities;
    public int confidence;
    public String probeEvidence;
    public long durationMs;
    public String errorMessage;
    public String notes;

    public CapabilityProbeResult() {}

    public CapabilityProbeResult(String modelId, boolean success, CapabilitySet updatedCapabilities, int confidence, String evidence) {
        this.modelId = modelId;
        this.success = success;
        this.updatedCapabilities = updatedCapabilities;
        this.confidence = confidence;
        this.probeEvidence = evidence;
        this.notes = evidence;
    }
}
