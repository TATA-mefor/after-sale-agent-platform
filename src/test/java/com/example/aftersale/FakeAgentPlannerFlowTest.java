package com.example.aftersale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.aftersale.ticket.application.TicketApplicationService;
import com.example.aftersale.ticket.domain.Ticket;
import com.example.aftersale.trace.application.ToolCallTraceApplicationService;
import com.example.aftersale.trace.domain.ToolCallTrace;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = "agent.planner.mode=fake")
@AutoConfigureMockMvc
class FakeAgentPlannerFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TicketApplicationService ticketApplicationService;

    @Autowired
    private ToolCallTraceApplicationService traceApplicationService;

    @Test
    void fakePlannerDrivesAgentRunThroughToolRegistry() throws Exception {
        Ticket ticket = ticketApplicationService.createTicket(
                "U-FAKE-1",
                "O-FAKE-1",
                "This message is intentionally ignored by the fake planner.");

        MvcResult result = mockMvc.perform(post("/api/tickets/{ticketId}/agent-runs", ticket.getTicketId()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("SUCCEEDED"))
                .andExpect(jsonPath("$.data.intent").value("RETURN_AND_REFUND"))
                .andExpect(jsonPath("$.data.plan", containsString("Fake planner")))
                .andExpect(jsonPath("$.data.finalSuggestion", containsString("Fake planner final suggestion")))
                .andExpect(jsonPath("$.data.toolCalls", hasItems("search_aftersale_policy", "add_ticket_note")))
                .andReturn();

        String runId = JsonPath.read(result.getResponse().getContentAsString(), "$.data.runId");
        assertThat(traceApplicationService.findByRunId(runId))
                .extracting(ToolCallTrace::getToolName)
                .contains("search_aftersale_policy", "add_ticket_note");

        mockMvc.perform(get("/api/tickets/{ticketId}", ticket.getTicketId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.internalNote", containsString("Fake planner note")))
                .andExpect(jsonPath("$.data.agentSuggestion", containsString("Fake planner final suggestion")));
    }
}
