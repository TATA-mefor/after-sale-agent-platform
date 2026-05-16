package com.example.aftersale.agent.application.workspace;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public record PolicyEvidence(
        String policyId,
        String category,
        String snippet,
        String matchReason,
        String sourceToolName,
        String subtaskId) {

    public PolicyEvidence {
        policyId = requireText(policyId, "policyId");
        category = requireText(category, "category");
        snippet = requireText(snippet, "snippet");
        matchReason = requireText(matchReason, "matchReason");
        sourceToolName = requireText(sourceToolName, "sourceToolName");
        subtaskId = subtaskId == null ? "" : subtaskId;
    }

    public static List<PolicyEvidence> fromToolData(
            String sourceToolName,
            String subtaskId,
            Map<String, Object> data) {
        Object results = data.get("results");
        if (!(results instanceof List<?> resultList) || resultList.isEmpty()) {
            return List.of();
        }
        return resultList.stream()
                .filter(Map.class::isInstance)
                .map(Map.class::cast)
                .map(result -> new PolicyEvidence(
                        text(result, "policyId"),
                        text(result, "category"),
                        text(result, "matchedText"),
                        text(result, "matchReason"),
                        sourceToolName,
                        subtaskId))
                .toList();
    }

    public String summary() {
        return policyId + ": " + category + " (" + matchReason + ")";
    }

    private static String text(Map<?, ?> data, String key) {
        Objects.requireNonNull(data, "data must not be null");
        Object value = data.get(key);
        if (value == null) {
            return "N/A";
        }
        return value.toString();
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
