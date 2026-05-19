package com.example.aftersale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.aftersale.agent.application.planner.AgentPlanningContext;
import com.example.aftersale.agent.prompt.AgentPlannerPromptFactory;
import com.example.aftersale.agent.prompt.CompactToolCatalogBuilder;
import com.example.aftersale.agent.prompt.PromptBudget;
import com.example.aftersale.agent.prompt.PromptBudgetApplier;
import com.example.aftersale.agent.prompt.PromptBudgetExceededException;
import com.example.aftersale.agent.prompt.PromptBudgetResult;
import com.example.aftersale.agent.prompt.PromptBuildResult;
import com.example.aftersale.agent.prompt.PromptSection;
import com.example.aftersale.agent.prompt.PromptSectionType;
import com.example.aftersale.ticket.domain.TicketStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class PromptBudgetTest {

    private static final String SENTINEL = "DO_NOT_INCLUDE_FULL_LONG_DOCUMENT_SENTINEL";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void promptSectionEstimatesTokensWithSimpleCharRule() {
        assertThat(new PromptSection(PromptSectionType.EXAMPLES, "12345678").estimatedTokens()).isEqualTo(2);
        assertThat(new PromptSection(PromptSectionType.EXAMPLES, "").estimatedTokens()).isEqualTo(1);
    }

    @Test
    void promptBudgetCalculatesTotalInputTokens() {
        PromptBudgetResult result = new PromptBudgetApplier().apply(baseSections(), new PromptBudget(
                2000,
                4000,
                8000,
                2000,
                1000,
                16000));

        int expected = result.sections().stream().mapToInt(PromptSection::estimatedTokens).sum();
        assertThat(result.telemetry().totalInputTokens()).isEqualTo(expected);
        assertThat(result.telemetry().maxOutputTokens()).isEqualTo(1000);
    }

    @Test
    void criticalSectionsAreNeverSilentlyDropped() {
        List<PromptSection> sections = List.of(
                new PromptSection(PromptSectionType.SYSTEM_INSTRUCTIONS, repeat("critical-system", 200)),
                new PromptSection(PromptSectionType.OUTPUT_SCHEMA, "{}"),
                new PromptSection(PromptSectionType.PLANNER_CONTRACT_SUMMARY, "contract"),
                new PromptSection(PromptSectionType.TOOL_CATALOG_COMPACT, "[]"),
                new PromptSection(PromptSectionType.RISK_POLICY_SUMMARY, "risk"),
                new PromptSection(PromptSectionType.TICKET_CONTEXT, "ticket"));

        assertThatThrownBy(() -> new PromptBudgetApplier().apply(sections, new PromptBudget(
                10,
                4000,
                8000,
                2000,
                1000,
                16000)))
                .isInstanceOf(PromptBudgetExceededException.class)
                .hasMessageContaining("Critical section systemInstructions");
    }

    @Test
    void optionalSectionsAreReducedInPolicyOrder() {
        List<PromptSection> sections = append(baseSections(), List.of(
                new PromptSection(PromptSectionType.DEBUG_HINTS, repeat("debug", 200)),
                new PromptSection(PromptSectionType.EXAMPLES, repeat("example", 200)),
                new PromptSection(PromptSectionType.CONVERSATION_HISTORY, repeat("history", 200)),
                new PromptSection(PromptSectionType.RAG_CONTEXT, repeat("rag", 200)),
                new PromptSection(PromptSectionType.EXTENDED_POLICY_TEXT, repeat("policy", 200)),
                new PromptSection(PromptSectionType.NON_ESSENTIAL_DOCS, repeat("docs", 200))));

        PromptBudgetResult result = new PromptBudgetApplier().apply(sections, new PromptBudget(
                2000,
                4000,
                8000,
                2000,
                1000,
                230));

        assertThat(result.telemetry().budgetExceeded()).isTrue();
        assertThat(result.telemetry().optionalTokensDropped()).isPositive();
        assertThat(result.telemetry().budgetAction())
                .startsWith("DROP_DEBUG_HINTS,TRUNCATE_EXAMPLES,TRUNCATE_CONVERSATION_HISTORY")
                .contains("COMPRESS_RAG_CONTEXT")
                .contains("COMPRESS_EXTENDED_POLICY_TEXT,DROP_NON_ESSENTIAL_DOCS");
    }

    @Test
    void budgetThrowsClearErrorWhenPromptStillExceedsBudget() {
        List<PromptSection> sections = append(baseSections(), List.of(
                new PromptSection(PromptSectionType.DEBUG_HINTS, repeat("debug", 20))));

        assertThatThrownBy(() -> new PromptBudgetApplier().apply(sections, new PromptBudget(
                2000,
                4000,
                8000,
                2000,
                1000,
                5)))
                .isInstanceOf(PromptBudgetExceededException.class)
                .hasMessageContaining("Prompt input token budget exceeded");
    }

    @Test
    void compactToolCatalogContainsOnlyPlannerSafeFields() {
        String catalog = new CompactToolCatalogBuilder(objectMapper).build(List.of(
                "get_order_by_id",
                "search_aftersale_policy",
                "add_ticket_note"));

        assertThat(catalog)
                .contains("\"name\":\"get_order_by_id\"")
                .contains("\"risk\":\"LOW\"")
                .contains("\"requiredInputFields\":[\"orderId\"]")
                .contains("\"purpose\":\"Fetch order facts and item details\"");
        assertThat(catalog)
                .doesNotContain("inputSchema")
                .doesNotContain("outputSchema")
                .doesNotContain("requiresApproval")
                .doesNotContain("TOOL_CONTRACTS");
    }

    @Test
    void longOptionalDocumentSentinelIsNotIncludedInFinalPrompt() {
        AgentPlannerPromptFactory factory = new AgentPlannerPromptFactory(
                objectMapper,
                new PromptBudgetApplier(),
                new PromptBudget(2000, 4000, 8000, 2000, 1000, 500),
                new CompactToolCatalogBuilder(objectMapper));

        PromptBuildResult result = factory.buildWithOptionalSections(planningContext(), List.of(
                new PromptSection(PromptSectionType.DEBUG_HINTS, repeat("debug", 200)),
                new PromptSection(PromptSectionType.NON_ESSENTIAL_DOCS, repeat("prefix", 200) + SENTINEL)));

        assertThat(result.userPrompt()).doesNotContain(SENTINEL);
        assertThat(result.telemetry().budgetAction()).contains("DROP_NON_ESSENTIAL_DOCS");
    }

    @Test
    void promptFactoryUsesSeparateBudgetAndCatalogCollaborators() {
        AgentPlannerPromptFactory factory = new AgentPlannerPromptFactory(
                objectMapper,
                new PromptBudgetApplier(),
                PromptBudget.defaults(),
                new CompactToolCatalogBuilder(objectMapper));

        PromptBuildResult result = factory.build(planningContext());

        assertThat(result.systemPrompt()).contains("Return only one JSON object");
        assertThat(result.userPrompt())
                .contains("## outputSchema")
                .contains("## toolCatalogCompact")
                .contains("T-BUDGET-1")
                .contains("get_order_by_id");
        assertThat(result.telemetry().toolCatalogTokens()).isPositive();
    }

    private static List<PromptSection> baseSections() {
        return List.of(
                new PromptSection(PromptSectionType.SYSTEM_INSTRUCTIONS, "system"),
                new PromptSection(PromptSectionType.OUTPUT_SCHEMA, "schema"),
                new PromptSection(PromptSectionType.PLANNER_CONTRACT_SUMMARY, "contract"),
                new PromptSection(PromptSectionType.TOOL_CATALOG_COMPACT, "catalog"),
                new PromptSection(PromptSectionType.RISK_POLICY_SUMMARY, "risk"),
                new PromptSection(PromptSectionType.TICKET_CONTEXT, "ticket"));
    }

    private static List<PromptSection> append(List<PromptSection> base, List<PromptSection> additions) {
        java.util.ArrayList<PromptSection> result = new java.util.ArrayList<>(base);
        result.addAll(additions);
        return result;
    }

    private static String repeat(String value, int count) {
        return value.repeat(count);
    }

    private static AgentPlanningContext planningContext() {
        return new AgentPlanningContext(
                "T-BUDGET-1",
                "U-BUDGET-1",
                "O-BUDGET-1",
                "用户反馈裙子尺码不合适，想换货。",
                TicketStatus.CREATED,
                List.of("get_order_by_id", "search_aftersale_policy", "add_ticket_note"),
                "High-risk actions require human approval.",
                Instant.parse("2026-05-14T00:00:00Z"));
    }
}
