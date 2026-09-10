package com.example.data.discovery;

import android.content.Context;
import androidx.annotation.NonNull;
import com.example.domain.model.CapabilitySet;
import com.example.domain.model.CapabilitySource;
import com.example.domain.model.ModelDescriptor;
import com.example.domain.model.SpeedTier;
import com.example.domain.model.Ternary;
import com.example.domain.provider.CapabilityProbeResult;
import com.example.domain.provider.LlmProvider;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CapabilityDiscoveryEngine {
    private final Context context;
    private final Gson gson = new Gson();
    private final Map<String, CapabilitySet> registryCache = new HashMap<>();
    private final Map<String, JsonObject> registryMetadata = new HashMap<>();

    public CapabilityDiscoveryEngine(@NonNull Context context) {
        this.context = context.getApplicationContext();
        loadStaticRegistry();
    }

    private void loadStaticRegistry() {
        try (InputStream is = context.getAssets().open("capabilities_registry.json");
             InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            JsonArray models = root.getAsJsonArray("models");
            for (JsonElement el : models) {
                JsonObject m = el.getAsJsonObject();
                String id = m.get("id").getAsString().toLowerCase(Locale.US);
                CapabilitySet caps = parseCapsFromJson(m);
                registryCache.put(id, caps);
                registryMetadata.put(id, m);
            }
        } catch (Exception e) {
            // Log or ignore asset load error
        }
    }

    private CapabilitySet parseCapsFromJson(JsonObject m) {
        CapabilitySet c = new CapabilitySet();
        c.visionInput = Ternary.fromString(getStringOrNull(m, "visionInput"));
        c.documentInputPdf = Ternary.fromString(getStringOrNull(m, "documentInputPdf"));
        c.documentInputText = Ternary.fromString(getStringOrNull(m, "documentInputText"));
        c.audioInput = Ternary.fromString(getStringOrNull(m, "audioInput"));
        c.audioOutput = Ternary.fromString(getStringOrNull(m, "audioOutput"));
        c.imageGeneration = Ternary.fromString(getStringOrNull(m, "imageGeneration"));
        c.videoGeneration = Ternary.fromString(getStringOrNull(m, "videoGeneration"));
        c.functionCalling = Ternary.fromString(getStringOrNull(m, "functionCalling"));
        c.structuredJsonOutput = Ternary.fromString(getStringOrNull(m, "structuredJsonOutput"));
        c.streaming = Ternary.fromString(getStringOrNull(m, "streaming"));
        c.systemPromptSupport = Ternary.fromString(getStringOrNull(m, "systemPromptSupport"));
        c.reasoningTokens = Ternary.fromString(getStringOrNull(m, "reasoningTokens"));
        c.multiImagePerTurn = Ternary.fromString(getStringOrNull(m, "multiImagePerTurn"));
        c.nativeFileUpload = Ternary.fromString(getStringOrNull(m, "nativeFileUpload"));
        c.webSearchTool = Ternary.fromString(getStringOrNull(m, "webSearchTool"));
        c.codeExecutionTool = Ternary.fromString(getStringOrNull(m, "codeExecutionTool"));
        c.longContextAbove128k = Ternary.fromString(getStringOrNull(m, "longContextAbove128k"));
        return c;
    }

    private String getStringOrNull(JsonObject obj, String key) {
        if (obj.has(key) && !obj.get(key).isJsonNull()) {
            return obj.get(key).getAsString();
        }
        return null;
    }

    /**
     * Executes Stage 1 (Registry) and Stage 2 (Heuristics)
     */
    public ModelDescriptor discoverCapabilities(String modelId, String providerId, String rawJson) {
        String lowerId = modelId != null ? modelId.toLowerCase(Locale.US) : "";
        ModelDescriptor desc = new ModelDescriptor(modelId, modelId, providerId);
        desc.rawJson = rawJson != null ? rawJson : "";

        // Stage 1: Static Registry
        if (registryCache.containsKey(lowerId)) {
            desc.capabilities = registryCache.get(lowerId).copy();
            JsonObject meta = registryMetadata.get(lowerId);
            if (meta != null) {
                if (meta.has("displayName")) desc.displayName = meta.get("displayName").getAsString();
                if (meta.has("family")) desc.family = meta.get("family").getAsString();
                if (meta.has("contextWindow")) desc.contextWindow = meta.get("contextWindow").getAsInt();
                if (meta.has("maxOutputTokens")) desc.maxOutputTokens = meta.get("maxOutputTokens").getAsInt();
            }
            desc.confidence = 95;
            desc.source = CapabilitySource.REGISTRY;
            desc.detectedAt = System.currentTimeMillis();
            return desc;
        }

        // Check for partial matches in registry (e.g. "openai/gpt-4o" matches "gpt-4o")
        for (Map.Entry<String, CapabilitySet> entry : registryCache.entrySet()) {
            if (lowerId.endsWith(entry.getKey()) || lowerId.contains(entry.getKey())) {
                desc.capabilities = entry.getValue().copy();
                JsonObject meta = registryMetadata.get(entry.getKey());
                if (meta != null) {
                    if (meta.has("displayName")) desc.displayName = meta.get("displayName").getAsString();
                    if (meta.has("family")) desc.family = meta.get("family").getAsString();
                    if (meta.has("contextWindow")) desc.contextWindow = meta.get("contextWindow").getAsInt();
                    if (meta.has("maxOutputTokens")) desc.maxOutputTokens = meta.get("maxOutputTokens").getAsInt();
                }
                desc.confidence = 85;
                desc.source = CapabilitySource.REGISTRY;
                desc.detectedAt = System.currentTimeMillis();
                return desc;
            }
        }

        // Stage 2: Heuristic / Regex Classifier
        CapabilitySet heuristicCaps = runHeuristicClassifier(lowerId);
        desc.capabilities = heuristicCaps;
        desc.confidence = 65;
        desc.source = CapabilitySource.HEURISTIC;
        desc.detectedAt = System.currentTimeMillis();

        // Extract context window heuristics if pattern like "32k", "128k", "1m" exists
        if (lowerId.contains("1m") || lowerId.contains("2m")) {
            desc.contextWindow = 1048576;
            desc.capabilities.longContextAbove128k = Ternary.TRUE;
        } else if (lowerId.contains("128k") || lowerId.contains("200k")) {
            desc.contextWindow = 128000;
        } else if (lowerId.contains("64k")) {
            desc.contextWindow = 65536;
        } else if (lowerId.contains("32k")) {
            desc.contextWindow = 32768;
        } else if (lowerId.contains("16k")) {
            desc.contextWindow = 16384;
        }

        // Set speed tier
        if (lowerId.contains("flash") || lowerId.contains("mini") || lowerId.contains("haiku") || lowerId.contains("small")) {
            desc.tier = SpeedTier.FAST;
        } else if (lowerId.contains("r1") || lowerId.contains("o1") || lowerId.contains("o3") || lowerId.contains("thinking")) {
            desc.tier = SpeedTier.REASONING;
        } else if (lowerId.contains("pro") || lowerId.contains("opus") || lowerId.contains("large") || lowerId.contains("405b") || lowerId.contains("70b")) {
            desc.tier = SpeedTier.POWERFUL;
        } else {
            desc.tier = SpeedTier.BALANCED;
        }

        return desc;
    }

    private CapabilitySet runHeuristicClassifier(String id) {
        CapabilitySet caps = new CapabilitySet();
        caps.streaming = Ternary.TRUE;
        caps.systemPromptSupport = Ternary.TRUE;

        // Vision regex
        if (Pattern.compile("vision|\\bvl\\b|4o|gemini|claude-3|pixtral", Pattern.CASE_INSENSITIVE).matcher(id).find()) {
            caps.visionInput = Ternary.TRUE;
            caps.multiImagePerTurn = Ternary.TRUE;
            caps.documentInputPdf = Ternary.TRUE;
        } else {
            caps.visionInput = Ternary.FALSE;
            caps.documentInputPdf = Ternary.FALSE;
        }

        // Image Generation
        if (Pattern.compile("dall-e|imagen|flux|stable-diffusion|midjourney|image-gen", Pattern.CASE_INSENSITIVE).matcher(id).find()) {
            caps.imageGeneration = Ternary.TRUE;
            caps.streaming = Ternary.FALSE;
        } else {
            caps.imageGeneration = Ternary.FALSE;
        }

        // Video Generation
        if (Pattern.compile("sora|veo|kling|runway|gen-2|gen-3|gen-4|luma|pika|minimax-video", Pattern.CASE_INSENSITIVE).matcher(id).find()) {
            caps.videoGeneration = Ternary.TRUE;
            caps.streaming = Ternary.FALSE;
        } else {
            caps.videoGeneration = Ternary.FALSE;
        }

        // Reasoning
        if (Pattern.compile("\\bo1\\b|\\bo3\\b|\\br1\\b|reason|thinking", Pattern.CASE_INSENSITIVE).matcher(id).find()) {
            caps.reasoningTokens = Ternary.TRUE;
        } else {
            caps.reasoningTokens = Ternary.FALSE;
        }

        // Audio
        if (Pattern.compile("whisper|tts|audio|realtime", Pattern.CASE_INSENSITIVE).matcher(id).find()) {
            caps.audioInput = Ternary.TRUE;
        }

        // Function calling / Tools
        if (Pattern.compile("gpt-4|gpt-3.5|claude-3|gemini|mistral-large|qwen", Pattern.CASE_INSENSITIVE).matcher(id).find()
                && !caps.imageGeneration.isTrue() && !caps.videoGeneration.isTrue()) {
            caps.functionCalling = Ternary.TRUE;
            caps.structuredJsonOutput = Ternary.TRUE;
        }

        return caps;
    }

    /**
     * Stage 4: Live probing
     */
    public ModelDescriptor applyProbeResult(ModelDescriptor descriptor, CapabilityProbeResult probeResult) {
        if (probeResult != null && probeResult.success && probeResult.updatedCapabilities != null) {
            descriptor.capabilities = probeResult.updatedCapabilities;
            descriptor.confidence = Math.max(descriptor.confidence, probeResult.confidence);
            descriptor.source = CapabilitySource.PROBED;
            descriptor.detectedAt = System.currentTimeMillis();
        }
        return descriptor;
    }
}
