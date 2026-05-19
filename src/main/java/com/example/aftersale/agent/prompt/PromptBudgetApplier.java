package com.example.aftersale.agent.prompt;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

public class PromptBudgetApplier {

    private static final int OPTIONAL_MIN_TOKEN_TARGET = 1;
    private static final int OPTIONAL_TARGET_TOKENS = 24;
    private static final int COMPRESSED_OPTIONAL_TOKENS = 16;
    private static final int COMPRESSED_TICKET_TOKENS = 32;

    private final PromptBudgetPolicy policy;

    public PromptBudgetApplier() {
        this(new PromptBudgetPolicy());
    }

    public PromptBudgetApplier(PromptBudgetPolicy policy) {
        this.policy = policy;
    }

    public PromptBudgetResult apply(List<PromptSection> inputSections, PromptBudget budget) {
        List<PromptSection> sections = new ArrayList<>(inputSections);
        validateCriticalSections(sections, budget);
        int originalOptionalTokens = optionalTokens(sections);
        StringJoiner actions = new StringJoiner(",");
        boolean budgetExceeded = totalTokens(sections) > budget.totalInputTokens();
        if (budgetExceeded) {
            for (PromptSectionType type : policy.reductionOrder()) {
                if (totalTokens(sections) <= budget.totalInputTokens()) {
                    break;
                }
                sections = reduce(sections, type, actions);
            }
        }
        if (totalTokens(sections) > budget.totalInputTokens()) {
            throw new PromptBudgetExceededException(
                    "Prompt input token budget exceeded after optional reductions: totalInputTokens="
                            + totalTokens(sections) + ", budget=" + budget.totalInputTokens());
        }
        int optionalTokensDropped = Math.max(0, originalOptionalTokens - optionalTokens(sections));
        String budgetAction = actions.length() == 0 ? "WITHIN_BUDGET" : actions.toString();
        return new PromptBudgetResult(
                sections,
                telemetry(sections, budget, optionalTokensDropped, budgetExceeded, budgetAction));
    }

    private static void validateCriticalSections(List<PromptSection> sections, PromptBudget budget) {
        int systemTokens = tokensFor(sections, PromptSectionType.SYSTEM_INSTRUCTIONS);
        if (systemTokens > budget.systemPromptTokens()) {
            throw new PromptBudgetExceededException(
                    "Critical section systemInstructions exceeds system prompt budget: "
                            + systemTokens + " > " + budget.systemPromptTokens());
        }
        int toolCatalogTokens = tokensFor(sections, PromptSectionType.TOOL_CATALOG_COMPACT);
        if (toolCatalogTokens > budget.toolCatalogTokens()) {
            throw new PromptBudgetExceededException(
                    "Critical section toolCatalogCompact exceeds tool catalog budget: "
                            + toolCatalogTokens + " > " + budget.toolCatalogTokens());
        }
        for (PromptSection section : sections) {
            if (section.type().isCritical() && section.content().isBlank()) {
                throw new PromptBudgetExceededException(
                        "Critical section " + section.type().sectionName() + " must not be blank");
            }
        }
    }

    private static List<PromptSection> reduce(
            List<PromptSection> sections,
            PromptSectionType type,
            StringJoiner actions) {
        List<PromptSection> result = new ArrayList<>();
        boolean applied = false;
        for (PromptSection section : sections) {
            if (section.type() != type) {
                result.add(section);
                continue;
            }
            applied = true;
            switch (type) {
                case DEBUG_HINTS -> {
                    actions.add("DROP_DEBUG_HINTS");
                }
                case NON_ESSENTIAL_DOCS -> {
                    actions.add("DROP_NON_ESSENTIAL_DOCS");
                }
                case EXAMPLES -> {
                    result.add(section.withContent(truncate(section.content(), OPTIONAL_TARGET_TOKENS)));
                    actions.add("TRUNCATE_EXAMPLES");
                }
                case CONVERSATION_HISTORY -> {
                    result.add(section.withContent(truncate(section.content(), OPTIONAL_TARGET_TOKENS)));
                    actions.add("TRUNCATE_CONVERSATION_HISTORY");
                }
                case RAG_CONTEXT -> {
                    result.add(section.withContent(compress(
                            "ragContext compressed to topKFinal=1",
                            section.content())));
                    actions.add("COMPRESS_RAG_CONTEXT");
                }
                case EXTENDED_POLICY_TEXT -> {
                    result.add(section.withContent(compress("extendedPolicyText summary only", section.content())));
                    actions.add("COMPRESS_EXTENDED_POLICY_TEXT");
                }
                case TICKET_CONTEXT -> {
                    result.add(section.withContent(compressTicketContext(section.content())));
                    actions.add("COMPRESS_TICKET_CONTEXT");
                }
                default -> result.add(section);
            }
        }
        if (!applied) {
            return sections;
        }
        return result;
    }

    private static String truncate(String text, int targetTokens) {
        int maxChars = Math.max(OPTIONAL_MIN_TOKEN_TARGET, targetTokens) * 4;
        if (text.length() <= maxChars) {
            return text;
        }
        return text.substring(0, maxChars) + "\n[truncated by prompt budget]";
    }

    private static String compress(String prefix, String text) {
        return prefix + ": " + truncate(text, COMPRESSED_OPTIONAL_TOKENS);
    }

    private static String compressTicketContext(String text) {
        return "ticketContext compressed to key fields only: " + truncate(text, COMPRESSED_TICKET_TOKENS);
    }

    private static PromptUsageTelemetry telemetry(
            List<PromptSection> sections,
            PromptBudget budget,
            int optionalTokensDropped,
            boolean budgetExceeded,
            String budgetAction) {
        return new PromptUsageTelemetry(
                tokensFor(sections, PromptSectionType.SYSTEM_INSTRUCTIONS),
                tokensFor(sections, PromptSectionType.PLANNER_CONTRACT_SUMMARY),
                tokensFor(sections, PromptSectionType.TOOL_CATALOG_COMPACT),
                tokensFor(sections, PromptSectionType.TICKET_CONTEXT),
                tokensFor(sections, PromptSectionType.ORDER_CONTEXT),
                tokensFor(sections, PromptSectionType.CONVERSATION_HISTORY),
                tokensFor(sections, PromptSectionType.RAG_CONTEXT),
                optionalTokensDropped,
                totalTokens(sections),
                budget.maxOutputTokens(),
                budgetExceeded,
                budgetAction,
                "unknown",
                "unknown");
    }

    private static int totalTokens(List<PromptSection> sections) {
        return sections.stream().mapToInt(PromptSection::estimatedTokens).sum();
    }

    private static int optionalTokens(List<PromptSection> sections) {
        return sections.stream()
                .filter(section -> !section.type().isCritical())
                .mapToInt(PromptSection::estimatedTokens)
                .sum();
    }

    private static int tokensFor(List<PromptSection> sections, PromptSectionType type) {
        return tokensByType(sections).getOrDefault(type, 0);
    }

    private static Map<PromptSectionType, Integer> tokensByType(List<PromptSection> sections) {
        Map<PromptSectionType, Integer> tokensByType = new EnumMap<>(PromptSectionType.class);
        for (PromptSection section : sections) {
            tokensByType.merge(section.type(), section.estimatedTokens(), Integer::sum);
        }
        return tokensByType;
    }
}
