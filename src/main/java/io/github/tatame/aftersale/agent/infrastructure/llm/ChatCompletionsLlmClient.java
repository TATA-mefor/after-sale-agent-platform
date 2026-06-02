package io.github.tatame.aftersale.agent.infrastructure.llm;

import io.github.tatame.aftersale.agent.application.planner.AgentPlanValidationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * 调用兼容 Chat Completions 的 Provider 来生成 AgentPlan。
 *
 * <p>边界：本 Client 只适配 Provider 传输，不解析业务策略、不执行工具、不持久化 prompt，
 * 也不在 Provider 错误消息中暴露凭证。
 */
public class ChatCompletionsLlmClient implements LlmClient {

    private final LlmProviderSettings settings;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public ChatCompletionsLlmClient(LlmProviderSettings settings, ObjectMapper objectMapper) {
        this(settings, objectMapper, HttpClient.newHttpClient());
    }

    ChatCompletionsLlmClient(LlmProviderSettings settings, ObjectMapper objectMapper, HttpClient httpClient) {
        this.settings = settings;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    /**
     * 发送一次 JSON-object 规划请求，并返回供下游解析的 Provider 文本。
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
                "messages", List.of(
                        Map.of("role", "system", "content", request.systemPrompt()),
                        Map.of("role", "user", "content", request.userPrompt())),
                "response_format", Map.of("type", "json_object"));
    }

    String extractText(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode content = root.path("choices").path(0).path("message").path("content");
            if (content.isTextual() && !content.asText().isBlank()) {
                return content.asText();
            }
            throw new AgentPlanValidationException("LLM chat response did not contain choices[0].message.content");
        } catch (JsonProcessingException exception) {
            throw new AgentPlanValidationException("LLM chat response is not valid JSON", exception);
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new AgentPlanValidationException("Failed to serialize LLM chat request", exception);
        }
    }
}
