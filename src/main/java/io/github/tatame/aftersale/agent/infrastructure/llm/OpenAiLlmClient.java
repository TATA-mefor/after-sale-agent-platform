package io.github.tatame.aftersale.agent.infrastructure.llm;

import io.github.tatame.aftersale.agent.application.planner.AgentPlanValidationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 调用 OpenAI Responses API，并提取结构化 AgentPlan 文本。
 *
 * <p>边界：本 Client 只负责传输。它向 Provider 发送 prompt 并返回文本；解析、校验、工具执行和领域变更
 * 都在 Client 之外完成。
 */
public class OpenAiLlmClient implements LlmClient {

    private static final String AGENT_PLAN_SCHEMA_NAME = "aftersale_agent_plan";

    private final LlmProviderSettings settings;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @SuppressFBWarnings(
            value = "EI_EXPOSE_REP2",
            justification = "Spring configuration properties are stored for provider settings only.")
    public OpenAiLlmClient(AgentPlannerProperties.Llm properties, ObjectMapper objectMapper) {
        this(new LlmProviderSettings(
                LlmProvider.OPENAI_RESPONSES,
                properties.getModel(),
                properties.getApiKey(),
                properties.getEndpoint(),
                properties.getTimeoutSeconds()), objectMapper);
    }

    public OpenAiLlmClient(LlmProviderSettings settings, ObjectMapper objectMapper) {
        this(settings, objectMapper, HttpClient.newHttpClient());
    }

    OpenAiLlmClient(LlmProviderSettings settings, ObjectMapper objectMapper, HttpClient httpClient) {
        this.settings = settings;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    /**
     * 向 Provider 发送一次结构化输出请求。
     *
     * <p>Provider 错误会通过 LlmProviderErrorFormatter 汇总，避免密钥和过长响应体进入日志或
     * AgentRun 失败文本。
     */
    @Override
    public LlmResponse complete(LlmRequest request) {
        try {
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(settings.endpoint()))
                    .timeout(Duration.ofSeconds(request.timeoutSeconds()))
                    .header("Authorization", "Bearer " + settings.apiKey())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(toJson(requestBody(request))))
                    .build();
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new AgentPlanValidationException(LlmProviderErrorFormatter.format(
                        settings,
                        response.statusCode(),
                        response.body()));
            }
            return new LlmResponse(extractText(response.body()));
        } catch (IOException exception) {
            throw new AgentPlanValidationException(
                    LlmProviderErrorFormatter.formatCallFailure(settings, exception.getMessage()),
                    exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AgentPlanValidationException("LLM provider call was interrupted", exception);
        }
    }

    Map<String, Object> requestBody(LlmRequest request) {
        return Map.of(
                "model", request.model(),
                "input", List.of(
                        Map.of("role", "system", "content", request.systemPrompt()),
                        Map.of("role", "user", "content", request.userPrompt())),
                "text", Map.of(
                        "format", Map.of(
                                "type", "json_schema",
                                "name", AGENT_PLAN_SCHEMA_NAME,
                                "strict", true,
                                "schema", schema())));
    }

    private static Map<String, Object> schema() {
        return Map.of(
                "type", "object",
                "additionalProperties", false,
                "required", List.of(
                        "intent",
                        "riskLevel",
                        "policyQuery",
                        "noteToAdd",
                        "finalSuggestion",
                        "evidenceHints",
                        "plannedTools",
                        "subtasks"),
                "properties", Map.of(
                        "intent", Map.of("type", "string"),
                        "riskLevel", Map.of("type", "string"),
                        "policyQuery", Map.of("type", "string"),
                        "noteToAdd", Map.of("type", "string"),
                        "finalSuggestion", Map.of("type", "string"),
                        "evidenceHints", Map.of("type", "array", "items", Map.of("type", "string")),
                        "plannedTools", plannedToolArraySchema(),
                        "subtasks", Map.of(
                                "type", "array",
                                "items", subtaskSchema())));
    }

    private static Map<String, Object> plannedToolArraySchema() {
        return Map.of(
                "type", "array",
                "items", plannedToolSchema());
    }

    private static Map<String, Object> plannedToolSchema() {
        return Map.of(
                "type", "object",
                "additionalProperties", false,
                "required", List.of("toolName", "reason"),
                "properties", Map.of(
                        "toolName", Map.of("type", "string"),
                        "reason", Map.of("type", "string")));
    }

    private static Map<String, Object> subtaskSchema() {
        return Map.of(
                "type", "object",
                "additionalProperties", false,
                "required", List.of(
                        "subtaskId",
                        "type",
                        "target",
                        "userMessageFragment",
                        "priority",
                        "riskLevel",
                        "policyQuery",
                        "plannedTools",
                        "dependencies"),
                "properties", Map.of(
                        "subtaskId", Map.of("type", "string"),
                        "type", Map.of("type", "string"),
                        "target", Map.of("type", "string"),
                        "userMessageFragment", Map.of("type", "string"),
                        "priority", Map.of("type", "integer"),
                        "riskLevel", Map.of("type", "string"),
                        "policyQuery", Map.of("type", "string"),
                        "plannedTools", plannedToolArraySchema(),
                        "dependencies", Map.of("type", "array", "items", Map.of("type", "string"))));
    }

    String extractText(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode outputText = root.get("output_text");
            if (outputText != null && outputText.isTextual() && !outputText.asText().isBlank()) {
                return outputText.asText();
            }
            List<String> texts = new ArrayList<>();
            // 兼容 Responses 的 Provider 可能把生成文本嵌套在 output/content 结构中。
            collectText(root, texts);
            if (!texts.isEmpty()) {
                return String.join("\n", texts);
            }
            throw new AgentPlanValidationException("LLM provider response did not contain text output");
        } catch (JsonProcessingException exception) {
            throw new AgentPlanValidationException("LLM provider response is not valid JSON", exception);
        }
    }

    private static void collectText(JsonNode node, List<String> texts) {
        if (node == null) {
            return;
        }
        JsonNode text = node.get("text");
        if (text != null && text.isTextual() && !text.asText().isBlank()) {
            texts.add(text.asText());
        }
        JsonNode content = node.get("content");
        if (content != null && content.isArray()) {
            content.forEach(child -> collectText(child, texts));
        }
        JsonNode output = node.get("output");
        if (output != null && output.isArray()) {
            output.forEach(child -> collectText(child, texts));
        }
        JsonNode choices = node.get("choices");
        if (choices != null && choices.isArray()) {
            choices.forEach(child -> collectText(child, texts));
        }
        JsonNode message = node.get("message");
        if (message != null) {
            collectText(message, texts);
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new AgentPlanValidationException("Failed to serialize LLM request", exception);
        }
    }
}
