package com.example.aftersale.agent.prompt;

import java.util.List;

public record PromptBuildResult(
        String systemPrompt,
        String userPrompt,
        PromptUsageTelemetry telemetry,
        List<PromptSection> sections) {

    public PromptBuildResult {
        sections = List.copyOf(sections);
    }
}
