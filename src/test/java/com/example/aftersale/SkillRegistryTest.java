package com.example.aftersale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.aftersale.agent.application.planner.AgentPlan;
import com.example.aftersale.agent.application.planner.AgentSubtask;
import com.example.aftersale.agent.application.planner.PlannedToolCall;
import com.example.aftersale.agent.application.planner.SubtaskType;
import com.example.aftersale.agent.application.skill.AgentSkill;
import com.example.aftersale.agent.application.skill.SkillDefinition;
import com.example.aftersale.agent.application.skill.SkillExecutionContext;
import com.example.aftersale.agent.application.skill.SkillExecutionResult;
import com.example.aftersale.agent.application.skill.SkillExecutionStatus;
import com.example.aftersale.agent.application.skill.SkillRegistry;
import com.example.aftersale.agent.application.skill.SkillRiskEvaluator;
import com.example.aftersale.agent.application.workspace.AgentWorkspace;
import com.example.aftersale.ticket.application.TicketApplicationService;
import com.example.aftersale.ticket.domain.IntentType;
import com.example.aftersale.ticket.domain.Ticket;
import com.example.aftersale.tool.domain.ToolRiskLevel;
import com.example.aftersale.trace.application.ToolCallTraceApplicationService;
import com.example.aftersale.trace.domain.ToolCallTrace;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class SkillRegistryTest {

    private static final String GET_ORDER_BY_ID = "get_order_by_id";
    private static final String SEARCH_POLICY = "search_aftersale_policy";
    private static final String ADD_TICKET_NOTE = "add_ticket_note";
    private static final String UPDATE_TICKET_STATUS = "update_ticket_status";
    private static final String RISK_POLICY_SUMMARY = "HIGH actions require human approval.";

    @Autowired
    private SkillRegistry skillRegistry;

    @Autowired
    private SkillRiskEvaluator skillRiskEvaluator;

    @Autowired
    private TicketApplicationService ticketApplicationService;

    @Autowired
    private ToolCallTraceApplicationService traceApplicationService;

    @Test
    void registryFindsSkillsByNameAndSubtaskType() {
        assertThat(skillRegistry.findSkill("ReturnEligibilityAssessmentSkill")).isPresent();
        assertThat(skillRegistry.findSkill("ExchangeRecommendationSkill")).isPresent();
        assertThat(skillRegistry.findSkill("CouponConsultationSkill")).isPresent();
        assertThat(skillRegistry.findBySubtaskType(SubtaskType.RETURN))
                .extracting(AgentSkill::skillName)
                .containsExactly("ReturnEligibilityAssessmentSkill");
        assertThat(skillRegistry.findBySubtaskType(SubtaskType.EXCHANGE))
                .extracting(AgentSkill::skillName)
                .containsExactly("ExchangeRecommendationSkill");
        assertThat(skillRegistry.findBySubtaskType(SubtaskType.UNKNOWN)).isEmpty();
    }

    @Test
    void skillDefinitionsExposeExpectedBoundaries() {
        Map<String, SkillDefinition> definitions = skillRegistry.listDefinitions().stream()
                .collect(Collectors.toMap(SkillDefinition::skillName, definition -> definition));

        assertDefinition(
                definitions.get("ReturnEligibilityAssessmentSkill"),
                Set.of(SubtaskType.RETURN),
                List.of(GET_ORDER_BY_ID, SEARCH_POLICY, ADD_TICKET_NOTE),
                ToolRiskLevel.LOW);
        assertDefinition(
                definitions.get("ExchangeRecommendationSkill"),
                Set.of(SubtaskType.EXCHANGE),
                List.of(GET_ORDER_BY_ID, SEARCH_POLICY, ADD_TICKET_NOTE),
                ToolRiskLevel.LOW);
        assertDefinition(
                definitions.get("CouponConsultationSkill"),
                Set.of(SubtaskType.COUPON_CONSULTATION),
                List.of(SEARCH_POLICY, ADD_TICKET_NOTE),
                ToolRiskLevel.LOW);
        assertDefinition(
                definitions.get("LogisticsIssueAnalysisSkill"),
                Set.of(SubtaskType.LOGISTICS_ISSUE),
                List.of(GET_ORDER_BY_ID, SEARCH_POLICY, ADD_TICKET_NOTE),
                ToolRiskLevel.LOW);
        assertDefinition(
                definitions.get("GeneralAfterSaleConsultationSkill"),
                Set.of(SubtaskType.GENERAL_CONSULTATION),
                List.of(SEARCH_POLICY, ADD_TICKET_NOTE),
                ToolRiskLevel.LOW);
        assertDefinition(
                definitions.get("HumanApprovalRoutingSkill"),
                Set.of(SubtaskType.HUMAN_ESCALATION),
                List.of(ADD_TICKET_NOTE),
                ToolRiskLevel.HIGH);
    }

    @Test
    void registryRejectsDuplicateSkillName() {
        AgentSkill first = new StubSkill(skillDefinition("DuplicateSkill", ToolRiskLevel.LOW, List.of()));
        AgentSkill second = new StubSkill(skillDefinition("DuplicateSkill", ToolRiskLevel.LOW, List.of()));

        assertThatThrownBy(() -> new SkillRegistry(List.of(first, second), skillRiskEvaluator))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Duplicate skillName: DuplicateSkill");
    }

    @Test
    void skillRiskMustNotBeLowerThanHighestRequiredToolRisk() {
        SkillDefinition definition = skillDefinition(
                "UnsafeStatusSkill",
                ToolRiskLevel.LOW,
                List.of(UPDATE_TICKET_STATUS));

        assertThatThrownBy(() -> skillRiskEvaluator.validate(definition))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("riskLevel LOW is lower than required tool risk MEDIUM");
    }

    @Test
    void specialistAdapterPreservesHandlerToolRegistryExecutionPath() {
        AgentSkill skill = skillRegistry.findSkill("ReturnEligibilityAssessmentSkill").orElseThrow();
        SkillExecutionContext context = executionContext(SubtaskType.RETURN, ToolRiskLevel.LOW);

        SkillExecutionResult result = skill.execute(context);

        assertThat(result.status()).isEqualTo(SkillExecutionStatus.SUCCEEDED);
        assertThat(result.toolCalls()).containsExactly(GET_ORDER_BY_ID, SEARCH_POLICY, ADD_TICKET_NOTE);
        assertThat(context.workspace().orderFacts()).isNotEmpty();
        assertThat(context.workspace().subtaskMemories()).isNotEmpty();
        assertThat(traceApplicationService.findByRunId(context.runId()))
                .extracting(ToolCallTrace::getToolName)
                .containsExactlyInAnyOrder(GET_ORDER_BY_ID, SEARCH_POLICY, ADD_TICKET_NOTE);
    }

    @Test
    void highRiskSkillDoesNotAutoExecuteBusinessAction() {
        AgentSkill skill = skillRegistry.findSkill("ReturnEligibilityAssessmentSkill").orElseThrow();
        SkillExecutionContext context = executionContext(SubtaskType.RETURN, ToolRiskLevel.HIGH);

        SkillExecutionResult result = skill.execute(context);

        assertThat(result.status()).isEqualTo(SkillExecutionStatus.WAITING_APPROVAL);
        assertThat(result.approvalRequired()).isTrue();
        assertThat(result.toolCalls()).isEmpty();
        assertThat(traceApplicationService.findByRunId(context.runId())).isEmpty();
    }

    private SkillExecutionContext executionContext(SubtaskType type, ToolRiskLevel riskLevel) {
        Ticket ticket = ticketApplicationService.createTicket(
                "U-SKILL-" + UUID.randomUUID(),
                "O202605130001",
                "Skill test message.");
        AgentSubtask subtask = new AgentSubtask(
                "subtask-" + type.name().toLowerCase(),
                type,
                "Wireless Headphones",
                "Wireless Headphones 想退货。",
                1,
                riskLevel,
                "质量问题 退货 换货",
                List.of(
                        new PlannedToolCall(GET_ORDER_BY_ID, "Order facts."),
                        new PlannedToolCall(SEARCH_POLICY, "Policy evidence."),
                        new PlannedToolCall(ADD_TICKET_NOTE, "Record note.")),
                List.of());
        AgentPlan plan = new AgentPlan(
                IntentType.MULTI_INTENT,
                ToolRiskLevel.LOW,
                "质量问题 退货 换货",
                "Skill test note.",
                "Skill test final suggestion.",
                List.of("skill evidence hint"),
                List.of(),
                List.of(subtask));
        AgentWorkspace workspace = AgentWorkspace.start(
                "RUN-SKILL-" + UUID.randomUUID(),
                ticket.getTicketId(),
                ticket.getCreatedAt());
        return new SkillExecutionContext(
                workspace.agentRunId(),
                ticket,
                plan,
                subtask,
                workspace,
                List.of(GET_ORDER_BY_ID, SEARCH_POLICY, ADD_TICKET_NOTE),
                RISK_POLICY_SUMMARY,
                List.of());
    }

    private static void assertDefinition(
            SkillDefinition definition,
            Set<SubtaskType> supportedTypes,
            List<String> requiredTools,
            ToolRiskLevel riskLevel) {
        assertThat(definition).isNotNull();
        assertThat(definition.supportedSubtaskTypes()).containsExactlyInAnyOrderElementsOf(supportedTypes);
        assertThat(definition.requiredTools()).containsExactlyElementsOf(requiredTools);
        assertThat(definition.riskLevel()).isEqualTo(riskLevel);
        assertThat(definition.evidenceRequirements()).contains("Tool evidence must come from ToolRegistry outputs.");
    }

    private static SkillDefinition skillDefinition(
            String skillName,
            ToolRiskLevel riskLevel,
            List<String> requiredTools) {
        return new SkillDefinition(
                skillName,
                "Stub skill.",
                Set.of(SubtaskType.RETURN),
                requiredTools,
                List.of(),
                riskLevel,
                "",
                List.of(),
                List.of(),
                List.of());
    }

    private record StubSkill(SkillDefinition definition) implements AgentSkill {

        @Override
        public SkillExecutionResult execute(SkillExecutionContext context) {
            return SkillExecutionResult.succeeded(definition.skillName(), "stub", List.of(), List.of());
        }
    }
}
