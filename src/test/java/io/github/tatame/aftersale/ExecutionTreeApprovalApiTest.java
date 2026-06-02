package io.github.tatame.aftersale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.tatame.aftersale.approval.application.ApprovalApplicationService;
import io.github.tatame.aftersale.approval.domain.ApprovalRequest;
import io.github.tatame.aftersale.approval.domain.ApprovalStatus;
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
import com.jayway.jsonpath.JsonPath;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class ExecutionTreeApprovalApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TicketApplicationService ticketApplicationService;

    @Autowired
    private ApprovalApplicationService approvalApplicationService;

    @MockBean
    private AgentPlanner agentPlanner;

    @Test
    void highRiskApprovalAppearsInExecutionTreeAndQueryIsReadOnly() throws Exception {
        Ticket ticket = ticketApplicationService.createTicket(
                "U-EXEC-TREE-APPROVAL-1",
                "O202605130001",
                "我要马上退款，金额较高，需要人工确认。");
        when(agentPlanner.plan(any())).thenReturn(highRiskPlan());

        MvcResult runResult = mockMvc.perform(post("/api/tickets/{ticketId}/agent-runs", ticket.getTicketId()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("SUCCEEDED"))
                .andReturn();
        String runId = JsonPath.read(runResult.getResponse().getContentAsString(), "$.data.runId");

        ApprovalRequest approvalRequest = approvalApplicationService.findPending().stream()
                .filter(request -> request.getTicketId().equals(ticket.getTicketId()))
                .findFirst()
                .orElseThrow();
        TicketStatus statusBeforeQuery = ticketApplicationService.getTicket(ticket.getTicketId()).getStatus();

        mockMvc.perform(get("/api/agent-runs/{runId}/execution-tree", runId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.subtasks[0].subtaskId").value("subtask-high-risk-refund"))
                .andExpect(jsonPath("$.data.subtasks[0].status").value("WAITING_APPROVAL"))
                .andExpect(jsonPath("$.data.subtasks[0].approvalRequests[0].approvalRequestId")
                        .value(approvalRequest.getApprovalId()))
                .andExpect(jsonPath("$.data.subtasks[0].approvalRequests[0].status").value("PENDING"))
                .andExpect(jsonPath("$.data.subtasks[0].approvalRequests[0].reason",
                        containsString("requires human approval")));

        assertThat(ticketApplicationService.getTicket(ticket.getTicketId()).getStatus())
                .isEqualTo(statusBeforeQuery)
                .isEqualTo(TicketStatus.WAITING_HUMAN_APPROVAL);
        assertThat(approvalApplicationService.getById(approvalRequest.getApprovalId()).getStatus())
                .isEqualTo(ApprovalStatus.PENDING);
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
