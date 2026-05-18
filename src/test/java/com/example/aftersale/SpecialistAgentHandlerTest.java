package com.example.aftersale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.aftersale.agent.application.handler.CouponAgentHandler;
import com.example.aftersale.agent.application.handler.ExchangeAgentHandler;
import com.example.aftersale.agent.application.handler.ReturnAgentHandler;
import com.example.aftersale.agent.application.handler.SpecialistAgentHandler;
import com.example.aftersale.agent.application.handler.SpecialistAgentHandlerRegistry;
import com.example.aftersale.agent.application.handler.SubtaskExecutionContext;
import com.example.aftersale.agent.application.handler.SubtaskExecutionResult;
import com.example.aftersale.agent.application.planner.AgentPlan;
import com.example.aftersale.agent.application.planner.AgentSubtask;
import com.example.aftersale.agent.application.planner.PlannedToolCall;
import com.example.aftersale.agent.application.planner.SubtaskStatus;
import com.example.aftersale.agent.application.planner.SubtaskType;
import com.example.aftersale.agent.application.workspace.AgentWorkspace;
import com.example.aftersale.agent.application.workspace.PolicyEvidence;
import com.example.aftersale.ticket.application.TicketApplicationService;
import com.example.aftersale.ticket.domain.IntentType;
import com.example.aftersale.ticket.domain.Ticket;
import com.example.aftersale.tool.domain.ToolRiskLevel;
import com.example.aftersale.trace.application.ToolCallTraceApplicationService;
import com.example.aftersale.trace.domain.ToolCallTrace;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class SpecialistAgentHandlerTest {

    private static final String RISK_POLICY_SUMMARY = "HIGH actions require human approval.";

    @Autowired
    private SpecialistAgentHandlerRegistry registry;

    @Autowired
    private ReturnAgentHandler returnAgentHandler;

    @Autowired
    private ExchangeAgentHandler exchangeAgentHandler;

    @Autowired
    private CouponAgentHandler couponAgentHandler;

    @Autowired
    private TicketApplicationService ticketApplicationService;

    @Autowired
    private ToolCallTraceApplicationService traceApplicationService;

    @Test
    void registryFindsHandlersBySubtaskType() {
        assertThat(registry.findHandler(SubtaskType.RETURN)).containsInstanceOf(ReturnAgentHandler.class);
        assertThat(registry.findHandler(SubtaskType.EXCHANGE)).containsInstanceOf(ExchangeAgentHandler.class);
        assertThat(registry.findHandler(SubtaskType.COUPON_CONSULTATION)).containsInstanceOf(CouponAgentHandler.class);
        assertThat(registry.supportedTypes())
                .contains(
                        SubtaskType.RETURN,
                        SubtaskType.EXCHANGE,
                        SubtaskType.COUPON_CONSULTATION,
                        SubtaskType.LOGISTICS_ISSUE,
                        SubtaskType.GENERAL_CONSULTATION,
                        SubtaskType.HUMAN_ESCALATION);
    }

    @Test
    void registryRejectsDuplicateHandlersForSameSubtaskType() {
        SpecialistAgentHandler first = new StubHandler(SubtaskType.RETURN);
        SpecialistAgentHandler second = new StubHandler(SubtaskType.RETURN);

        assertThatThrownBy(() -> new SpecialistAgentHandlerRegistry(List.of(first, second)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Duplicate specialist handler");
    }

    @Test
    void registryReturnsClearFailureForUnsupportedSubtaskType() {
        SubtaskExecutionContext context = contextFor(SubtaskType.REPAIR, List.of(
                new PlannedToolCall("search_aftersale_policy", "Search repair policy.")));

        SubtaskExecutionResult result = registry.handle(context);

        assertThat(result.status()).isEqualTo(SubtaskStatus.FAILED);
        assertThat(result.errorMessage()).contains("No specialist handler registered");
        assertThat(result.toolCalls()).isEmpty();
    }

    @Test
    void returnHandlerCallsOrderPolicyAndTicketNoteToolsThroughToolRegistry() {
        SubtaskExecutionContext context = contextFor(SubtaskType.RETURN, List.of(
                new PlannedToolCall("get_order_by_id", "Order facts."),
                new PlannedToolCall("search_aftersale_policy", "Policy evidence."),
                new PlannedToolCall("add_ticket_note", "Record note.")));

        SubtaskExecutionResult result = returnAgentHandler.handle(context);

        assertThat(result.status()).isEqualTo(SubtaskStatus.SUCCEEDED);
        assertThat(result.toolCalls())
                .containsExactly("get_order_by_id", "search_aftersale_policy", "add_ticket_note");
        assertThat(context.workspace().orderFacts())
                .extracting(orderFact -> orderFact.orderId())
                .contains("O202605130001");
        assertThat(context.workspace().policyEvidence())
                .extracting(PolicyEvidence::policyId)
                .contains("POL-QUALITY-RETURN-EXCHANGE");
        assertThat(context.workspace().toolResultSummaries())
                .extracting(toolSummary -> toolSummary.toolName())
                .containsExactly("get_order_by_id", "search_aftersale_policy", "add_ticket_note");
        assertThat(context.workspace().subtaskMemories())
                .extracting(subtaskMemory -> subtaskMemory.subtaskId())
                .contains(context.subtask().subtaskId());
        assertTraceTools(context.runId(), "get_order_by_id", "search_aftersale_policy", "add_ticket_note");
    }

    @Test
    void handlerCallsPolicyToolBeforeActionToolEvenWhenPlanIsOutOfOrder() {
        SubtaskExecutionContext context = contextFor(SubtaskType.RETURN, List.of(
                new PlannedToolCall("add_ticket_note", "Record note."),
                new PlannedToolCall("search_aftersale_policy", "Policy evidence.")));

        SubtaskExecutionResult result = returnAgentHandler.handle(context);

        assertThat(result.status()).isEqualTo(SubtaskStatus.SUCCEEDED);
        assertThat(result.toolCalls()).containsExactly("get_order_by_id", "search_aftersale_policy",
                "add_ticket_note");
    }

    @Test
    void exchangeHandlerCallsOrderPolicyAndTicketNoteToolsThroughToolRegistry() {
        SubtaskExecutionContext context = contextFor(SubtaskType.EXCHANGE, List.of(
                new PlannedToolCall("get_order_by_id", "Order facts."),
                new PlannedToolCall("search_aftersale_policy", "Policy evidence."),
                new PlannedToolCall("add_ticket_note", "Record note.")));

        SubtaskExecutionResult result = exchangeAgentHandler.handle(context);

        assertThat(result.status()).isEqualTo(SubtaskStatus.SUCCEEDED);
        assertThat(result.toolCalls())
                .containsExactly("get_order_by_id", "search_aftersale_policy", "add_ticket_note");
        assertTraceTools(context.runId(), "get_order_by_id", "search_aftersale_policy", "add_ticket_note");
    }

    @Test
    void returnHandlerGeneratesItemLevelReturnRecommendationFromOrderItems() {
        SubtaskExecutionContext context = contextFor(
                SubtaskType.RETURN,
                "O202605130001",
                "Wireless Headphones",
                "Wireless Headphones 左耳没声音，想退货。",
                ToolRiskLevel.LOW,
                defaultPlannedTools());

        SubtaskExecutionResult result = returnAgentHandler.handle(context);

        assertThat(result.status()).isEqualTo(SubtaskStatus.SUCCEEDED);
        assertThat(result.summary())
                .contains(
                        "Item-level return recommendation",
                        "orderItemId=OI-O202605130001-1",
                        "productId=P-HEADPHONE-001",
                        "productName=Wireless Headphones",
                        "category=电子数码",
                        "supportReturn=true",
                        "supportExchange=true",
                        "isSpecialItem=false",
                        "no real return action is executed",
                        "no special restriction found in current demo data");
        assertThat(context.workspace().orderFacts().get(0).orderItems()).hasSize(1);
        assertThat(context.workspace().subtaskMemories().get(0).summary())
                .contains("Item-level return recommendation");
        assertThat(ticketApplicationService.getTicket(context.ticket().getTicketId()).getInternalNote())
                .contains("Item-level return recommendation", "orderItemId=OI-O202605130001-1");
        assertTraceTools(context.runId(), "get_order_by_id", "search_aftersale_policy", "add_ticket_note");
    }

    @Test
    void exchangeHandlerGeneratesItemLevelExchangeRecommendationFromOrderItems() {
        SubtaskExecutionContext context = contextFor(
                SubtaskType.EXCHANGE,
                "O202605130001",
                "Wireless Headphones",
                "Wireless Headphones 想换货。",
                ToolRiskLevel.LOW,
                defaultPlannedTools());

        SubtaskExecutionResult result = exchangeAgentHandler.handle(context);

        assertThat(result.status()).isEqualTo(SubtaskStatus.SUCCEEDED);
        assertThat(result.summary())
                .contains(
                        "Item-level exchange recommendation",
                        "orderItemId=OI-O202605130001-1",
                        "productName=Wireless Headphones",
                        "supportExchange=true",
                        "isSpecialItem=false",
                        "no real exchange action is executed");
        assertThat(ticketApplicationService.getTicket(context.ticket().getTicketId()).getInternalNote())
                .contains("Item-level exchange recommendation", "orderItemId=OI-O202605130001-1");
        assertTraceTools(context.runId(), "get_order_by_id", "search_aftersale_policy", "add_ticket_note");
    }

    @Test
    void returnHandlerDoesNotRecommendDirectReturnForUnsupportedSpecialItem() {
        SubtaskExecutionContext context = contextFor(
                SubtaskType.RETURN,
                "O-SPECIAL-GOODS",
                "Customized Gift Box",
                "Customized Gift Box 想退货。",
                ToolRiskLevel.LOW,
                defaultPlannedTools());

        SubtaskExecutionResult result = returnAgentHandler.handle(context);

        assertThat(result.summary())
                .contains(
                        "Item-level return recommendation",
                        "productName=Customized Gift Box",
                        "supportReturn=false",
                        "isSpecialItem=true",
                        "Do not recommend direct return",
                        "special item restriction applies");
    }

    @Test
    void exchangeHandlerDoesNotRecommendDirectExchangeForUnsupportedSpecialItem() {
        SubtaskExecutionContext context = contextFor(
                SubtaskType.EXCHANGE,
                "O-SPECIAL-GOODS",
                "Customized Gift Box",
                "Customized Gift Box 想换货。",
                ToolRiskLevel.LOW,
                defaultPlannedTools());

        SubtaskExecutionResult result = exchangeAgentHandler.handle(context);

        assertThat(result.summary())
                .contains(
                        "Item-level exchange recommendation",
                        "productName=Customized Gift Box",
                        "supportExchange=false",
                        "isSpecialItem=true",
                        "Do not recommend direct exchange",
                        "special item restriction applies");
    }

    @Test
    void itemLevelRecommendationUsesClearFallbackWhenTargetDoesNotMatchAnyItem() {
        SubtaskExecutionContext context = contextFor(
                SubtaskType.RETURN,
                "O202605130001",
                "完全不匹配的商品",
                "用户描述没有商品名也没有类目。",
                ToolRiskLevel.LOW,
                defaultPlannedTools());

        SubtaskExecutionResult result = returnAgentHandler.handle(context);

        assertThat(result.summary())
                .contains(
                        "Item-level return recommendation",
                        "productName=Wireless Headphones",
                        "fallback item selected because target/message did not match productName or category");
    }

    @Test
    void couponHandlerCallsPolicyAndTicketNoteToolsThroughToolRegistry() {
        SubtaskExecutionContext context = contextFor(SubtaskType.COUPON_CONSULTATION, List.of(
                new PlannedToolCall("search_aftersale_policy", "Policy evidence."),
                new PlannedToolCall("add_ticket_note", "Record note.")));

        SubtaskExecutionResult result = couponAgentHandler.handle(context);

        assertThat(result.status()).isEqualTo(SubtaskStatus.SUCCEEDED);
        assertThat(result.toolCalls()).contains("search_aftersale_policy", "add_ticket_note");
        assertTraceTools(context.runId(), "search_aftersale_policy", "add_ticket_note");
    }

    @Test
    void highRiskSubtaskWritesRiskFlagWithoutExecutingTools() {
        SubtaskExecutionContext context = contextFor(
                SubtaskType.RETURN,
                ToolRiskLevel.HIGH,
                List.of(new PlannedToolCall("get_order_by_id", "Order facts.")));

        SubtaskExecutionResult result = returnAgentHandler.handle(context);

        assertThat(result.requiresHumanApproval()).isTrue();
        assertThat(result.status()).isEqualTo(SubtaskStatus.WAITING_APPROVAL);
        assertThat(result.toolCalls()).isEmpty();
        assertThat(context.workspace().riskFlags())
                .extracting(riskFlag -> riskFlag.subtaskId())
                .contains(context.subtask().subtaskId());
        assertThat(context.workspace().subtaskMemories())
                .extracting(subtaskMemory -> subtaskMemory.status())
                .contains(SubtaskStatus.WAITING_APPROVAL);
        assertThat(traceApplicationService.findByRunId(context.runId())).isEmpty();
    }

    private SubtaskExecutionContext contextFor(SubtaskType type, List<PlannedToolCall> plannedTools) {
        return contextFor(type, ToolRiskLevel.LOW, plannedTools);
    }

    private SubtaskExecutionContext contextFor(
            SubtaskType type,
            ToolRiskLevel riskLevel,
            List<PlannedToolCall> plannedTools) {
        return contextFor(type, "O202605130001", "handler target", "handler fragment", riskLevel, plannedTools);
    }

    private SubtaskExecutionContext contextFor(
            SubtaskType type,
            String orderId,
            String target,
            String fragment,
            ToolRiskLevel riskLevel,
            List<PlannedToolCall> plannedTools) {
        Ticket ticket = ticketApplicationService.createTicket(
                "U-HANDLER-" + UUID.randomUUID(),
                orderId,
                "Handler test message. " + fragment);
        AgentSubtask subtask = new AgentSubtask(
                "subtask-" + type.name().toLowerCase(),
                type,
                target,
                fragment,
                1,
                riskLevel,
                "质量问题 退货 换货 优惠券",
                plannedTools,
                List.of());
        AgentPlan plan = new AgentPlan(
                IntentType.MULTI_INTENT,
                ToolRiskLevel.LOW,
                "质量问题 退货 换货 优惠券",
                "Handler test note.",
                "Handler test final suggestion.",
                List.of("handler evidence hint"),
                List.of(),
                List.of(subtask));
        AgentWorkspace workspace = AgentWorkspace.start(
                "RUN-HANDLER-" + UUID.randomUUID(),
                ticket.getTicketId(),
                ticket.getCreatedAt());
        return new SubtaskExecutionContext(
                workspace.agentRunId(),
                ticket,
                plan,
                subtask,
                workspace,
                List.of("get_order_by_id", "search_aftersale_policy", "add_ticket_note"),
                RISK_POLICY_SUMMARY,
                List.of());
    }

    private static List<PlannedToolCall> defaultPlannedTools() {
        return List.of(
                new PlannedToolCall("get_order_by_id", "Order facts."),
                new PlannedToolCall("search_aftersale_policy", "Policy evidence."),
                new PlannedToolCall("add_ticket_note", "Record note."));
    }

    private void assertTraceTools(String runId, String... expectedToolNames) {
        assertThat(traceApplicationService.findByRunId(runId))
                .extracting(ToolCallTrace::getToolName)
                .containsExactlyInAnyOrder(expectedToolNames);
    }

    private record StubHandler(SubtaskType supportedType) implements SpecialistAgentHandler {

        @Override
        public SubtaskExecutionResult handle(SubtaskExecutionContext context) {
            return SubtaskExecutionResult.succeeded(
                    context.subtask().subtaskId(),
                    context.subtask().type(),
                    "stub",
                    List.of(),
                    List.of());
        }
    }
}
