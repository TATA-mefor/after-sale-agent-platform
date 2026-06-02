package io.github.tatame.aftersale.tool.application.policy;

import io.github.tatame.aftersale.policy.rag.application.RagPolicySearchApplicationService;
import io.github.tatame.aftersale.policy.rag.search.RagPolicyEvidence;
import io.github.tatame.aftersale.policy.rag.search.RagPolicySearchQuery;
import io.github.tatame.aftersale.policy.rag.search.RagPolicySearchResult;
import io.github.tatame.aftersale.policy.rag.search.RetrievalMode;
import io.github.tatame.aftersale.tool.application.ToolExecutor;
import io.github.tatame.aftersale.tool.domain.ToolDefinition;
import io.github.tatame.aftersale.tool.domain.ToolExecutionStatus;
import io.github.tatame.aftersale.tool.domain.ToolInput;
import io.github.tatame.aftersale.tool.domain.ToolOutput;
import io.github.tatame.aftersale.tool.domain.ToolRiskLevel;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * 提供低风险售后政策检索工具，为 Agent 输出可追溯政策证据。
 *
 * <p>边界：工具只返回政策片段和匹配原因，不根据政策直接执行退款、换货或补偿动作。
 */
@Component
public class SearchAfterSalePolicyToolExecutor implements ToolExecutor {

    private static final ToolDefinition DEFINITION = ToolDefinition.of(
            "search_aftersale_policy",
            "Search after-sale policy evidence by KEYWORD, VECTOR, or HYBRID retrieval.",
            "{\"query\":\"string\",\"retrievalMode\":\"KEYWORD|VECTOR|HYBRID\",\"topK\":\"number\"}",
            "{\"results\":[{\"policyId\":\"string\",\"category\":\"string\","
                    + "\"productType\":\"string\",\"matchedText\":\"string\",\"matchReason\":\"string\"}],"
                    + "\"evidences\":[{\"evidenceId\":\"string\",\"snippet\":\"string\",\"score\":\"number\"}],"
                    + "\"retrievalMode\":\"string\",\"message\":\"string\",\"fallbackUsed\":\"boolean\"}",
            ToolRiskLevel.LOW);

    private static final String INVALID_INPUT = "INVALID_POLICY_SEARCH_INPUT";

    private final RagPolicySearchApplicationService ragPolicySearchApplicationService;

    @SuppressFBWarnings(
            value = "EI_EXPOSE_REP2",
            justification = "Spring constructor injection intentionally stores the application service dependency.")
    public SearchAfterSalePolicyToolExecutor(RagPolicySearchApplicationService ragPolicySearchApplicationService) {
        this.ragPolicySearchApplicationService = ragPolicySearchApplicationService;
    }

    @Override
    public ToolDefinition definition() {
        return DEFINITION;
    }

    @Override
    public ToolOutput execute(ToolInput input) {
        RagPolicySearchQuery query;
        try {
            query = toQuery(input);
        } catch (IllegalArgumentException exception) {
            return ToolOutput.failure(DEFINITION.toolName(), INVALID_INPUT, sanitizeInputFailure(exception));
        }

        RagPolicySearchResult result = ragPolicySearchApplicationService.search(query);
        if (query.retrievalMode() == RetrievalMode.VECTOR && result.fallbackUsed() && result.evidences().isEmpty()) {
            return ToolOutput.failure(DEFINITION.toolName(), "VECTOR_POLICY_SEARCH_UNAVAILABLE", result.message());
        }

        return new ToolOutput(
                DEFINITION.toolName(),
                ToolExecutionStatus.SUCCEEDED,
                toOutputData(result),
                null,
                "ok");
    }

    private static RagPolicySearchQuery toQuery(ToolInput input) {
        Map<String, Object> arguments = input.arguments();
        String query = requireString(arguments, "query");
        RetrievalMode retrievalMode = RetrievalMode.parse(optionalString(arguments, "retrievalMode").orElse(null));
        int topK = optionalInt(arguments, "topK").orElse(RagPolicySearchQuery.DEFAULT_TOP_K);
        Double minScore = optionalDouble(arguments, "minScore").orElse(null);
        return new RagPolicySearchQuery(
                query,
                retrievalMode,
                topK,
                minScore,
                optionalString(arguments, "category").orElse(null),
                optionalString(arguments, "productType").orElse(null),
                optionalDate(arguments, "effectiveAt").orElse(null),
                optionalString(arguments, "embeddingModel").orElse(null),
                retrievalMode != RetrievalMode.VECTOR,
                retrievalMode != RetrievalMode.KEYWORD);
    }

    private static Map<String, Object> toOutputData(RagPolicySearchResult result) {
        Map<String, Object> data = new LinkedHashMap<>();
        List<Map<String, Object>> evidences = result.evidences().stream()
                .map(SearchAfterSalePolicyToolExecutor::toEvidenceMap)
                .toList();
        data.put("query", result.query());
        data.put("retrievalMode", result.retrievalMode().name());
        data.put("results", evidences.stream()
                .map(SearchAfterSalePolicyToolExecutor::toCompatibleResultMap)
                .toList());
        data.put("evidences", evidences);
        data.put("message", result.message());
        data.put("fallbackUsed", result.fallbackUsed());
        data.put("totalKeywordMatches", result.totalKeywordMatches());
        data.put("totalVectorMatches", result.totalVectorMatches());
        return data;
    }

    private static Map<String, Object> toEvidenceMap(RagPolicyEvidence evidence) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("evidenceId", evidence.evidenceId());
        putIfPresent(data, "policyId", evidence.policyId());
        putIfPresent(data, "documentId", evidence.documentId());
        putIfPresent(data, "chunkId", evidence.chunkId());
        putIfPresent(data, "documentTitle", evidence.documentTitle());
        data.put("category", evidence.category());
        data.put("productType", evidence.productType());
        data.put("snippet", evidence.snippet());
        data.put("score", evidence.score());
        putIfPresent(data, "keywordScore", evidence.keywordScore());
        putIfPresent(data, "vectorScore", evidence.vectorScore());
        data.put("retrievalMode", evidence.retrievalMode().name());
        data.put("source", evidence.source().name());
        putIfPresent(data, "effectiveFrom", evidence.effectiveFrom());
        putIfPresent(data, "effectiveTo", evidence.effectiveTo());
        return data;
    }

    private static Map<String, Object> toCompatibleResultMap(Map<String, Object> evidence) {
        Map<String, Object> data = new LinkedHashMap<>(evidence);
        data.put("matchedText", evidence.get("snippet"));
        data.put("matchReason", evidence.get("source"));
        return data;
    }

    private static String requireString(Map<String, Object> arguments, String fieldName) {
        Object value = arguments.get(fieldName);
        if (!(value instanceof String text) || text.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must be a non-blank string");
        }
        return text.trim();
    }

    private static Optional<String> optionalString(Map<String, Object> arguments, String fieldName) {
        Object value = arguments.get(fieldName);
        if (value == null) {
            return Optional.empty();
        }
        if (!(value instanceof String text) || text.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must be a non-blank string when provided");
        }
        return Optional.of(text.trim());
    }

    private static Optional<Integer> optionalInt(Map<String, Object> arguments, String fieldName) {
        Object value = arguments.get(fieldName);
        if (value == null) {
            return Optional.empty();
        }
        if (value instanceof Integer integer) {
            return Optional.of(integer);
        }
        if (value instanceof Number number) {
            int parsed = number.intValue();
            if (Double.compare(number.doubleValue(), parsed) != 0) {
                throw new IllegalArgumentException(fieldName + " must be an integer");
            }
            return Optional.of(parsed);
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return Optional.of(Integer.parseInt(text.trim()));
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException(fieldName + " must be an integer", exception);
            }
        }
        throw new IllegalArgumentException(fieldName + " must be an integer");
    }

    private static Optional<Double> optionalDouble(Map<String, Object> arguments, String fieldName) {
        Object value = arguments.get(fieldName);
        if (value == null) {
            return Optional.empty();
        }
        if (value instanceof Number number) {
            return Optional.of(number.doubleValue());
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return Optional.of(Double.parseDouble(text.trim()));
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException(fieldName + " must be a number", exception);
            }
        }
        throw new IllegalArgumentException(fieldName + " must be a number");
    }

    private static Optional<LocalDate> optionalDate(Map<String, Object> arguments, String fieldName) {
        Optional<String> value = optionalString(arguments, fieldName);
        if (value.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(LocalDate.parse(value.orElseThrow()));
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException(fieldName + " must be an ISO-8601 date", exception);
        }
    }

    private static void putIfPresent(Map<String, Object> data, String key, Object value) {
        if (value != null) {
            data.put(key, value);
        }
    }

    private static String sanitizeInputFailure(IllegalArgumentException exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return "Invalid policy search input.";
        }
        return message
                .replaceAll("sk-[A-Za-z0-9_-]+", "sk-***")
                .replaceAll("Bearer\\s+[A-Za-z0-9._~+/=-]+", "Bearer ***")
                .replaceAll("[A-Za-z]:\\\\[^\\s,;]+", "[local-path]");
    }
}
