package com.example.aftersale.agent.prompt;

import java.util.Objects;

public record PromptSection(PromptSectionType type, String content) {

    public PromptSection {
        type = Objects.requireNonNull(type, "type must not be null");
        content = Objects.requireNonNullElse(content, "");
    }

    public int estimatedTokens() {
        return PromptTokenEstimator.estimate(content);
    }

    public PromptSection withContent(String newContent) {
        return new PromptSection(type, newContent);
    }
}
