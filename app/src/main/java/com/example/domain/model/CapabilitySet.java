package com.example.domain.model;

import androidx.annotation.NonNull;
import java.util.Objects;

public class CapabilitySet {
    public Ternary visionInput = Ternary.UNKNOWN;
    public Ternary documentInputPdf = Ternary.UNKNOWN;
    public Ternary documentInputText = Ternary.UNKNOWN;
    public Ternary audioInput = Ternary.UNKNOWN;
    public Ternary audioOutput = Ternary.UNKNOWN;
    public Ternary imageGeneration = Ternary.UNKNOWN;
    public Ternary videoGeneration = Ternary.UNKNOWN;
    public Ternary functionCalling = Ternary.UNKNOWN;
    public Ternary structuredJsonOutput = Ternary.UNKNOWN;
    public Ternary streaming = Ternary.UNKNOWN;
    public Ternary systemPromptSupport = Ternary.UNKNOWN;
    public Ternary reasoningTokens = Ternary.UNKNOWN;
    public Ternary multiImagePerTurn = Ternary.UNKNOWN;
    public Ternary nativeFileUpload = Ternary.UNKNOWN;
    public Ternary webSearchTool = Ternary.UNKNOWN;
    public Ternary codeExecutionTool = Ternary.UNKNOWN;
    public Ternary longContextAbove128k = Ternary.UNKNOWN;

    public CapabilitySet() {}

    public CapabilitySet copy() {
        CapabilitySet c = new CapabilitySet();
        c.visionInput = this.visionInput;
        c.documentInputPdf = this.documentInputPdf;
        c.documentInputText = this.documentInputText;
        c.audioInput = this.audioInput;
        c.audioOutput = this.audioOutput;
        c.imageGeneration = this.imageGeneration;
        c.videoGeneration = this.videoGeneration;
        c.functionCalling = this.functionCalling;
        c.structuredJsonOutput = this.structuredJsonOutput;
        c.streaming = this.streaming;
        c.systemPromptSupport = this.systemPromptSupport;
        c.reasoningTokens = this.reasoningTokens;
        c.multiImagePerTurn = this.multiImagePerTurn;
        c.nativeFileUpload = this.nativeFileUpload;
        c.webSearchTool = this.webSearchTool;
        c.codeExecutionTool = this.codeExecutionTool;
        c.longContextAbove128k = this.longContextAbove128k;
        return c;
    }

    /**
     * Merge with another capability set if flags are UNKNOWN in this one
     */
    public void mergeFallback(CapabilitySet fallback) {
        if (fallback == null) return;
        if (this.visionInput.isUnknown()) this.visionInput = fallback.visionInput;
        if (this.documentInputPdf.isUnknown()) this.documentInputPdf = fallback.documentInputPdf;
        if (this.documentInputText.isUnknown()) this.documentInputText = fallback.documentInputText;
        if (this.audioInput.isUnknown()) this.audioInput = fallback.audioInput;
        if (this.audioOutput.isUnknown()) this.audioOutput = fallback.audioOutput;
        if (this.imageGeneration.isUnknown()) this.imageGeneration = fallback.imageGeneration;
        if (this.videoGeneration.isUnknown()) this.videoGeneration = fallback.videoGeneration;
        if (this.functionCalling.isUnknown()) this.functionCalling = fallback.functionCalling;
        if (this.structuredJsonOutput.isUnknown()) this.structuredJsonOutput = fallback.structuredJsonOutput;
        if (this.streaming.isUnknown()) this.streaming = fallback.streaming;
        if (this.systemPromptSupport.isUnknown()) this.systemPromptSupport = fallback.systemPromptSupport;
        if (this.reasoningTokens.isUnknown()) this.reasoningTokens = fallback.reasoningTokens;
        if (this.multiImagePerTurn.isUnknown()) this.multiImagePerTurn = fallback.multiImagePerTurn;
        if (this.nativeFileUpload.isUnknown()) this.nativeFileUpload = fallback.nativeFileUpload;
        if (this.webSearchTool.isUnknown()) this.webSearchTool = fallback.webSearchTool;
        if (this.codeExecutionTool.isUnknown()) this.codeExecutionTool = fallback.codeExecutionTool;
        if (this.longContextAbove128k.isUnknown()) this.longContextAbove128k = fallback.longContextAbove128k;
    }

    @NonNull
    @Override
    public String toString() {
        return "CapabilitySet{" +
                "vision=" + visionInput +
                ", docPdf=" + documentInputPdf +
                ", imageGen=" + imageGeneration +
                ", videoGen=" + videoGeneration +
                ", tools=" + functionCalling +
                ", stream=" + streaming +
                '}';
    }
}
