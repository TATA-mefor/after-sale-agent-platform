package io.github.tatame.aftersale.agent.prompt;

public enum PromptSectionType {

    SYSTEM_INSTRUCTIONS("systemInstructions", true),
    OUTPUT_SCHEMA("outputSchema", true),
    PLANNER_CONTRACT_SUMMARY("plannerContractSummary", true),
    TOOL_CATALOG_COMPACT("toolCatalogCompact", true),
    RISK_POLICY_SUMMARY("riskPolicySummary", true),
    TICKET_CONTEXT("ticketContext", true),
    CONVERSATION_HISTORY("conversationHistory", false),
    RAG_CONTEXT("ragContext", false),
    EXAMPLES("examples", false),
    DEBUG_HINTS("debugHints", false),
    EXTENDED_POLICY_TEXT("extendedPolicyText", false),
    NON_ESSENTIAL_DOCS("nonEssentialDocs", false),
    ORDER_CONTEXT("orderContext", false);

    private final String sectionName;
    private final boolean critical;

    PromptSectionType(String sectionName, boolean critical) {
        this.sectionName = sectionName;
        this.critical = critical;
    }

    public String sectionName() {
        return sectionName;
    }

    public boolean isCritical() {
        return critical;
    }
}
