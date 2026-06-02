package io.github.tatame.aftersale.agent.prompt;

import java.util.List;

public class PromptBudgetPolicy {

    private static final List<PromptSectionType> REDUCTION_ORDER = List.of(
            PromptSectionType.DEBUG_HINTS,
            PromptSectionType.EXAMPLES,
            PromptSectionType.CONVERSATION_HISTORY,
            PromptSectionType.RAG_CONTEXT,
            PromptSectionType.EXTENDED_POLICY_TEXT,
            PromptSectionType.NON_ESSENTIAL_DOCS,
            PromptSectionType.TICKET_CONTEXT);

    public List<PromptSectionType> reductionOrder() {
        return REDUCTION_ORDER;
    }
}
