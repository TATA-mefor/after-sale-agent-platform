package io.github.tatame.aftersale.agent.prompt;

public record PromptBudget(
        int systemPromptTokens,
        int historyTokens,
        int ragContextTokens,
        int toolCatalogTokens,
        int maxOutputTokens,
        int totalInputTokens) {

    public static PromptBudget defaults() {
        return new PromptBudget(2000, 4000, 8000, 2000, 1000, 16000);
    }

    public PromptBudget {
        requirePositive(systemPromptTokens, "systemPromptTokens");
        requirePositive(historyTokens, "historyTokens");
        requirePositive(ragContextTokens, "ragContextTokens");
        requirePositive(toolCatalogTokens, "toolCatalogTokens");
        requirePositive(maxOutputTokens, "maxOutputTokens");
        requirePositive(totalInputTokens, "totalInputTokens");
    }

    private static void requirePositive(int value, String fieldName) {
        if (value <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
    }
}
