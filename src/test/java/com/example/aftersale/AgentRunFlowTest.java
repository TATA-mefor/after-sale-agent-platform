package com.example.aftersale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.aftersale.ticket.application.TicketApplicationService;
import com.example.aftersale.ticket.domain.Ticket;
import com.example.aftersale.ticket.domain.TicketStatus;
import com.example.aftersale.tool.application.ToolRegistry;
import com.example.aftersale.tool.application.ToolTraceContext;
import com.example.aftersale.tool.domain.ToolInput;
import com.example.aftersale.trace.application.ToolCallTraceApplicationService;
import com.example.aftersale.trace.domain.ToolCallStatus;
import com.example.aftersale.trace.domain.ToolCallTrace;
import com.jayway.jsonpath.JsonPath;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class AgentRunFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TicketApplicationService ticketApplicationService;

    @Autowired
    private ToolCallTraceApplicationService traceApplicationService;

    @Autowired
    private ToolRegistry toolRegistry;

    @Test
    void demoFlowCreatesTicketRunsAgentUpdatesTicketAndExposesTrace() throws Exception {
        MvcResult ticketResult = mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": "U-DEMO-1",
                                  "orderId": "O202605130001",
                                  "message": "我买的耳机有质量问题，左耳没声音，想退货退款。"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.status").value("CREATED"))
                .andReturn();
        String ticketId = JsonPath.read(ticketResult.getResponse().getContentAsString(), "$.data.ticketId");

        MvcResult agentRunResult = mockMvc.perform(post("/api/tickets/{ticketId}/agent-runs", ticketId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.status").value("SUCCEEDED"))
                .andExpect(jsonPath("$.data.intent").value("RETURN_AND_REFUND"))
                .andExpect(jsonPath("$.data.toolCalls", hasItems("search_aftersale_policy", "add_ticket_note")))
                .andReturn();
        String runId = JsonPath.read(agentRunResult.getResponse().getContentAsString(), "$.data.runId");

        mockMvc.perform(get("/api/tickets/{ticketId}", ticketId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("RESOLVED"))
                .andExpect(jsonPath("$.data.intentType").value("RETURN_AND_REFUND"))
                .andExpect(jsonPath("$.data.internalNote", containsString("RETURN_AND_REFUND")))
                .andExpect(jsonPath("$.data.agentSuggestion", containsString("RETURN_AND_REFUND")));

        mockMvc.perform(get("/api/agent-runs/{runId}/traces", runId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].toolName", hasItems("search_aftersale_policy", "add_ticket_note")))
                .andExpect(jsonPath("$.data[*].status", hasItems("SUCCEEDED")));
    }

    @Test
    void triggerAgentRunSucceedsAndReturnsStructuredOutput() throws Exception {
        Ticket ticket = ticketApplicationService.createTicket(
                "U-7001",
                "O-7001",
                "我买的耳机有质量问题，左耳没声音，想退货退款。");

        MvcResult result = mockMvc.perform(post("/api/tickets/{ticketId}/agent-runs", ticket.getTicketId()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.status").value("SUCCEEDED"))
                .andExpect(jsonPath("$.data.intent").value("RETURN_AND_REFUND"))
                .andExpect(jsonPath("$.data.plan", containsString("search_aftersale_policy")))
                .andExpect(jsonPath("$.data.finalSuggestion", containsString("RETURN_AND_REFUND")))
                .andExpect(jsonPath("$.data.evidence[0]", containsString("POL-QUALITY-RETURN-EXCHANGE")))
                .andExpect(jsonPath("$.data.toolCalls", hasItems("search_aftersale_policy", "add_ticket_note")))
                .andReturn();

        String runId = JsonPath.read(result.getResponse().getContentAsString(), "$.data.runId");
        assertThat(traceApplicationService.findByRunId(runId))
                .extracting(ToolCallTrace::getToolName)
                .contains("search_aftersale_policy", "add_ticket_note");
    }

    @Test
    void traceQueryReturnsToolCallRecords() throws Exception {
        Ticket ticket = ticketApplicationService.createTicket(
                "U-7002",
                "O-7002",
                "物流显示签收了，但是我没收到货。");

        MvcResult runResult = mockMvc.perform(post("/api/tickets/{ticketId}/agent-runs", ticket.getTicketId()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.intent").value("LOGISTICS_ISSUE"))
                .andReturn();
        String runId = JsonPath.read(runResult.getResponse().getContentAsString(), "$.data.runId");

        mockMvc.perform(get("/api/agent-runs/{runId}/traces", runId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data[*].toolName", hasItems("search_aftersale_policy", "add_ticket_note")))
                .andExpect(jsonPath("$.data[0].runId").value(runId))
                .andExpect(jsonPath("$.data[?(@.toolName == 'search_aftersale_policy')].inputJson",
                        hasItem(containsString("\"query\""))))
                .andExpect(jsonPath("$.data[?(@.toolName == 'search_aftersale_policy')].outputJson",
                        hasItem(containsString("\"results\""))))
                .andExpect(jsonPath("$.data[?(@.toolName == 'add_ticket_note')].inputJson",
                        hasItem(containsString("\"note\""))))
                .andExpect(jsonPath("$.data[*].status", hasItems("SUCCEEDED", "SUCCEEDED")));
    }

    @Test
    void agentRunFailureRecordsFailedStatus() throws Exception {
        Ticket ticket = ticketApplicationService.createTicket("U-7003", "O-7003", "这个工单先关闭。");
        ticketApplicationService.updateTicketStatus(ticket.getTicketId(), TicketStatus.RESOLVED, "Already handled.");
        ticketApplicationService.updateTicketStatus(ticket.getTicketId(), TicketStatus.CLOSED, null);

        mockMvc.perform(post("/api/tickets/{ticketId}/agent-runs", ticket.getTicketId()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("FAILED"))
                .andExpect(jsonPath("$.data.errorMessage", containsString("terminal")));
    }

    @Test
    void failedToolCallIsRecordedInTrace() {
        String runId = "RUN-FAILED-TOOL-TEST";

        ToolTraceContext.runWith(runId, () -> toolRegistry.execute("add_ticket_note", ToolInput.of(Map.of(
                "ticketId", "T-MISSING-M7",
                "note", "This tool call should fail."))));

        List<ToolCallTrace> traces = traceApplicationService.findByRunId(runId);
        assertThat(traces).hasSize(1);
        assertThat(traces.get(0).getToolName()).isEqualTo("add_ticket_note");
        assertThat(traces.get(0).getStatus()).isEqualTo(ToolCallStatus.FAILED);
        assertThat(traces.get(0).getErrorMessage()).contains("T-MISSING-M7");
    }

    @Test
    void toolTraceContextClearsRunIdAfterActionFails() {
        assertThat(ToolTraceContext.currentRunId()).isEmpty();

        assertThatThrownBy(() -> ToolTraceContext.runWith("RUN-CLEANUP-TEST", () -> {
            assertThat(ToolTraceContext.currentRunId()).contains("RUN-CLEANUP-TEST");
            throw new IllegalStateException("boom");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(ToolTraceContext.currentRunId()).isEmpty();
    }
}
