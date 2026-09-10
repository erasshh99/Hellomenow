package com.example.data.transcoder;

import com.example.domain.model.CapabilitySet;
import com.example.domain.model.ContentPart;
import com.example.domain.model.ContentPartType;
import com.example.domain.model.ModelDescriptor;
import com.example.domain.model.Role;
import com.example.domain.model.UnifiedMessage;
import com.example.util.TokenEstimator;
import java.util.ArrayList;
import java.util.List;

public class ContextTranscoder {

    public static class TranscodeResult {
        public List<UnifiedMessage> messages;
        public String effectiveSystemPrompt;
        public int estimatedTokens;
        public boolean wasTruncated;
        public List<String> warnings = new ArrayList<>();
    }

    public static TranscodeResult transcodeForModel(
            List<UnifiedMessage> originalHistory,
            String conversationSystemPrompt,
            ModelDescriptor targetModel
    ) {
        TranscodeResult result = new TranscodeResult();
        CapabilitySet caps = targetModel.capabilities;
        boolean supportsVision = caps != null && caps.visionInput.isTrue();
        boolean supportsPdf = caps != null && caps.documentInputPdf.isTrue();
        boolean supportsSystem = caps != null && !caps.systemPromptSupport.isFalse();
        boolean supportsReasoning = caps != null && caps.reasoningTokens.isTrue();

        int maxContextWindow = targetModel.contextWindow > 0 ? targetModel.contextWindow : 8192;
        int maxInputBudget = Math.max(1024, maxContextWindow - targetModel.maxOutputTokens - 512);

        List<UnifiedMessage> transformed = new ArrayList<>();
        String effectiveSysPrompt = conversationSystemPrompt;

        // If system prompt is not supported natively, fold into user turn 0
        if (!supportsSystem && effectiveSysPrompt != null && !effectiveSysPrompt.isEmpty()) {
            result.warnings.add("System prompt folded into first user turn (target lacks system role support)");
        } else {
            result.effectiveSystemPrompt = effectiveSysPrompt;
        }

        for (int i = 0; i < originalHistory.size(); i++) {
            UnifiedMessage orig = originalHistory.get(i);
            UnifiedMessage copy = new UnifiedMessage();
            copy.id = orig.id;
            copy.conversationId = orig.conversationId;
            copy.role = orig.role;
            copy.createdAt = orig.createdAt;
            copy.sourceModelId = orig.sourceModelId;
            copy.sourceProviderId = orig.sourceProviderId;

            List<ContentPart> degradedParts = new ArrayList<>();
            for (ContentPart part : orig.parts) {
                if (part.type == ContentPartType.IMAGE) {
                    if (supportsVision) {
                        degradedParts.add(part);
                    } else {
                        // Vision degradation: descriptive text fallback
                        String fallbackText = "[Attached Image: " + (part.localPath != null ? part.localPath : "image") + 
                                (part.transcript != null ? " | OCR/Desc: " + part.transcript : " (model does not support vision input)") + "]";
                        degradedParts.add(ContentPart.text(fallbackText));
                        result.warnings.add("Image degraded to descriptive text (target model lacks vision capability)");
                    }
                } else if (part.type == ContentPartType.DOCUMENT) {
                    if (supportsPdf || (part.mimeType != null && part.mimeType.contains("text"))) {
                        degradedParts.add(part);
                    } else {
                        String textFallback = part.text != null && !part.text.isEmpty() 
                                ? "[Document Extract: " + part.text + "]"
                                : "[Attached Document: " + part.localPath + " (model cannot process raw PDF binary)]";
                        degradedParts.add(ContentPart.text(textFallback));
                        result.warnings.add("Document degraded to text extract (target model lacks PDF input capability)");
                    }
                } else if (part.type == ContentPartType.REASONING) {
                    if (supportsReasoning) {
                        degradedParts.add(part);
                    } else {
                        // Fold into thinking block or omit from history
                        if (part.text != null && !part.text.isEmpty()) {
                            degradedParts.add(ContentPart.text("<thought>\n" + part.text + "\n</thought>"));
                        }
                    }
                } else {
                    degradedParts.add(part);
                }
            }

            copy.parts = degradedParts;
            transformed.add(copy);
        }

        // If system prompt needed folding into first user message
        if (!supportsSystem && effectiveSysPrompt != null && !effectiveSysPrompt.isEmpty()) {
            if (!transformed.isEmpty()) {
                UnifiedMessage firstMsg = transformed.get(0);
                firstMsg.parts.add(0, ContentPart.text("[System Instructions:\n" + effectiveSysPrompt + "\n]\n\n"));
            } else {
                UnifiedMessage sysUserMsg = new UnifiedMessage(null, Role.USER, "[System Instructions:\n" + effectiveSysPrompt + "\n]");
                transformed.add(sysUserMsg);
            }
        }

        // Sliding window token budgeting
        int totalTokens = 0;
        for (UnifiedMessage m : transformed) {
            totalTokens += TokenEstimator.estimateTokens(m.getTextContent());
        }
        if (result.effectiveSystemPrompt != null) {
            totalTokens += TokenEstimator.estimateTokens(result.effectiveSystemPrompt);
        }

        while (totalTokens > maxInputBudget && transformed.size() > 1) {
            // Drop the oldest non-system message
            UnifiedMessage dropped = transformed.remove(0);
            int droppedTokens = TokenEstimator.estimateTokens(dropped.getTextContent());
            totalTokens -= droppedTokens;
            result.wasTruncated = true;
        }

        if (result.wasTruncated) {
            result.warnings.add("Older conversation context truncated to fit within " + maxInputBudget + " input token budget");
        }

        result.messages = transformed;
        result.estimatedTokens = totalTokens;
        return result;
    }
}
