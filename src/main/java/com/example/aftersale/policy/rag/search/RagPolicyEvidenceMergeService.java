package com.example.aftersale.policy.rag.search;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class RagPolicyEvidenceMergeService {

    private static final String DEFAULT_EMPTY_QUERY = "empty hybrid policy search";

    public RagPolicySearchResult merge(
            RagPolicySearchResult keywordResult,
            RagPolicySearchResult vectorResult,
            RagPolicyEvidenceMergeOptions options) {
        RagPolicyEvidenceMergeOptions mergeOptions =
                options == null ? RagPolicyEvidenceMergeOptions.defaults() : options;
        InputView keyword = InputView.from(keywordResult, RetrievalMode.KEYWORD);
        InputView vector = InputView.from(vectorResult, RetrievalMode.VECTOR);

        Map<String, MergeCandidate> candidates = new LinkedHashMap<>();
        keyword.evidences().forEach(evidence -> addEvidence(candidates, evidence, mergeOptions, RetrievalMode.KEYWORD));
        vector.evidences().forEach(evidence -> addEvidence(candidates, evidence, mergeOptions, RetrievalMode.VECTOR));

        List<RagPolicyEvidence> merged = candidates.values().stream()
                .filter(candidate -> candidate.shouldInclude(mergeOptions))
                .map(candidate -> candidate.toMergedEvidence(mergeOptions))
                .filter(evidence -> evidence.score() >= mergeOptions.minScore())
                .sorted(resultComparator(mergeOptions))
                .limit(mergeOptions.topK())
                .toList();

        boolean fallbackUsed = keyword.empty() || vector.empty() || keyword.fallbackUsed() || vector.fallbackUsed();
        return new RagPolicySearchResult(
                chooseQuery(keyword, vector),
                RetrievalMode.HYBRID,
                merged,
                mergeMessage(keyword, vector),
                fallbackUsed,
                keyword.totalMatches(),
                vector.totalMatches());
    }

    private static void addEvidence(
            Map<String, MergeCandidate> candidates,
            RagPolicyEvidence evidence,
            RagPolicyEvidenceMergeOptions options,
            RetrievalMode side) {
        String key = dedupKey(candidates, evidence, options);
        candidates.compute(key, (ignored, existing) -> {
            if (existing == null) {
                return MergeCandidate.from(evidence);
            }
            return existing.merge(evidence);
        });
    }

    private static String dedupKey(
            Map<String, MergeCandidate> candidates,
            RagPolicyEvidence evidence,
            RagPolicyEvidenceMergeOptions options) {
        List<String> keys = candidateKeys(evidence, options);
        for (String key : keys) {
            if (candidates.containsKey(key)) {
                return key;
            }
        }
        for (Map.Entry<String, MergeCandidate> entry : candidates.entrySet()) {
            if (entry.getValue().isDuplicateOf(evidence, options)) {
                return entry.getKey();
            }
        }
        return keys.stream().findFirst().orElse("evidence:" + evidence.evidenceId());
    }

    private static List<String> candidateKeys(RagPolicyEvidence evidence, RagPolicyEvidenceMergeOptions options) {
        List<String> keys = new ArrayList<>();
        if (options.dedupByChunkId() && evidence.chunkId() != null) {
            keys.add("chunk:" + evidence.chunkId());
        }
        if (options.dedupByPolicyId() && evidence.policyId() != null) {
            keys.add("policy:" + evidence.policyId());
        }
        if (options.dedupBySnippet()) {
            keys.add("snippet:" + normalizeSnippet(evidence.snippet()));
        }
        keys.add("evidence:" + evidence.evidenceId());
        return keys;
    }

    private static String chooseQuery(InputView keyword, InputView vector) {
        if (!keyword.empty()) {
            return keyword.query();
        }
        if (!vector.empty()) {
            return vector.query();
        }
        if (keyword.query() != null) {
            return keyword.query();
        }
        if (vector.query() != null) {
            return vector.query();
        }
        return DEFAULT_EMPTY_QUERY;
    }

    private static String mergeMessage(InputView keyword, InputView vector) {
        if (keyword.empty() && vector.empty()) {
            return "No keyword or vector policy evidence available for hybrid merge.";
        }
        if (keyword.empty()) {
            return "Hybrid merge used vector evidence only because keyword side was empty.";
        }
        if (vector.empty()) {
            return "Hybrid merge used keyword evidence only because vector side was empty.";
        }
        return "Hybrid policy evidence merged from keyword and vector results.";
    }

    private static Comparator<RagPolicyEvidence> resultComparator(RagPolicyEvidenceMergeOptions options) {
        Comparator<RagPolicyEvidence> comparator = Comparator.comparingDouble(RagPolicyEvidence::score).reversed();
        if (options.preferKeywordWhenTie()) {
            comparator = comparator.thenComparing(RagPolicyEvidenceMergeService::keywordPresenceSortKey);
        }
        return comparator.thenComparing(RagPolicyEvidence::evidenceId);
    }

    private static int keywordPresenceSortKey(RagPolicyEvidence evidence) {
        return evidence.keywordScore() == null ? 1 : 0;
    }

    private static String normalizeSnippet(String value) {
        return value.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private record InputView(
            String query,
            List<RagPolicyEvidence> evidences,
            boolean fallbackUsed,
            int totalMatches) {

        private static InputView from(RagPolicySearchResult result, RetrievalMode expectedMode) {
            if (result == null) {
                return new InputView(null, List.of(), true, 0);
            }
            if (result.retrievalMode() != expectedMode) {
                throw new IllegalArgumentException("Expected " + expectedMode + " result for hybrid merge");
            }
            int matches = expectedMode == RetrievalMode.KEYWORD
                    ? result.totalKeywordMatches()
                    : result.totalVectorMatches();
            return new InputView(result.query(), result.evidences(), result.fallbackUsed(), matches);
        }

        private boolean empty() {
            return evidences.isEmpty();
        }
    }

    private record MergeCandidate(
            String evidenceId,
            String documentId,
            String chunkId,
            String policyId,
            String documentTitle,
            String category,
            String productType,
            String snippet,
            Double keywordScore,
            Double vectorScore,
            java.time.LocalDate effectiveFrom,
            java.time.LocalDate effectiveTo,
            String metadataJson) {

        private static MergeCandidate from(RagPolicyEvidence evidence) {
            return new MergeCandidate(
                    evidence.evidenceId(),
                    evidence.documentId(),
                    evidence.chunkId(),
                    evidence.policyId(),
                    evidence.documentTitle(),
                    evidence.category(),
                    evidence.productType(),
                    evidence.snippet(),
                    effectiveKeywordScore(evidence),
                    effectiveVectorScore(evidence),
                    evidence.effectiveFrom(),
                    evidence.effectiveTo(),
                    evidence.metadataJson());
        }

        private MergeCandidate merge(RagPolicyEvidence evidence) {
            return new MergeCandidate(
                    evidenceId,
                    firstPresent(documentId, evidence.documentId()),
                    firstPresent(chunkId, evidence.chunkId()),
                    firstPresent(policyId, evidence.policyId()),
                    firstPresent(documentTitle, evidence.documentTitle()),
                    category,
                    productType,
                    preferLongerSnippet(snippet, evidence.snippet()),
                    maxScore(keywordScore, effectiveKeywordScore(evidence)),
                    maxScore(vectorScore, effectiveVectorScore(evidence)),
                    firstDate(effectiveFrom, evidence.effectiveFrom()),
                    firstDate(effectiveTo, evidence.effectiveTo()),
                    mergeMetadata(metadataJson, evidence.metadataJson()));
        }

        private RagPolicyEvidence toMergedEvidence(RagPolicyEvidenceMergeOptions options) {
            double score = mergedScore(options);
            return new RagPolicyEvidence(
                    "hybrid:" + evidenceId,
                    documentId,
                    chunkId,
                    policyId,
                    documentTitle,
                    category,
                    productType,
                    snippet,
                    score,
                    keywordScore,
                    vectorScore,
                    RetrievalMode.HYBRID,
                    RagPolicyEvidenceSource.MERGED_HYBRID,
                    effectiveFrom,
                    effectiveTo,
                    metadataJson);
        }

        private boolean shouldInclude(RagPolicyEvidenceMergeOptions options) {
            if (keywordScore != null && vectorScore != null) {
                return true;
            }
            if (keywordScore != null) {
                return options.includeKeywordOnly();
            }
            return vectorScore != null && options.includeVectorOnly();
        }

        private boolean isDuplicateOf(RagPolicyEvidence evidence, RagPolicyEvidenceMergeOptions options) {
            if (options.dedupByChunkId() && chunkId != null && chunkId.equals(evidence.chunkId())) {
                return true;
            }
            if (options.dedupByPolicyId() && policyId != null && policyId.equals(evidence.policyId())) {
                return true;
            }
            return options.dedupBySnippet() && normalizeSnippet(snippet).equals(normalizeSnippet(evidence.snippet()));
        }

        private double mergedScore(RagPolicyEvidenceMergeOptions options) {
            double weighted = 0.0d;
            double divisor = 0.0d;
            if (keywordScore != null) {
                weighted += keywordScore * options.keywordWeight();
                divisor += options.keywordWeight();
            }
            if (vectorScore != null) {
                weighted += vectorScore * options.vectorWeight();
                divisor += options.vectorWeight();
            }
            if (divisor == 0.0d) {
                return 0.0d;
            }
            return clamp(weighted / divisor);
        }

        private static Double effectiveKeywordScore(RagPolicyEvidence evidence) {
            if (evidence.keywordScore() != null) {
                return evidence.keywordScore();
            }
            return evidence.retrievalMode() == RetrievalMode.KEYWORD ? evidence.score() : null;
        }

        private static Double effectiveVectorScore(RagPolicyEvidence evidence) {
            if (evidence.vectorScore() != null) {
                return evidence.vectorScore();
            }
            return evidence.retrievalMode() == RetrievalMode.VECTOR ? evidence.score() : null;
        }

        private static String firstPresent(String current, String incoming) {
            return current != null ? current : incoming;
        }

        private static java.time.LocalDate firstDate(java.time.LocalDate current, java.time.LocalDate incoming) {
            return current != null ? current : incoming;
        }

        private static String preferLongerSnippet(String current, String incoming) {
            return incoming.length() > current.length() ? incoming : current;
        }

        private static Double maxScore(Double current, Double incoming) {
            if (current == null) {
                return incoming;
            }
            if (incoming == null) {
                return current;
            }
            return Math.max(current, incoming);
        }

        private static String mergeMetadata(String current, String incoming) {
            String normalizedCurrent = Optional.ofNullable(current).orElse("{}");
            String normalizedIncoming = Optional.ofNullable(incoming).orElse("{}");
            if ("{}".equals(normalizedCurrent)) {
                return normalizedIncoming;
            }
            return normalizedCurrent;
        }

        private static double clamp(double value) {
            double clamped = Math.max(0.0d, Math.min(1.0d, value));
            return Math.round(clamped * 1_000_000.0d) / 1_000_000.0d;
        }
    }
}
