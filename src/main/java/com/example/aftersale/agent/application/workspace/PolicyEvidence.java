package com.example.aftersale.agent.application.workspace;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.lang.Nullable;

/**
 * 保存政策检索工具返回的可引用证据。
 *
 * <p>边界：证据用于 AgentRun 内部汇总和解释，不改变政策库内容，也不代表最终审批结论。
 */
public record PolicyEvidence(
        String evidenceId,
        String policyId,
        String documentId,
        String chunkId,
        String documentTitle,
        String category,
        String productType,
        String snippet,
        String matchReason,
        @Nullable
        Double score,
        String retrievalMode,
        String source,
        String sourceToolName,
        String subtaskId) {

    public PolicyEvidence {
        evidenceId = optionalText(evidenceId);
        policyId = optionalText(policyId);
        documentId = optionalText(documentId);
        chunkId = optionalText(chunkId);
        documentTitle = optionalText(documentTitle);
        category = requireText(sanitize(category), "category");
        productType = optionalText(productType);
        snippet = requireText(sanitize(snippet), "snippet");
        matchReason = requireText(sanitize(matchReason), "matchReason");
        if (score != null && (score < 0.0d || score > 1.0d)) {
            throw new IllegalArgumentException("score must be between 0.0 and 1.0");
        }
        retrievalMode = optionalText(retrievalMode);
        source = optionalText(source);
        sourceToolName = requireText(sourceToolName, "sourceToolName");
        subtaskId = subtaskId == null ? "" : subtaskId;
    }

    public PolicyEvidence(
            String policyId,
            String category,
            String snippet,
            String matchReason,
            String sourceToolName,
            String subtaskId) {
        this(
                "",
                policyId,
                "",
                "",
                "",
                category,
                "",
                snippet,
                matchReason,
                null,
                "",
                "",
                sourceToolName,
                subtaskId);
    }

    public static List<PolicyEvidence> fromToolData(
            String sourceToolName,
            String subtaskId,
            Map<String, Object> data) {
        Objects.requireNonNull(data, "data must not be null");
        Object evidences = data.get("evidences");
        if (evidences instanceof List<?> evidenceList) {
            return fromEvidenceList(sourceToolName, subtaskId, evidenceList);
        }
        Object results = data.get("results");
        if (results instanceof List<?> resultList) {
            return fromLegacyResultList(sourceToolName, subtaskId, resultList);
        }
        return List.of();
    }

    public String summary() {
        String mode = retrievalMode.isBlank() ? "KEYWORD" : retrievalMode;
        String id = firstPresent(
                idPart("policy", policyId),
                idPart("chunk", chunkId),
                idPart("document", documentId),
                idPart("evidence", evidenceId));
        String scorePart = score == null ? "" : " / score=" + String.format("%.2f", score);
        String titlePart = documentTitle.isBlank() ? "" : " / title=" + documentTitle;
        return "Policy evidence[" + mode + "]: " + category
                + id
                + scorePart
                + titlePart
                + " / " + snippet;
    }

    private static List<PolicyEvidence> fromEvidenceList(
            String sourceToolName,
            String subtaskId,
            List<?> evidenceList) {
        if (evidenceList.isEmpty()) {
            return List.of();
        }
        return evidenceList.stream()
                .filter(Map.class::isInstance)
                .map(Map.class::cast)
                .map(evidence -> new PolicyEvidence(
                        optional(evidence, "evidenceId"),
                        optional(evidence, "policyId"),
                        optional(evidence, "documentId"),
                        optional(evidence, "chunkId"),
                        optional(evidence, "documentTitle"),
                        text(evidence, "category"),
                        optional(evidence, "productType"),
                        text(evidence, "snippet"),
                        text(evidence, "source"),
                        score(evidence, "score"),
                        optional(evidence, "retrievalMode"),
                        optional(evidence, "source"),
                        sourceToolName,
                        subtaskId))
                .toList();
    }

    private static List<PolicyEvidence> fromLegacyResultList(
            String sourceToolName,
            String subtaskId,
            List<?> resultList) {
        if (resultList.isEmpty()) {
            return List.of();
        }
        return resultList.stream()
                .filter(Map.class::isInstance)
                .map(Map.class::cast)
                .map(result -> new PolicyEvidence(
                        optional(result, "evidenceId"),
                        optional(result, "policyId"),
                        optional(result, "documentId"),
                        optional(result, "chunkId"),
                        optional(result, "documentTitle"),
                        text(result, "category"),
                        optional(result, "productType"),
                        text(result, "matchedText"),
                        text(result, "matchReason"),
                        score(result, "score"),
                        optional(result, "retrievalMode"),
                        optional(result, "source"),
                        sourceToolName,
                        subtaskId))
                .toList();
    }

    private static String text(Map<?, ?> data, String key) {
        Objects.requireNonNull(data, "data must not be null");
        Object value = data.get(key);
        if (value == null) {
            return "N/A";
        }
        return value.toString();
    }

    private static String optional(Map<?, ?> data, String key) {
        Objects.requireNonNull(data, "data must not be null");
        Object value = data.get(key);
        if (value == null) {
            return "";
        }
        return value.toString();
    }

    @Nullable
    private static Double score(Map<?, ?> data, String key) {
        Objects.requireNonNull(data, "data must not be null");
        Object value = data.get(key);
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return Double.parseDouble(text);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private static String idPart(String label, String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return " / " + label + "=" + value;
    }

    private static String firstPresent(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private static String sanitize(String value) {
        if (value == null) {
            return "";
        }
        String sanitized = value
                .replaceAll("sk-[A-Za-z0-9_-]{8,}", "sk-***")
                .replaceAll("Bearer\\s+[A-Za-z0-9._~+/=-]+", "Bearer ***")
                .replaceAll("(?i)api[_-]?key\\s*[:=]\\s*[^\\s,;]+", "apiKey=***")
                .replaceAll("(?i)password\\s*[:=]\\s*[^\\s,;]+", "password=***")
                .replaceAll("(?i)token\\s*[:=]\\s*[^\\s,;]+", "token=***")
                .replaceAll("[A-Za-z]:\\\\[^\\s,;]+", "[local-path]")
                .replaceAll("\\s+", " ")
                .trim();
        if (sanitized.length() > 180) {
            return sanitized.substring(0, 177) + "...";
        }
        return sanitized;
    }

    private static String optionalText(String value) {
        return value == null ? "" : sanitize(value);
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
