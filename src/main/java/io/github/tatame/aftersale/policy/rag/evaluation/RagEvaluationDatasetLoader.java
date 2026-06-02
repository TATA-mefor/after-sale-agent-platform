package io.github.tatame.aftersale.policy.rag.evaluation;

import io.github.tatame.aftersale.policy.rag.search.RagPolicyEvidenceSource;
import io.github.tatame.aftersale.policy.rag.search.RetrievalMode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class RagEvaluationDatasetLoader {

    private final ObjectMapper objectMapper;

    public RagEvaluationDatasetLoader(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }

    public List<RagEvaluationCase> load(Path datasetPath) {
        Objects.requireNonNull(datasetPath, "datasetPath must not be null");
        try {
            List<String> lines = Files.readAllLines(datasetPath);
            List<RagEvaluationCase> cases = new ArrayList<>();
            for (int index = 0; index < lines.size(); index++) {
                String line = lines.get(index);
                if (!line.isBlank()) {
                    cases.add(parseCase(line, index + 1));
                }
            }
            return List.copyOf(cases);
        } catch (IOException exception) {
            throw new IllegalArgumentException("Failed to read RAG evaluation dataset: " + datasetPath, exception);
        }
    }

    private RagEvaluationCase parseCase(String line, int lineNumber) {
        try {
            JsonNode node = objectMapper.readTree(line);
            JsonNode expected = requiredObject(node, "expected");
            return new RagEvaluationCase(
                    requiredText(node, "caseId"),
                    requiredText(node, "query"),
                    enumValue(node, "retrievalMode", RetrievalMode.class),
                    optionalInt(node, "topK", 5),
                    optionalDouble(node, "minScore"),
                    optionalText(node, "category"),
                    optionalText(node, "productType"),
                    expected(expected));
        } catch (RuntimeException | IOException exception) {
            throw new IllegalArgumentException(
                    "Invalid RAG evaluation case at line " + lineNumber + ": " + exception.getMessage(),
                    exception);
        }
    }

    private static RagEvaluationExpected expected(JsonNode expected) {
        return new RagEvaluationExpected(
                enumValue(expected, "requiredRetrievalMode", RetrievalMode.class),
                enumList(expected, "requiredEvidenceSources", RagPolicyEvidenceSource.class),
                stringList(expected, "requiredCategories"),
                stringList(expected, "requiredProductTypes"),
                stringList(expected, "requiredAnySnippetContains"),
                stringList(expected, "forbiddenSnippetContains"),
                requiredInt(expected, "minEvidenceCount"),
                requiredInt(expected, "maxEvidenceCount"),
                requiredBoolean(expected, "expectFallbackUsed"),
                requiredBoolean(expected, "expectEmptyResult"),
                requiredBoolean(expected, "requireCitation"),
                optionalStringList(expected, "requiredDocumentIds"),
                optionalStringList(expected, "requiredChunkIds"));
    }

    private static JsonNode requiredObject(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        if (!value.isObject()) {
            throw new IllegalArgumentException(fieldName + " must be an object");
        }
        return value;
    }

    private static String requiredText(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        if (value.isMissingNode() || value.isNull() || !value.isTextual() || value.asText().isBlank()) {
            throw new IllegalArgumentException(fieldName + " must be a non-blank string");
        }
        return value.asText();
    }

    private static String optionalText(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        if (!value.isTextual()) {
            throw new IllegalArgumentException(fieldName + " must be a string");
        }
        return value.asText();
    }

    private static int requiredInt(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        if (!value.canConvertToInt()) {
            throw new IllegalArgumentException(fieldName + " must be an integer");
        }
        return value.asInt();
    }

    private static int optionalInt(JsonNode node, String fieldName, int defaultValue) {
        JsonNode value = node.path(fieldName);
        if (value.isMissingNode() || value.isNull()) {
            return defaultValue;
        }
        if (!value.canConvertToInt()) {
            throw new IllegalArgumentException(fieldName + " must be an integer");
        }
        return value.asInt();
    }

    private static Double optionalDouble(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        if (!value.isNumber()) {
            throw new IllegalArgumentException(fieldName + " must be a number");
        }
        return value.asDouble();
    }

    private static boolean requiredBoolean(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        if (!value.isBoolean()) {
            throw new IllegalArgumentException(fieldName + " must be a boolean");
        }
        return value.asBoolean();
    }

    private static List<String> optionalStringList(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        if (value.isMissingNode() || value.isNull()) {
            return List.of();
        }
        return stringList(node, fieldName);
    }

    private static List<String> stringList(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        if (!value.isArray()) {
            throw new IllegalArgumentException(fieldName + " must be an array");
        }
        List<String> values = new ArrayList<>();
        for (JsonNode item : value) {
            if (!item.isTextual()) {
                throw new IllegalArgumentException(fieldName + " must contain only strings");
            }
            values.add(item.asText());
        }
        return List.copyOf(values);
    }

    private static <T extends Enum<T>> T enumValue(JsonNode node, String fieldName, Class<T> enumType) {
        String value = requiredText(node, fieldName);
        try {
            return Enum.valueOf(enumType, value);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(fieldName + " has unsupported value", exception);
        }
    }

    private static <T extends Enum<T>> List<T> enumList(JsonNode node, String fieldName, Class<T> enumType) {
        return stringList(node, fieldName).stream()
                .map(value -> Enum.valueOf(enumType, value))
                .toList();
    }
}
