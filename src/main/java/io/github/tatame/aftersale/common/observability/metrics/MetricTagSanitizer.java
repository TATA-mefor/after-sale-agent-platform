package io.github.tatame.aftersale.common.observability.metrics;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Keeps metric tags low-cardinality and free of secrets, paths, prompts, queries, and identifiers.
 */
final class MetricTagSanitizer {

    private static final String UNKNOWN = "unknown";
    private static final int MAX_TAG_LENGTH = 48;
    private static final Pattern SAFE_VALUE = Pattern.compile("[a-z0-9][a-z0-9_-]{0,47}");
    private static final Pattern SENSITIVE_VALUE = Pattern.compile(
            "(?i).*(api[_ -]?key|password|token|secret|bearer|prompt|query|snippet|jdbc:|sk-|://|@|\\\\|/).*");

    private MetricTagSanitizer() {
    }

    static String sanitize(String value) {
        if (value == null || value.isBlank()) {
            return UNKNOWN;
        }
        String normalized = value.trim()
                .replace(' ', '_')
                .replace('.', '_')
                .toLowerCase(Locale.ROOT);
        if (normalized.length() > MAX_TAG_LENGTH || SENSITIVE_VALUE.matcher(normalized).matches()) {
            return UNKNOWN;
        }
        if (!SAFE_VALUE.matcher(normalized).matches()) {
            return UNKNOWN;
        }
        return normalized;
    }

    static String sanitize(Enum<?> value) {
        if (value == null) {
            return UNKNOWN;
        }
        return sanitize(value.name());
    }

    static String booleanTag(boolean value) {
        return Boolean.toString(value);
    }
}
