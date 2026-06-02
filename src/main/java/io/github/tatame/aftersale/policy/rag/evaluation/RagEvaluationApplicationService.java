package io.github.tatame.aftersale.policy.rag.evaluation;

import io.github.tatame.aftersale.policy.rag.search.RagPolicyEvidence;
import io.github.tatame.aftersale.policy.rag.search.RagPolicyEvidenceSource;
import io.github.tatame.aftersale.policy.rag.search.RagPolicySearchQuery;
import io.github.tatame.aftersale.policy.rag.search.RagPolicySearchResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class RagEvaluationApplicationService {

    private static final Path DEFAULT_DATASET_PATH = Path.of("docs/evaluation/rag_policy_cases.jsonl");

    private final RagEvaluationDatasetLoader datasetLoader;

    public RagEvaluationApplicationService(ObjectMapper objectMapper) {
        this.datasetLoader = new RagEvaluationDatasetLoader(objectMapper);
    }

    public List<RagEvaluationCase> loadCases(Path datasetPath) {
        return datasetLoader.load(datasetPath);
    }

    public RagEvaluationReport runDefault() {
        return run(DEFAULT_DATASET_PATH);
    }

    public RagEvaluationReport run(Path datasetPath) {
        List<RagEvaluationCase> cases = datasetLoader.load(datasetPath);
        return evaluate(cases);
    }

    public RagEvaluationReport evaluate(List<RagEvaluationCase> cases) {
        List<RagEvaluationCase> evaluationCases = List.copyOf(Objects.requireNonNull(cases, "cases must not be null"));
        List<RagEvaluationResult> results = evaluationCases.stream()
                .map(this::evaluateCase)
                .toList();
        return report(results);
    }

    private RagEvaluationResult evaluateCase(RagEvaluationCase evaluationCase) {
        RagPolicySearchResult searchResult = RagEvaluationFixture.searchService().search(toQuery(evaluationCase));
        List<RagEvaluationFailure> failures = new ArrayList<>();
        RagEvaluationExpected expected = evaluationCase.expected();
        compare(evaluationCase, "retrievalMode", expected.requiredRetrievalMode(), searchResult.retrievalMode(),
                failures);
        compare(evaluationCase, "fallbackUsed", expected.expectFallbackUsed(), searchResult.fallbackUsed(), failures);
        compare(evaluationCase, "emptyResult", expected.expectEmptyResult(), searchResult.evidences().isEmpty(),
                failures);
        compareEvidenceCount(evaluationCase, searchResult, failures);
        compareSources(evaluationCase, searchResult, failures);
        compareCategories(evaluationCase, searchResult, failures);
        compareProductTypes(evaluationCase, searchResult, failures);
        compareSnippetKeywords(evaluationCase, searchResult, failures);
        compareDocumentIds(evaluationCase, searchResult, failures);
        compareChunkIds(evaluationCase, searchResult, failures);
        boolean citationComplete = citationComplete(searchResult, expected.requireCitation());
        if (!citationComplete) {
            failures.add(failure(evaluationCase, "citationCompleteness", "traceable citation", "missing",
                    "Evidence must contain policyId or documentId/chunkId."));
        }
        boolean safetyPassed = safetyPassed(searchResult, expected.forbiddenSnippetContains());
        if (!safetyPassed) {
            failures.add(failure(evaluationCase, "safety", "no forbidden or sensitive text", "unsafe text",
                    "Evidence or message contained forbidden completed-action or sensitive text."));
        }

        return new RagEvaluationResult(
                evaluationCase.caseId(),
                failures.isEmpty(),
                searchResult.retrievalMode(),
                searchResult.evidences().size(),
                searchResult.fallbackUsed(),
                searchResult.evidences().isEmpty(),
                citationComplete,
                safetyPassed,
                evidenceSources(searchResult),
                categories(searchResult),
                documentIds(searchResult),
                chunkIds(searchResult),
                failures);
    }

    private static RagPolicySearchQuery toQuery(RagEvaluationCase evaluationCase) {
        return new RagPolicySearchQuery(
                evaluationCase.query(),
                evaluationCase.retrievalMode(),
                evaluationCase.topK(),
                evaluationCase.minScore(),
                evaluationCase.category(),
                evaluationCase.productType(),
                null,
                null,
                evaluationCase.retrievalMode().name().equals("KEYWORD"),
                !evaluationCase.retrievalMode().name().equals("KEYWORD"));
    }

    private static RagEvaluationReport report(List<RagEvaluationResult> results) {
        int totalCases = results.size();
        int passedCases = (int) results.stream().filter(RagEvaluationResult::passed).count();
        int failedCases = totalCases - passedCases;
        List<RagEvaluationFailure> failures = results.stream()
                .flatMap(result -> result.failures().stream())
                .toList();
        RagEvaluationMetric passRate = RagEvaluationMetric.of("passRate", passedCases, totalCases);
        RagEvaluationMetric evidenceRecall = metric(results, "evidenceRecallPassRate",
                List.of("evidenceCount", "categories", "productTypes", "snippetKeywords"));
        RagEvaluationMetric evidenceSource = metric(results, "evidenceSourcePassRate", List.of("evidenceSources"));
        RagEvaluationMetric retrievalMode = metric(results, "retrievalModePassRate", List.of("retrievalMode"));
        RagEvaluationMetric fallback = metric(results, "fallbackAccuracy", List.of("fallbackUsed"));
        RagEvaluationMetric emptyResult = metric(results, "emptyResultAccuracy", List.of("emptyResult"));
        RagEvaluationMetric citation = metric(results, "citationCompletenessRate", List.of("citationCompleteness"));
        RagEvaluationMetric safety = metric(results, "safetyPassRate", List.of("safety"));
        List<RagEvaluationMetric> metrics = List.of(
                passRate,
                evidenceRecall,
                evidenceSource,
                retrievalMode,
                fallback,
                emptyResult,
                citation,
                safety);
        return new RagEvaluationReport(
                totalCases,
                passedCases,
                failedCases,
                passRate.value(),
                evidenceRecall.value(),
                evidenceSource.value(),
                retrievalMode.value(),
                fallback.value(),
                emptyResult.value(),
                citation.value(),
                safety.value(),
                averageEvidenceCount(results),
                metrics,
                results,
                failures);
    }

    private static RagEvaluationMetric metric(
            List<RagEvaluationResult> results,
            String metricName,
            List<String> fieldNames) {
        int passed = (int) results.stream()
                .filter(result -> fieldNames.stream().allMatch(result::passedField))
                .count();
        return RagEvaluationMetric.of(metricName, passed, results.size());
    }

    private static double averageEvidenceCount(List<RagEvaluationResult> results) {
        if (results.isEmpty()) {
            return 0.0d;
        }
        double average = results.stream().mapToInt(RagEvaluationResult::evidenceCount).average().orElse(0.0d);
        return Math.round(average * 1_000_000.0d) / 1_000_000.0d;
    }

    private static void compareEvidenceCount(
            RagEvaluationCase evaluationCase,
            RagPolicySearchResult result,
            List<RagEvaluationFailure> failures) {
        int count = result.evidences().size();
        RagEvaluationExpected expected = evaluationCase.expected();
        if (count < expected.minEvidenceCount() || count > expected.maxEvidenceCount()) {
            failures.add(failure(evaluationCase, "evidenceCount",
                    expected.minEvidenceCount() + ".." + expected.maxEvidenceCount(),
                    count,
                    "Evidence count was outside expected bounds."));
        }
    }

    private static void compareSources(
            RagEvaluationCase evaluationCase,
            RagPolicySearchResult result,
            List<RagEvaluationFailure> failures) {
        List<RagPolicyEvidenceSource> actual = evidenceSources(result);
        List<RagPolicyEvidenceSource> expected = evaluationCase.expected().requiredEvidenceSources();
        if (!actual.containsAll(expected)) {
            failures.add(failure(evaluationCase, "evidenceSources", expected, actual,
                    "Required evidence sources were not present."));
        }
    }

    private static void compareCategories(
            RagEvaluationCase evaluationCase,
            RagPolicySearchResult result,
            List<RagEvaluationFailure> failures) {
        List<String> actual = categories(result);
        List<String> expected = evaluationCase.expected().requiredCategories();
        if (!actual.containsAll(expected)) {
            failures.add(failure(evaluationCase, "categories", expected, actual,
                    "Required categories were not present."));
        }
    }

    private static void compareProductTypes(
            RagEvaluationCase evaluationCase,
            RagPolicySearchResult result,
            List<RagEvaluationFailure> failures) {
        List<String> actual = productTypes(result);
        List<String> expected = evaluationCase.expected().requiredProductTypes();
        if (!actual.containsAll(expected)) {
            failures.add(failure(evaluationCase, "productTypes", expected, actual,
                    "Required product types were not present."));
        }
    }

    private static void compareSnippetKeywords(
            RagEvaluationCase evaluationCase,
            RagPolicySearchResult result,
            List<RagEvaluationFailure> failures) {
        List<String> expected = evaluationCase.expected().requiredAnySnippetContains();
        if (expected.isEmpty()) {
            return;
        }
        boolean matched = result.evidences().stream()
                .map(RagPolicyEvidence::snippet)
                .anyMatch(snippet -> expected.stream().allMatch(snippet::contains));
        if (!matched) {
            failures.add(failure(evaluationCase, "snippetKeywords", expected, snippetPreview(result),
                    "No evidence snippet contained all required keywords."));
        }
    }

    private static void compareDocumentIds(
            RagEvaluationCase evaluationCase,
            RagPolicySearchResult result,
            List<RagEvaluationFailure> failures) {
        List<String> actual = documentIds(result);
        List<String> expected = evaluationCase.expected().requiredDocumentIds();
        if (!actual.containsAll(expected)) {
            failures.add(failure(evaluationCase, "documentIds", expected, actual,
                    "Required document IDs were not present."));
        }
    }

    private static void compareChunkIds(
            RagEvaluationCase evaluationCase,
            RagPolicySearchResult result,
            List<RagEvaluationFailure> failures) {
        List<String> actual = chunkIds(result);
        List<String> expected = evaluationCase.expected().requiredChunkIds();
        if (!actual.containsAll(expected)) {
            failures.add(failure(evaluationCase, "chunkIds", expected, actual,
                    "Required chunk IDs were not present."));
        }
    }

    private static void compare(
            RagEvaluationCase evaluationCase,
            String fieldName,
            Object expected,
            Object actual,
            List<RagEvaluationFailure> failures) {
        if (!Objects.equals(expected, actual)) {
            failures.add(failure(evaluationCase, fieldName, expected, actual, "Expected value did not match."));
        }
    }

    private static boolean citationComplete(RagPolicySearchResult result, boolean requireCitation) {
        if (!requireCitation) {
            return true;
        }
        return result.evidences().stream().allMatch(evidence -> evidence.policyId() != null
                || evidence.documentId() != null
                || evidence.chunkId() != null);
    }

    private static boolean safetyPassed(RagPolicySearchResult result, List<String> forbiddenValues) {
        List<String> forbidden = new ArrayList<>(forbiddenValues);
        forbidden.addAll(List.of("api_key", "apikey", "password", "token", "OPENAI_API_KEY", "rawText", "D:\\"));
        String message = result.message();
        if (containsAny(message, forbidden)) {
            return false;
        }
        return result.evidences().stream()
                .noneMatch(evidence -> containsAny(evidence.snippet(), forbidden)
                        || containsAny(evidence.metadataJson(), forbidden));
    }

    private static boolean containsAny(String value, List<String> forbiddenValues) {
        if (value == null) {
            return false;
        }
        return forbiddenValues.stream().anyMatch(value::contains);
    }

    private static List<RagPolicyEvidenceSource> evidenceSources(RagPolicySearchResult result) {
        Set<RagPolicyEvidenceSource> sources = new LinkedHashSet<>();
        result.evidences().stream().map(RagPolicyEvidence::source).forEach(sources::add);
        return List.copyOf(sources);
    }

    private static List<String> categories(RagPolicySearchResult result) {
        Set<String> values = new LinkedHashSet<>();
        result.evidences().stream().map(RagPolicyEvidence::category).forEach(values::add);
        return List.copyOf(values);
    }

    private static List<String> productTypes(RagPolicySearchResult result) {
        Set<String> values = new LinkedHashSet<>();
        result.evidences().stream().map(RagPolicyEvidence::productType).forEach(values::add);
        return List.copyOf(values);
    }

    private static List<String> documentIds(RagPolicySearchResult result) {
        Set<String> values = new LinkedHashSet<>();
        result.evidences().stream()
                .map(RagPolicyEvidence::documentId)
                .filter(Objects::nonNull)
                .forEach(values::add);
        return List.copyOf(values);
    }

    private static List<String> chunkIds(RagPolicySearchResult result) {
        Set<String> values = new LinkedHashSet<>();
        result.evidences().stream()
                .map(RagPolicyEvidence::chunkId)
                .filter(Objects::nonNull)
                .forEach(values::add);
        return List.copyOf(values);
    }

    private static List<String> snippetPreview(RagPolicySearchResult result) {
        return result.evidences().stream()
                .map(RagPolicyEvidence::snippet)
                .map(value -> value.length() > 80 ? value.substring(0, 80) : value)
                .toList();
    }

    private static RagEvaluationFailure failure(
            RagEvaluationCase evaluationCase,
            String fieldName,
            Object expected,
            Object actual,
            String message) {
        return new RagEvaluationFailure(
                evaluationCase.caseId(),
                fieldName,
                String.valueOf(expected),
                String.valueOf(actual),
                message);
    }
}
