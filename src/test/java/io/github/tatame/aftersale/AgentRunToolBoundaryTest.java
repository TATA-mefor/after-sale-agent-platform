package io.github.tatame.aftersale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.tatame.aftersale.agent.application.planner.AgentPlan;
import io.github.tatame.aftersale.agent.application.planner.AgentPlanner;
import io.github.tatame.aftersale.agent.application.planner.AgentPlanningContext;
import io.github.tatame.aftersale.agent.application.planner.PlannedToolCall;
import io.github.tatame.aftersale.ticket.application.TicketApplicationService;
import io.github.tatame.aftersale.ticket.domain.IntentType;
import io.github.tatame.aftersale.ticket.domain.Ticket;
import io.github.tatame.aftersale.ticket.domain.TicketStatus;
import io.github.tatame.aftersale.tool.domain.ToolRiskLevel;
import io.github.tatame.aftersale.trace.application.ToolCallTraceApplicationService;
import com.jayway.jsonpath.JsonPath;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class AgentRunToolBoundaryTest {

    private static final List<String> AGENT_RUN_TOOLS = List.of(
            "get_order_by_id",
            "search_aftersale_policy",
            "add_ticket_note");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TicketApplicationService ticketApplicationService;

    @Autowired
    private ToolCallTraceApplicationService traceApplicationService;

    @MockBean
    private AgentPlanner agentPlanner;

    @Test
    void plannerContextOnlyExposesAgentRunExecutableTools() throws Exception {
        Ticket ticket = ticketApplicationService.createTicket(
                "U-TOOL-BOUNDARY-1",
                "O202605130001",
                "耳机有质量问题，想退货退款。");
        when(agentPlanner.plan(any())).thenReturn(allowedPlan());

        mockMvc.perform(post("/api/tickets/{ticketId}/agent-runs", ticket.getTicketId()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("SUCCEEDED"));

        ArgumentCaptor<AgentPlanningContext> captor = ArgumentCaptor.forClass(AgentPlanningContext.class);
        verify(agentPlanner).plan(captor.capture());
        assertThat(captor.getValue().availableTools()).containsExactlyElementsOf(AGENT_RUN_TOOLS);
        assertThat(captor.getValue().availableTools())
                .doesNotContain("create_aftersale_ticket", "update_ticket_status", "get_user_orders");
    }

    @Test
    void registeredButDisallowedToolFailsBeforeToolExecutionAndMarksTicketFailed() throws Exception {
        Ticket ticket = ticketApplicationService.createTicket(
                "U-TOOL-BOUNDARY-2",
                "O202605130001",
                "耳机有质量问题，想退货退款。");
        when(agentPlanner.plan(any())).thenReturn(disallowedToolPlan());

        MvcResult result = mockMvc.perform(post("/api/tickets/{ticketId}/agent-runs", ticket.getTicketId()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("FAILED"))
                .andExpect(jsonPath("$.data.errorMessage",
                        containsString("not allowed for current AgentRun")))
                .andReturn();

        String runId = JsonPath.read(result.getResponse().getContentAsString(), "$.data.runId");
        Ticket updatedTicket = ticketApplicationService.getTicket(ticket.getTicketId());
        assertThat(updatedTicket.getStatus()).isEqualTo(TicketStatus.FAILED);
        assertThat(updatedTicket.getAgentSuggestion()).contains("create_aftersale_ticket");
        assertThat(traceApplicationService.findByRunId(runId)).isEmpty();
    }

    private static AgentPlan allowedPlan() {
        return new AgentPlan(
                IntentType.RETURN_AND_REFUND,
                ToolRiskLevel.MEDIUM,
                "质量问题 退货",
                "AgentRun allowed tool boundary test note.",
                "AgentRun allowed tool boundary test suggestion.",
                List.of("Allowed tool boundary evidence."),
                List.of(
                        new PlannedToolCall("get_order_by_id", "Fetch order facts."),
                        new PlannedToolCall("search_aftersale_policy", "Fetch policy evidence."),
                        new PlannedToolCall("add_ticket_note", "Persist agent note.")));
    }

    private static AgentPlan disallowedToolPlan() {
        return new AgentPlan(
                IntentType.RETURN_AND_REFUND,
                ToolRiskLevel.LOW,
                "质量问题 退货",
                "Disallowed tool boundary test note.",
                "Disallowed tool boundary test suggestion.",
                List.of("Disallowed tool boundary evidence."),
                List.of(new PlannedToolCall("create_aftersale_ticket", "Registered but not allowed in AgentRun.")));
    }
}
