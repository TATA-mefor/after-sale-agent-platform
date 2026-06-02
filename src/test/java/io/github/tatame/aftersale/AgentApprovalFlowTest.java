package io.github.tatame.aftersale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.tatame.aftersale.approval.application.ApprovalApplicationService;
import io.github.tatame.aftersale.agent.application.planner.AgentPlan;
import io.github.tatame.aftersale.agent.application.planner.AgentPlanner;
import io.github.tatame.aftersale.agent.application.planner.AgentSubtask;
import io.github.tatame.aftersale.agent.application.planner.PlannedToolCall;
import io.github.tatame.aftersale.agent.application.planner.SubtaskType;
import io.github.tatame.aftersale.ticket.application.TicketApplicationService;
import io.github.tatame.aftersale.ticket.domain.IntentType;
import io.github.tatame.aftersale.ticket.domain.Ticket;
import io.github.tatame.aftersale.ticket.domain.TicketStatus;
import io.github.tatame.aftersale.tool.domain.ToolRiskLevel;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class AgentApprovalFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TicketApplicationService ticketApplicationService;

    @Autowired
    private ApprovalApplicationService approvalApplicationService;

    @MockBean
    private AgentPlanner agentPlanner;

    @Test
    void highRiskSubtaskCreatesApprovalRequestAndLeavesTicketWaitingForApproval() throws Exception {
        Ticket ticket = ticketApplicationService.createTicket(
                "U-AGENT-APPROVAL-1",
                "O202605130001",
                "我要马上退款，金额较高，需要人工确认。");
        when(agentPlanner.plan(any())).thenReturn(highRiskPlan());

        mockMvc.perform(post("/api/tickets/{ticketId}/agent-runs", ticket.getTicketId()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("SUCCEEDED"))
                .andExpect(jsonPath("$.data.finalSuggestion", containsString("requires human approval")))
                .andExpect(jsonPath("$.data.toolCalls").isEmpty());

        Ticket updatedTicket = ticketApplicationService.getTicket(ticket.getTicketId());
        assertThat(updatedTicket.getStatus()).isEqualTo(TicketStatus.WAITING_HUMAN_APPROVAL);
        assertThat(updatedTicket.getInternalNote()).contains("Approval request created");

        assertThat(approvalApplicationService.findPending())
                .filteredOn(request -> request.getTicketId().equals(ticket.getTicketId()))
                .singleElement()
                .satisfies(request -> {
                    assertThat(request.getSubtaskId()).isEqualTo("subtask-high-risk-refund");
                    assertThat(request.getRiskLevel()).isEqualTo(ToolRiskLevel.HIGH);
                    assertThat(request.getRequestedAction()).contains("requires human approval");
                });
    }

    private static AgentPlan highRiskPlan() {
        AgentSubtask subtask = new AgentSubtask(
                "subtask-high-risk-refund",
                SubtaskType.RETURN,
                "high risk refund",
                "我要马上退款",
                1,
                ToolRiskLevel.HIGH,
                "高风险 退款 人工确认",
                List.of(new PlannedToolCall("search_aftersale_policy", "Policy evidence before review.")),
                List.of());
        return new AgentPlan(
                IntentType.MULTI_INTENT,
                ToolRiskLevel.HIGH,
                "高风险 退款 人工确认",
                "High risk refund requires manual approval.",
                "High risk refund requires human approval.",
                List.of("High risk request."),
                List.of(),
                List.of(subtask));
    }
}
