package com.example.aftersale.agent.infrastructure.llm;

import com.example.aftersale.agent.application.planner.AgentPlanValidationException;
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

public class OpenAiLlmClient implements LlmClient {

    private static final String AGENT_PLAN_SCHEMA_NAME = "aftersale_agent_plan";

    private final AgentPlannerProperties.Llm properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @SuppressFBWarnings(
            value = "EI_EXPOSE_REP2",
            justification = "Spring configuration properties are stored for provider settings only.")
    public OpenAiLlmClient(AgentPlannerProperties.Llm properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newHttpClient();
    }

    @Override
    public LlmResponse complete(LlmRequest request) {
        try {
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(properties.getEndpoint()))
                    .timeout(Duration.ofSeconds(request.timeoutSeconds()))
                    .header("Authorization", "Bearer " + properties.getApiKey())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(toJson(requestBody(request))))
                    .build();
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new AgentPlanValidationException(
                        "LLM provider returned HTTP " + response.statusCode() + ": " + response.body());
            }
            return new LlmResponse(extractText(response.body()));
        } catch (IOException exception) {
            throw new AgentPlanValidationException("LLM provider call failed", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AgentPlanValidationException("LLM provider call was interrupted", exception);
        }
    }

    private Map<String, Object> requestBody(LlmRequest request) {
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
                        "plannedTools"),
                "properties", Map.of(
                        "intent", Map.of("type", "string"),
                        "riskLevel", Map.of("type", "string"),
                        "policyQuery", Map.of("type", "string"),
                        "noteToAdd", Map.of("type", "string"),
                        "finalSuggestion", Map.of("type", "string"),
                        "evidenceHints", Map.of("type", "array", "items", Map.of("type", "string")),
                        "plannedTools", Map.of(
                                "type", "array",
                                "items", Map.of(
                                        "type", "object",
                                        "additionalProperties", false,
                                        "required", List.of("toolName", "reason"),
                                        "properties", Map.of(
                                                "toolName", Map.of("type", "string"),
                                                "reason", Map.of("type", "string"))))));
    }

    private String extractText(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode outputText = root.get("output_text");
            if (outputText != null && outputText.isTextual() && !outputText.asText().isBlank()) {
                return outputText.asText();
            }
            List<String> texts = new ArrayList<>();
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
