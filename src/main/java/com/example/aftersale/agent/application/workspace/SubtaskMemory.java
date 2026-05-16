package com.example.aftersale.agent.application.workspace;

import com.example.aftersale.agent.application.planner.SubtaskStatus;
import com.example.aftersale.agent.application.planner.SubtaskType;
import java.util.List;
import java.util.Objects;

public record SubtaskMemory(
        String subtaskId,
        SubtaskType subtaskType,
        String target,
        SubtaskStatus status,
        String summary,
        List<String> relatedToolNames) {

    public SubtaskMemory {
        subtaskId = requireText(subtaskId, "subtaskId");
        subtaskType = Objects.requireNonNull(subtaskType, "subtaskType must not be null");
        target = requireText(target, "target");
        status = Objects.requireNonNull(status, "status must not be null");
        summary = requireText(summary, "summary");
        relatedToolNames = List.copyOf(Objects.requireNonNull(
                relatedToolNames,
                "relatedToolNames must not be null"));
    }

    public String summary() {
        return subtaskId + " " + subtaskType.name() + " " + status.name() + ": " + summary;
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
