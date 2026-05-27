package com.example.aftersale.policy.rag.ingestion.domain;

import java.util.Objects;
import java.util.regex.Pattern;

final class PolicyIngestionText {

    private static final int SAFE_TEXT_LIMIT = 500;
    private static final Pattern WINDOWS_PATH = Pattern.compile("[A-Za-z]:\\\\[^\\s]+");
    private static final Pattern SECRET_ASSIGNMENT = Pattern.compile(
            "(?i)(api[_-]?key|password|token|secret)\\s*[:=]\\s*[^\\s,;]+");
    private static final Pattern OPENAI_KEY = Pattern.compile("sk-[A-Za-z0-9_-]{8,}");
    private static final Pattern PROMPT_ASSIGNMENT = Pattern.compile("(?i)prompt\\s*[:=]\\s*[^\\n;]+");

    private PolicyIngestionText() {
    }

    static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        String normalized = value.trim();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }

    static String optionalText(String value, String fieldName) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank when provided");
        }
        return normalized;
    }

    static String metadata(String value) {
        if (value == null || value.isBlank()) {
            return "{}";
        }
        return value.trim();
    }

    static String sanitizedOptionalText(String value, String fieldName) {
        String normalized = optionalText(value, fieldName);
        if (normalized == null) {
            return null;
        }
        return sanitize(normalized);
    }

    static String sanitize(String value) {
        String sanitized = WINDOWS_PATH.matcher(value).replaceAll("[redacted-path]");
        sanitized = SECRET_ASSIGNMENT.matcher(sanitized).replaceAll("$1=[redacted]");
        sanitized = OPENAI_KEY.matcher(sanitized).replaceAll("[redacted-api-key]");
        sanitized = PROMPT_ASSIGNMENT.matcher(sanitized).replaceAll("prompt=[redacted]");
        if (sanitized.length() <= SAFE_TEXT_LIMIT) {
            return sanitized;
        }
        return sanitized.substring(0, SAFE_TEXT_LIMIT).trim();
    }
}
