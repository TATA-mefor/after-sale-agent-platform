package io.github.tatame.aftersale.agent.prompt;

public record PromptUsageTelemetry(
        int systemPromptTokens,
        int plannerContractTokens,
        int toolCatalogTokens,
        int ticketContextTokens,
        int orderContextTokens,
        int historyTokens,
        int ragContextTokens,
        int optionalTokensDropped,
        int totalInputTokens,
        int maxOutputTokens,
        boolean budgetExceeded,
        String budgetAction,
        String outputTokens,
        String cacheReadTokens) {
}
