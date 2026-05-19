package com.example.aftersale.agent.prompt;

import java.util.List;

public record PromptBudgetResult(List<PromptSection> sections, PromptUsageTelemetry telemetry) {

    public PromptBudgetResult {
        sections = List.copyOf(sections);
    }
}
