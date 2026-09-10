package com.example.util;

public class TokenEstimator {
    /**
     * Approximate token calculation:
     * English: ~4 chars per token.
     * CJK / Arabic / Multilingual: ~1.5 to 2 chars per token.
     * Code: ~3 chars per token.
     */
    public static int estimateTokens(String text) {
        if (text == null || text.isEmpty()) return 0;
        int len = text.length();
        // A robust hybrid approximation: 3.5 characters per token + 1 token base
        return (int) Math.ceil(len / 3.5);
    }
}
