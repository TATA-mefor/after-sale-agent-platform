package io.github.tatame.aftersale.common.observability.correlation;

import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Sanitizes request correlation identifiers before they enter response headers or MDC.
 */
public final class CorrelationIds {

    public static final int MAX_LENGTH = 128;

    private static final Pattern SAFE_ID_PATTERN = Pattern.compile("[A-Za-z0-9._:-]{1," + MAX_LENGTH + "}");
    private static final Pattern WINDOWS_DRIVE_PATH_PATTERN = Pattern.compile("^[A-Za-z]:.*");

    private CorrelationIds() {
    }

    public static String safeOrGenerated(String candidate) {
        if (isSafe(candidate)) {
            return candidate;
        }
        return UUID.randomUUID().toString();
    }

    public static boolean isSafe(String candidate) {
        if (candidate == null || candidate.isBlank() || candidate.length() > MAX_LENGTH) {
            return false;
        }
        if (!SAFE_ID_PATTERN.matcher(candidate).matches()) {
            return false;
        }
        if (containsUnsafeCharacter(candidate) || isUrlLike(candidate) || isPathLike(candidate)) {
            return false;
        }
        return !isSecretLike(candidate);
    }

    private static boolean containsUnsafeCharacter(String candidate) {
        for (int index = 0; index < candidate.length(); index++) {
            char value = candidate.charAt(index);
            if (Character.isWhitespace(value) || Character.isISOControl(value)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isUrlLike(String candidate) {
        String lower = candidate.toLowerCase(Locale.ROOT);
        return lower.contains("://")
                || lower.startsWith("http:")
                || lower.startsWith("https:")
                || lower.startsWith("jdbc:")
                || lower.startsWith("file:")
                || lower.startsWith("mailto:");
    }

    private static boolean isPathLike(String candidate) {
        return candidate.contains("/")
                || candidate.contains("\\")
                || candidate.startsWith("..")
                || WINDOWS_DRIVE_PATH_PATTERN.matcher(candidate).matches();
    }

    private static boolean isSecretLike(String candidate) {
        String lower = candidate.toLowerCase(Locale.ROOT);
        return lower.contains("token")
                || lower.contains("secret")
                || lower.contains("password")
                || lower.contains("api_key")
                || lower.contains("apikey")
                || lower.contains("api-key")
                || lower.contains("bearer")
                || lower.contains("authorization")
                || lower.contains("sk-");
    }
}
