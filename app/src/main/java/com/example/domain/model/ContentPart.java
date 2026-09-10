package com.example.domain.model;

import java.io.Serializable;

public class ContentPart implements Serializable {
    public ContentPartType type = ContentPartType.TEXT;
    public String text;
    public String mediaUrl;
    public String localPath;
    public String mimeType;
    public String toolCallId;
    public String toolName;
    public String toolArgs;
    public boolean isTranscoded;
    public String transcript;

    public ContentPart() {}

    public static ContentPart text(String content) {
        ContentPart part = new ContentPart();
        part.type = ContentPartType.TEXT;
        part.text = content;
        return part;
    }

    public static ContentPart reasoning(String reasoning) {
        ContentPart part = new ContentPart();
        part.type = ContentPartType.REASONING;
        part.text = reasoning;
        return part;
    }

    public static ContentPart image(String url, String localPath) {
        return image(url, localPath, "image/jpeg");
    }

    public static ContentPart image(String url, String localPath, String mimeType) {
        ContentPart part = new ContentPart();
        part.type = ContentPartType.IMAGE;
        part.mediaUrl = url;
        part.localPath = localPath;
        part.mimeType = mimeType != null ? mimeType : "image/jpeg";
        return part;
    }

    public static ContentPart document(String localPath, String mimeType, String textContent) {
        ContentPart part = new ContentPart();
        part.type = ContentPartType.DOCUMENT;
        part.localPath = localPath;
        part.mimeType = mimeType;
        part.text = textContent;
        return part;
    }

    public static ContentPart video(String url, String localPath) {
        ContentPart part = new ContentPart();
        part.type = ContentPartType.VIDEO;
        part.mediaUrl = url;
        part.localPath = localPath;
        part.mimeType = "video/mp4";
        return part;
    }

    public static ContentPart error(String message) {
        ContentPart part = new ContentPart();
        part.type = ContentPartType.ERROR;
        part.text = message;
        return part;
    }
}
