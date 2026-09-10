package com.example.domain.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UnifiedMessage implements Serializable {
    public long id;
    public String conversationId;
    public Role role = Role.USER;
    public List<ContentPart> parts = new ArrayList<>();
    public String sourceModelId;
    public String sourceProviderId;
    public long createdAt;
    public TokenUsage usage = new TokenUsage();
    public TokenUsage tokenUsage = usage;
    public int status = MessageStatus.DONE;
    public long latencyMs = 0;
    public ErrorDescriptor error;
    public Map<String, String> metadata = new HashMap<>();

    public static final class Status {
        public static final int STREAMING = 1;
        public static final int SUCCESS = 2;
        public static final int FAILED = 3;
    }

    public void setTextContent(String text) {
        for (int i = parts.size() - 1; i >= 0; i--) {
            if (parts.get(i).type == ContentPartType.TEXT) {
                parts.remove(i);
            }
        }
        if (text != null) {
            parts.add(0, ContentPart.text(text));
        }
    }

    public void setReasoningContent(String text) {
        for (int i = parts.size() - 1; i >= 0; i--) {
            if (parts.get(i).type == ContentPartType.REASONING) {
                parts.remove(i);
            }
        }
        if (text != null) {
            parts.add(ContentPart.reasoning(text));
        }
    }

    public UnifiedMessage() {
        this.createdAt = System.currentTimeMillis();
    }

    public UnifiedMessage(String conversationId, Role role, String text) {
        this.conversationId = conversationId;
        this.role = role;
        this.createdAt = System.currentTimeMillis();
        if (text != null) {
            this.parts.add(ContentPart.text(text));
        }
    }

    public String getTextContent() {
        StringBuilder sb = new StringBuilder();
        for (ContentPart part : parts) {
            if (part.type == ContentPartType.TEXT && part.text != null) {
                sb.append(part.text);
            }
        }
        return sb.toString();
    }

    public String getReasoningContent() {
        StringBuilder sb = new StringBuilder();
        for (ContentPart part : parts) {
            if (part.type == ContentPartType.REASONING && part.text != null) {
                sb.append(part.text);
            }
        }
        return sb.toString();
    }

    public void appendDelta(String delta) {
        if (delta == null || delta.isEmpty()) return;
        for (int i = parts.size() - 1; i >= 0; i--) {
            ContentPart p = parts.get(i);
            if (p.type == ContentPartType.TEXT) {
                p.text = (p.text != null ? p.text : "") + delta;
                return;
            }
        }
        parts.add(ContentPart.text(delta));
    }

    public void appendReasoningDelta(String delta) {
        if (delta == null || delta.isEmpty()) return;
        for (int i = parts.size() - 1; i >= 0; i--) {
            ContentPart p = parts.get(i);
            if (p.type == ContentPartType.REASONING) {
                p.text = (p.text != null ? p.text : "") + delta;
                return;
            }
        }
        parts.add(ContentPart.reasoning(delta));
    }
}
