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
        assertTraceTools(context.runId(), "get_order_by_id", "search_aftersale_policy", "add_ticket_note");
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
    void couponHandlerCallsPolicyAndTicketNoteToolsThroughToolRegistry() {
        SubtaskExecutionContext context = contextFor(SubtaskType.COUPON_CONSULTATION, List.of(
                new PlannedToolCall("search_aftersale_policy", "Policy evidence."),
                new PlannedToolCall("add_ticket_note", "Record note.")));

        SubtaskExecutionResult result = couponAgentHandler.handle(context);

        assertThat(result.status()).isEqualTo(SubtaskStatus.SUCCEEDED);
        assertThat(result.toolCalls()).contains("search_aftersale_policy", "add_ticket_note");
        assertTraceTools(context.runId(), "search_aftersale_policy", "add_ticket_note");
    }

    private SubtaskExecutionContext contextFor(SubtaskType type, List<PlannedToolCall> plannedTools) {
        Ticket ticket = ticketApplicationService.createTicket(
                "U-HANDLER-" + UUID.randomUUID(),
                "O202605130001",
                "Handler test message.");
        AgentSubtask subtask = new AgentSubtask(
                "subtask-" + type.name().toLowerCase(),
                type,
                "handler target",
                "handler fragment",
                1,
                ToolRiskLevel.LOW,
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
        return new SubtaskExecutionContext(
                "RUN-HANDLER-" + UUID.randomUUID(),
                ticket,
                plan,
                subtask,
                List.of("get_order_by_id", "search_aftersale_policy", "add_ticket_note"),
                RISK_POLICY_SUMMARY,
                List.of());
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
