package com.example.aftersale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import com.example.aftersale.agent.domain.AgentRunStatus;
import com.example.aftersale.ticket.api.TicketCreateRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

@Tag("live")
@ActiveProfiles("mysql")
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "agent.planner.mode=llm")
@EnabledIfSystemProperty(named = "live.llm", matches = "true")
@EnabledIfSystemProperty(named = "live.mysql", matches = "true")
@EnabledIfEnvironmentVariable(named = "OPENAI_API_KEY", matches = ".+")
@EnabledIfEnvironmentVariable(named = "AFTERSALE_MYSQL_URL", matches = ".+")
@EnabledIfEnvironmentVariable(named = "AFTERSALE_MYSQL_USERNAME", matches = ".+")
@EnabledIfEnvironmentVariable(named = "AFTERSALE_MYSQL_PASSWORD", matches = ".+")
class RealAgentValidationLiveTest {

    private static final String DEFAULT_LIVE_ORDER_ID = "O202605130001";
    private static final String GET_ORDER_BY_ID = "get_order_by_id";
    private static final String SEARCH_AFTERSALE_POLICY = "search_aftersale_policy";
    private static final String ADD_TICKET_NOTE = "add_ticket_note";

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void realLlmPlannerRunsFullHttpAgentFlowAgainstMysqlSeedData() {
        String orderId = envOrDefault("AFTERSALE_LIVE_ORDER_ID", DEFAULT_LIVE_ORDER_ID);
        JsonNode ticketResponse = postTicket(orderId);
        String ticketId = text(ticketResponse.path("data").path("ticketId"));

        JsonNode agentRunResponse = postAgentRun(ticketId);
        JsonNode agentRun = agentRunResponse.path("data");
        String runId = text(agentRun.path("runId"));
        assertThat(runId).isNotBlank();

        failWithProviderHintIfNeeded(agentRunResponse.toString());
        assertThat(text(agentRun.path("status")))
                .as("AgentRun should succeed. Error summary: %s", text(agentRun.path("errorMessage")))
                .isEqualTo(AgentRunStatus.SUCCEEDED.name());

        JsonNode executionTreeResponse = get("/api/agent-runs/" + runId + "/execution-tree");
        JsonNode executionTree = executionTreeResponse.path("data");
        List<JsonNode> toolCalls = collectToolCalls(executionTree);
        assertThat(toolCalls).isNotEmpty();
        assertThat(toolCalls).extracting(toolCall -> text(toolCall.path("toolName")))
                .contains(GET_ORDER_BY_ID, SEARCH_AFTERSALE_POLICY, ADD_TICKET_NOTE);
        assertThat(toolCalls.stream()
                .filter(toolCall -> GET_ORDER_BY_ID.equals(text(toolCall.path("toolName"))))
                .anyMatch(toolCall -> text(toolCall.path("outputJson")).contains("\"orderItems\"")))
                .isTrue();

        JsonNode traceResponse = get("/api/agent-runs/" + runId + "/traces");
        JsonNode traces = traceResponse.path("data");
        assertThat(traces.isArray()).isTrue();
        assertThat(traces.size()).isPositive();
        assertThat(containsToolCall(traces, GET_ORDER_BY_ID)).isTrue();

        assertThat(combinedSummary(executionTree, agentRun))
                .containsAnyOf("Item-level", "item-level", "orderItemId", "productName", "商品明细");
        assertThat(executionTree.toString()).contains("orderItems");
    }

    private JsonNode postTicket(String orderId) {
        TicketCreateRequest request = new TicketCreateRequest(
                "U-LIVE-VALIDATION",
                orderId,
                "我买的订单里有商品出现质量问题想退货，同时也想确认能不能换货。请查看订单商品明细和售后政策。");
        ResponseEntity<String> response = restTemplate.postForEntity("/api/tickets", request, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        JsonNode body = readBody(response);
        assertThat(text(body.path("data").path("ticketId"))).isNotBlank();
        return body;
    }

    private JsonNode postAgentRun(String ticketId) {
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/tickets/" + ticketId + "/agent-runs",
                null,
                String.class);
        String body = response.getBody() == null ? "" : response.getBody();
        failWithProviderHintIfNeeded(body);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        JsonNode responseBody = readBody(response);
        assertThat(text(responseBody.path("data").path("runId"))).isNotBlank();
        return responseBody;
    }

    private JsonNode get(String path) {
        ResponseEntity<String> response = restTemplate.getForEntity(path, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return readBody(response);
    }

    private JsonNode readBody(ResponseEntity<String> response) {
        try {
            String body = response.getBody();
            assertThat(body).isNotBlank();
            return objectMapper.readTree(body);
        } catch (Exception exception) {
            fail("Failed to parse HTTP response body: " + exception.getMessage());
            throw new IllegalStateException(exception);
        }
    }

    private static List<JsonNode> collectToolCalls(JsonNode executionTree) {
        List<JsonNode> toolCalls = new ArrayList<>();
        appendArray(toolCalls, executionTree.path("toolCalls"));
        JsonNode subtasks = executionTree.path("subtasks");
        if (subtasks.isArray()) {
            subtasks.forEach(subtask -> appendArray(toolCalls, subtask.path("toolCalls")));
        }
        return toolCalls;
    }

    private static void appendArray(List<JsonNode> values, JsonNode array) {
        if (array.isArray()) {
            array.forEach(values::add);
        }
    }

    private static boolean containsToolCall(JsonNode traces, String toolName) {
        if (!traces.isArray()) {
            return false;
        }
        for (JsonNode trace : traces) {
            if (toolName.equals(text(trace.path("toolName")))) {
                return true;
            }
        }
        return false;
    }

    private static String combinedSummary(JsonNode executionTree, JsonNode agentRun) {
        return String.join(
                " ",
                text(executionTree.path("finalSuggestion")),
                text(executionTree.path("rootSummary")),
                text(agentRun.path("finalSuggestion")),
                executionTree.path("subtasks").toString());
    }

    private static void failWithProviderHintIfNeeded(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        if (lower.contains("insufficient_balance")
                || lower.contains("insufficient balance")
                || lower.contains("insufficient quota")
                || lower.contains("http 403")) {
            fail("Live LLM provider/account-balance error. Check provider billing or API access. Summary: "
                    + sanitize(text));
        }
    }

    private static String sanitize(String value) {
        String sanitized = value.replaceAll("sk-[A-Za-z0-9_-]+", "sk-***");
        if (sanitized.length() > 800) {
            return sanitized.substring(0, 800);
        }
        return sanitized;
    }

    private static String text(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return "";
        }
        return node.asText("");
    }

    private static String envOrDefault(String name, String defaultValue) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value;
    }
}
