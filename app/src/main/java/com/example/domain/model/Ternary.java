package com.example.domain.model;

public enum Ternary {
    TRUE("Supported"),
    FALSE("Unsupported"),
    UNKNOWN("Unknown");

    public final String label;

    Ternary(String label) {
        this.label = label;
    }

    public boolean isTrue() {
        return this == TRUE;
    }

    public boolean isFalse() {
        return this == FALSE;
    }

    public boolean isUnknown() {
        return this == UNKNOWN;
    }

    public static Ternary fromBoolean(Boolean b) {
        if (b == null) return UNKNOWN;
        return b ? TRUE : FALSE;
    }

    public static Ternary fromString(String val) {
        if (val == null) return UNKNOWN;
        String s = val.trim().toUpperCase();
        if ("TRUE".equals(s) || "1".equals(s) || "YES".equals(s)) return TRUE;
        if ("FALSE".equals(s) || "0".equals(s) || "NO".equals(s)) return FALSE;
        return UNKNOWN;
    }
}
