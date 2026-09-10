package com.example.domain.model;

import androidx.annotation.NonNull;
import java.io.Serializable;

public class ErrorDescriptor implements Serializable {
    public enum Kind {
        INVALID_MODEL,
        AUTH_FAILURE,
        UNKNOWN_ENDPOINT,
        PAYLOAD_TOO_LARGE,
        UNSUPPORTED_MODALITY,
        RATE_LIMIT,
        INSUFFICIENT_CREDITS,
        SERVER_ERROR,
        NETWORK_TIMEOUT,
        TLS_FAILURE,
        CLEARTEXT_BLOCKED,
        UNKNOWN
    }

    public int code;
    public Kind kind = Kind.UNKNOWN;
    public String userMessage;
    public String userFacingMessage;
    public boolean retryable;
    public long retryAfterMs;
    public String rawBody;

    public ErrorDescriptor() {}

    public ErrorDescriptor(int code, Kind kind, String userMessage, boolean retryable, long retryAfterMs, String rawBody) {
        this.code = code;
        this.kind = kind;
        this.userMessage = userMessage;
        this.userFacingMessage = userMessage;
        this.retryable = retryable;
        this.retryAfterMs = retryAfterMs;
        this.rawBody = rawBody;
    }

    @NonNull
    @Override
    public String toString() {
        return "[" + kind + " " + code + "] " + userMessage;
    }
}
