package io.github.tatame.aftersale.agent.prompt;

import io.github.tatame.aftersale.agent.application.planner.AgentPlanningContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 构建 LLM 生成 AgentPlan 所需的紧凑 Planner Prompt。
 *
 * <p>边界：本工厂只组装 prompt section 和 schema 提示。预算策略交给 PromptBudgetApplier，
 * 避免 prompt 构建过程变成隐藏策略引擎。
 */
public class AgentPlannerPromptFactory {

    private final ObjectMapper objectMapper;
    private final PromptBudgetApplier budgetApplier;
    private final PromptBudget budget;
    private final CompactToolCatalogBuilder toolCatalogBuilder;

    @SuppressFBWarnings(
            value = "EI_EXPOSE_REP2",
            justification = "ObjectMapper is an application-wide JSON collaborator injected by Spring.")
    public AgentPlannerPromptFactory(ObjectMapper objectMapper) {
        this(
                objectMapper,
                new PromptBudgetApplier(),
                PromptBudget.defaults(),
                new CompactToolCatalogBuilder(objectMapper));
    }

    @SuppressFBWarnings(
            value = "EI_EXPOSE_REP2",
            justification = "Prompt collaborators are stateless services or configuration values.")
    public AgentPlannerPromptFactory(
            ObjectMapper objectMapper,
            PromptBudgetApplier budgetApplier,
            PromptBudget budget,
            CompactToolCatalogBuilder toolCatalogBuilder) {
        this.objectMapper = objectMapper;
        this.budgetApplier = budgetApplier;
        this.budget = budget;
        this.toolCatalogBuilder = toolCatalogBuilder;
    }

    public String systemPrompt() {
        return systemInstructions();
    }

    public String userPrompt(AgentPlanningContext context) {
        return build(context).userPrompt();
    }

    /**
     * 使用默认 optional section 集合构建 system prompt 和 user prompt。
     */
    public PromptBuildResult build(AgentPlanningContext context) {
        return buildWithOptionalSections(context, List.of());
    }

    /**
     * 构建经过预算处理的 prompt，同时允许测试或未来调用方提供 optional context。
     *
     * <p>Optional section 可以被预算策略裁剪，但 Planner 契约、风险策略、Ticket 上下文和工具目录等
     * critical section 必须保留。
     */
    public PromptBuildResult buildWithOptionalSections(
            AgentPlanningContext context,
            List<PromptSection> optionalSections) {
        List<PromptSection> sections = new ArrayList<>();
        sections.add(new PromptSection(PromptSectionType.SYSTEM_INSTRUCTIONS, systemInstructions()));
        sections.add(new PromptSection(PromptSectionType.OUTPUT_SCHEMA, outputSchema()));
        sections.add(new PromptSection(PromptSectionType.PLANNER_CONTRACT_SUMMARY, plannerContractSummary()));
        sections.add(new PromptSection(
                PromptSectionType.TOOL_CATALOG_COMPACT,
                toolCatalogBuilder.build(context.availableTools())));
        sections.add(new PromptSection(PromptSectionType.RISK_POLICY_SUMMARY, context.riskPolicySummary()));
        sections.add(new PromptSection(PromptSectionType.TICKET_CONTEXT, ticketContext(context)));
        sections.addAll(optionalSections);

        PromptBudgetResult budgetResult = budgetApplier.apply(sections, budget);
        String systemPrompt = sectionContent(budgetResult.sections(), PromptSectionType.SYSTEM_INSTRUCTIONS);
        String userPrompt = userPromptFromSections(budgetResult.sections());
        return new PromptBuildResult(systemPrompt, userPrompt, budgetResult.telemetry(), budgetResult.sections());
    }

    private static String systemInstructions() {
        return """
                You are the planner for an after-sale ticket Agent.
                Return only one JSON object matching the AgentPlan schema.
                You may plan tools, but you must not execute tools.
                You must not claim refunds, compensation, dispute closure, payment changes, or other high-risk actions
                have already been completed.
                Java backend validates the plan and executes tools through ToolRegistry.
                """;
    }

    private String outputSchema() {
        return toJson(Map.of(
                "intent", "supported IntentType enum",
                "riskLevel", "LOW | MEDIUM | HIGH",
                "policyQuery", "non-empty policy search query",
                "noteToAdd", "safe internal note",
                "finalSuggestion", "safe final suggestion",
                "evidenceHints", "array of strings",
                "plannedTools", "array of {toolName, reason}; keep non-empty for root-level fallback execution",
                "subtasks", "array of {subtaskId, type, target, userMessageFragment, priority, riskLevel, "
                        + "policyQuery, plannedTools, dependencies}; use [] for single-intent cases"));
    }

    private static String plannerContractSummary() {
        return """
                Create one AgentPlan for the ticket context.
                The LLM may only plan. It must not execute tools or claim that business actions have completed.
                The Java backend validates AgentPlan and executes all tools through ToolRegistry.
                Use subtasks for multi-intent tickets so specialist handlers can process return, exchange, coupon,
                logistics, refund-only, and general consultation work separately.
                High-risk refunds, compensation, payment changes, and dispute closure require approval boundaries.
                """;
    }

    private String ticketContext(AgentPlanningContext context) {
        return toJson(Map.of(
                "ticketId", context.ticketId(),
                "userId", context.userId(),
                "orderId", context.orderId(),
                "rawUserMessage", context.rawUserMessage(),
                "currentTicketStatus", context.currentTicketStatus().name(),
                "availableTools", context.availableTools(),
                "riskPolicySummary", context.riskPolicySummary(),
                "createdAt", context.createdAt().toString()));
    }

    private static String userPromptFromSections(List<PromptSection> sections) {
        StringBuilder prompt = new StringBuilder("Create an AgentPlan for this context:");
        for (PromptSection section : sections) {
            if (section.type() == PromptSectionType.SYSTEM_INSTRUCTIONS || section.content().isBlank()) {
                continue;
            }
            prompt.append("\n\n## ")
                    .append(section.type().sectionName())
                    .append("\n")
                    .append(section.content());
        }
        return prompt.toString();
    }

    private static String sectionContent(List<PromptSection> sections, PromptSectionType type) {
        return sections.stream()
                .filter(section -> section.type() == type)
                .findFirst()
                .map(PromptSection::content)
                .orElseThrow(() -> new PromptBudgetExceededException(
                        "Missing required prompt section " + type.sectionName()));
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize LLM planning prompt", exception);
        }
    }
}
