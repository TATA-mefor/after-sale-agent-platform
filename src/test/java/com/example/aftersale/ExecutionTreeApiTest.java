package com.example.aftersale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.aftersale.ticket.application.TicketApplicationService;
import com.example.aftersale.ticket.domain.Ticket;
import com.jayway.jsonpath.JsonPath;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class ExecutionTreeApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TicketApplicationService ticketApplicationService;

    @Test
    void singleIntentExecutionTreeReturnsRootAndUnassignedToolCalls() throws Exception {
        Ticket ticket = ticketApplicationService.createTicket(
                "U-EXEC-TREE-1",
                "O202605130001",
                "我买的耳机有质量问题，左耳没声音，想退货退款。");

        MvcResult runResult = mockMvc.perform(post("/api/tickets/{ticketId}/agent-runs", ticket.getTicketId()))
                .andExpect(status().isCreated())
                .andReturn();
        String runId = JsonPath.read(runResult.getResponse().getContentAsString(), "$.data.runId");

        mockMvc.perform(get("/api/agent-runs/{runId}/execution-tree", runId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.runId").value(runId))
                .andExpect(jsonPath("$.data.ticketId").value(ticket.getTicketId()))
                .andExpect(jsonPath("$.data.agentRunStatus").value("SUCCEEDED"))
                .andExpect(jsonPath("$.data.finalSuggestion", containsString("Workspace summary")))
                .andExpect(jsonPath("$.data.rootSummary", containsString("Workspace counts")))
                .andExpect(jsonPath("$.data.subtasks", empty()))
                .andExpect(jsonPath("$.data.approvalRequests", empty()))
                .andExpect(jsonPath("$.data.toolCalls[*].toolName", hasItems(
                        "get_order_by_id",
                        "search_aftersale_policy",
                        "add_ticket_note")))
                .andExpect(jsonPath("$.data.toolCalls[*].status", hasItems("SUCCEEDED")))
                .andExpect(jsonPath("$.data.toolCalls[?(@.toolName == 'get_order_by_id')].inputJson",
                        hasItems(containsString("\"orderId\""))))
                .andExpect(jsonPath("$.data.policyEvidence[0].retrievalMode").value("KEYWORD"))
                .andExpect(jsonPath("$.data.policyEvidence[0].source").value("KEYWORD_POLICY"))
                .andExpect(jsonPath("$.data.policyEvidence[0].score").exists())
                .andExpect(jsonPath("$.data.policyEvidence[0].toolCallId").isNotEmpty())
                .andExpect(jsonPath("$.data.errors", empty()));
    }

    @Test
    void multiIntentExecutionTreeReturnsSubtaskNodesWithToolCalls() throws Exception {
        Ticket ticket = ticketApplicationService.createTicket(
                "U-EXEC-TREE-2",
                "O202605130001",
                "我买了三件衣服，其中一件有污渍要退货，另一件要换尺码，还有一张优惠券没用上怎么退？");

        MvcResult runResult = mockMvc.perform(post("/api/tickets/{ticketId}/agent-runs", ticket.getTicketId()))
                .andExpect(status().isCreated())
                .andReturn();
        String runId = JsonPath.read(runResult.getResponse().getContentAsString(), "$.data.runId");

        MvcResult treeResult = mockMvc.perform(get("/api/agent-runs/{runId}/execution-tree", runId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.subtasks[*].subtaskId", hasItems(
                        "subtask-1",
                        "subtask-2",
                        "subtask-3")))
                .andExpect(jsonPath("$.data.toolCalls", empty()))
                .andReturn();

        List<Map<String, Object>> subtasks = JsonPath.read(
                treeResult.getResponse().getContentAsString(),
                "$.data.subtasks");
        assertThat(subtasks).hasSize(3);
        assertSubtaskToolCalls(subtasks, "subtask-1");
        assertSubtaskToolCalls(subtasks, "subtask-2");
        assertSubtaskToolCalls(subtasks, "subtask-3");
        assertSubtaskPolicyEvidence(subtasks, "subtask-1");
        assertSubtaskPolicyEvidence(subtasks, "subtask-2");
        assertSubtaskHasNoFabricatedPolicyEvidence(subtasks, "subtask-3");
    }

    @Test
    void missingRunReturnsClearNotFoundError() throws Exception {
        mockMvc.perform(get("/api/agent-runs/{runId}/execution-tree", "RUN-NOT-FOUND-EXEC-TREE"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("AGENT_RUN_NOT_FOUND"))
                .andExpect(jsonPath("$.message", containsString("AgentRun not found")));
    }

    @SuppressWarnings("unchecked")
    private static void assertSubtaskToolCalls(List<Map<String, Object>> subtasks, String subtaskId) {
        Map<String, Object> subtask = subtasks.stream()
                .filter(item -> subtaskId.equals(item.get("subtaskId")))
                .findFirst()
                .orElseThrow();
        List<Map<String, Object>> toolCalls = (List<Map<String, Object>>) subtask.get("toolCalls");
        assertThat(toolCalls)
                .extracting(item -> item.get("toolName"))
                .containsExactlyInAnyOrder("get_order_by_id", "search_aftersale_policy", "add_ticket_note");
        assertThat(toolCalls)
                .extracting(item -> item.get("inputJson"))
                .allSatisfy(inputJson -> assertThat(inputJson.toString()).contains(subtaskId));
    }

    @SuppressWarnings("unchecked")
    private static void assertSubtaskPolicyEvidence(List<Map<String, Object>> subtasks, String subtaskId) {
        Map<String, Object> subtask = subtasks.stream()
                .filter(item -> subtaskId.equals(item.get("subtaskId")))
                .findFirst()
                .orElseThrow();
        List<Map<String, Object>> evidence = (List<Map<String, Object>>) subtask.get("policyEvidence");
        assertThat(evidence).isNotEmpty();
        assertThat(evidence)
                .allSatisfy(item -> {
                    assertThat(item.get("subtaskId")).isEqualTo(subtaskId);
                    assertThat(item.get("toolCallId")).isNotNull();
                    assertThat(item.get("retrievalMode")).isEqualTo("KEYWORD");
                    assertThat(item.get("source")).isEqualTo("KEYWORD_POLICY");
                    assertThat(item.get("snippet").toString()).doesNotContain("rawText", "password", "token");
                });
    }

    @SuppressWarnings("unchecked")
    private static void assertSubtaskHasNoFabricatedPolicyEvidence(
            List<Map<String, Object>> subtasks, String subtaskId) {
        Map<String, Object> subtask = subtasks.stream()
                .filter(item -> subtaskId.equals(item.get("subtaskId")))
                .findFirst()
                .orElseThrow();
        List<Map<String, Object>> evidence = (List<Map<String, Object>>) subtask.get("policyEvidence");
        assertThat(evidence).isEmpty();
        List<Map<String, Object>> toolCalls = (List<Map<String, Object>>) subtask.get("toolCalls");
        assertThat(toolCalls)
                .filteredOn(item -> "search_aftersale_policy".equals(item.get("toolName")))
                .singleElement()
                .satisfies(item -> assertThat(item.get("outputJson").toString())
                        .contains("\"evidences\":[]")
                        .contains("No after-sale policy matched the query."));
    }
}
