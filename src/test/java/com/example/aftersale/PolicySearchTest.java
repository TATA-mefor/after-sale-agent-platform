package com.example.aftersale;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.aftersale.policy.application.PolicyApplicationService;
import com.example.aftersale.policy.domain.PolicySearchResult;
import com.example.aftersale.policy.domain.PolicySnippet;
import com.example.aftersale.policy.infrastructure.repository.InMemoryPolicyRepository;
import com.example.aftersale.policy.rag.application.EmbeddingClient;
import com.example.aftersale.policy.rag.application.EmbeddingRequest;
import com.example.aftersale.policy.rag.application.EmbeddingResponse;
import com.example.aftersale.policy.rag.application.FakeEmbeddingClient;
import com.example.aftersale.policy.rag.application.RagPolicySearchApplicationService;
import com.example.aftersale.policy.rag.domain.PolicyChunk;
import com.example.aftersale.policy.rag.domain.PolicyDocument;
import com.example.aftersale.policy.rag.domain.PolicyDocumentSourceType;
import com.example.aftersale.policy.rag.domain.PolicyEmbedding;
import com.example.aftersale.policy.rag.infrastructure.memory.InMemoryPolicyVectorRepository;
import com.example.aftersale.tool.application.ToolRegistry;
import com.example.aftersale.tool.application.policy.SearchAfterSalePolicyToolExecutor;
import com.example.aftersale.tool.domain.ToolExecutionStatus;
import com.example.aftersale.tool.domain.ToolInput;
import com.example.aftersale.tool.domain.ToolOutput;
import com.example.aftersale.tool.domain.ToolRiskLevel;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class PolicySearchTest {

    private static final Instant NOW = Instant.parse("2026-05-27T00:00:00Z");

    @Autowired
    private PolicyApplicationService policyApplicationService;

    @Autowired
    private ToolRegistry toolRegistry;

    @Test
    void initializesV1PolicyData() {
        assertThat(policyApplicationService.listPolicies())
                .hasSize(6)
                .extracting("category")
                .contains(
                        "7 天无理由退货规则",
                        "质量问题退换货规则",
                        "已签收未收到物流争议规则",
                        "换货规则",
                        "维修规则",
                        "特殊商品不支持退货规则");
    }

    @Test
    void searchesReturnPolicyKeywords() {
        PolicySearchResult result = policyApplicationService.search("商品有质量问题，想退货");

        assertThat(result.hasMatches()).isTrue();
        assertThat(result.snippets())
                .extracting(PolicySnippet::policyId)
                .contains("POL-QUALITY-RETURN-EXCHANGE");
        PolicySnippet firstSnippet = result.snippets().get(0);
        assertThat(firstSnippet.category()).isEqualTo("质量问题退换货规则");
        assertThat(firstSnippet.snippetText()).contains("质量问题");
        assertThat(firstSnippet.matchReason()).contains("Matched controlled keyword set");
    }

    @Test
    void searchesExchangePolicyKeywords() {
        PolicySearchResult result = policyApplicationService.search("衣服尺码不合适，想换货");

        assertThat(result.hasMatches()).isTrue();
        assertThat(result.snippets())
                .extracting(PolicySnippet::policyId)
                .contains("POL-EXCHANGE");
    }

    @Test
    void searchesRefundPolicyKeywords() {
        PolicySearchResult result = policyApplicationService.search("商品故障，需要退款");

        assertThat(result.hasMatches()).isTrue();
        assertThat(result.snippets())
                .extracting(PolicySnippet::policyId)
                .contains("POL-QUALITY-RETURN-EXCHANGE");
    }

    @Test
    void searchesLogisticsDisputePolicy() {
        PolicySearchResult result = policyApplicationService.search("物流显示签收但我没收到货");

        assertThat(result.hasMatches()).isTrue();
        assertThat(result.snippets().get(0).policyId()).isEqualTo("POL-LOGISTICS-NOT-RECEIVED");
        assertThat(result.snippets().get(0).category()).isEqualTo("已签收未收到物流争议规则");
        assertThat(result.snippets().get(0).snippetText()).contains("未收到货");
    }

    @Test
    void searchPolicyToolExecutesThroughToolRegistry() {
        assertThat(toolRegistry.findDefinition("search_aftersale_policy"))
                .hasValueSatisfying(definition -> {
                    assertThat(definition.riskLevel()).isEqualTo(ToolRiskLevel.LOW);
                    assertThat(definition.requiresApproval()).isFalse();
                    assertThat(definition.inputSchema()).contains("query");
                });

        ToolOutput output = toolRegistry.execute("search_aftersale_policy", ToolInput.of(Map.of(
                "query", "商品有质量问题")));

        assertThat(output.status()).isEqualTo(ToolExecutionStatus.SUCCEEDED);
        assertThat(output.data()).containsKey("results");
        assertThat(output.data()).containsEntry("retrievalMode", "KEYWORD");
        assertThat(output.data()).containsKey("evidences");

        List<?> results = (List<?>) output.data().get("results");
        assertThat(results).isNotEmpty();
        Map<?, ?> firstResult = (Map<?, ?>) results.get(0);
        assertThat(firstResult.get("policyId")).isEqualTo("POL-QUALITY-RETURN-EXCHANGE");
        assertThat(firstResult.containsKey("productType")).isTrue();
        assertThat(firstResult.containsKey("matchReason")).isTrue();
        assertThat(firstResult.containsKey("matchedText")).isTrue();
    }

    @Test
    void searchPolicyToolKeepsOldInputCompatibleAndDefaultsToKeyword() {
        ToolOutput output = toolRegistry.execute("search_aftersale_policy", ToolInput.of(Map.of(
                "query", "商品有质量问题")));

        assertThat(output.status()).isEqualTo(ToolExecutionStatus.SUCCEEDED);
        assertThat(output.data()).containsEntry("retrievalMode", "KEYWORD");
        assertThat(output.data()).containsEntry("fallbackUsed", false);
    }

    @Test
    void searchPolicyToolAcceptsExplicitKeywordAndHybridModes() {
        ToolOutput keywordOutput = toolRegistry.execute("search_aftersale_policy", ToolInput.of(Map.of(
                "query", "商品有质量问题",
                "retrievalMode", "KEYWORD",
                "topK", 3)));
        ToolOutput hybridOutput = toolRegistry.execute("search_aftersale_policy", ToolInput.of(Map.of(
                "query", "商品有质量问题",
                "retrievalMode", "HYBRID",
                "topK", "3")));

        assertThat(keywordOutput.status()).isEqualTo(ToolExecutionStatus.SUCCEEDED);
        assertThat(keywordOutput.data()).containsEntry("retrievalMode", "KEYWORD");
        assertThat(hybridOutput.status()).isEqualTo(ToolExecutionStatus.SUCCEEDED);
        assertThat(hybridOutput.data()).containsEntry("retrievalMode", "HYBRID");
        assertThat(hybridOutput.data()).containsEntry("fallbackUsed", true);
    }

    @Test
    void searchPolicyToolReturnsClearFailureForUnavailableVectorMode() {
        ToolOutput output = toolRegistry.execute("search_aftersale_policy", ToolInput.of(Map.of(
                "query", "商品有质量问题",
                "retrievalMode", "VECTOR")));

        assertThat(output.status()).isEqualTo(ToolExecutionStatus.FAILED);
        assertThat(output.errorCode()).isEqualTo("VECTOR_POLICY_SEARCH_UNAVAILABLE");
        assertThat(output.message()).contains("Vector policy search is unavailable");
    }

    @Test
    void searchPolicyToolRunsVectorAndHybridModesThroughToolRegistryWithFakeRuntime() {
        ToolRegistry registry = fakeVectorToolRegistry();

        ToolOutput vectorOutput = registry.execute("search_aftersale_policy", ToolInput.of(Map.of(
                "query", "质量问题退换货",
                "retrievalMode", "VECTOR",
                "topK", 3)));
        ToolOutput hybridOutput = registry.execute("search_aftersale_policy", ToolInput.of(Map.of(
                "query", "质量问题退换货",
                "retrievalMode", "HYBRID",
                "topK", 3)));

        assertThat(vectorOutput.status()).isEqualTo(ToolExecutionStatus.SUCCEEDED);
        assertThat(vectorOutput.data()).containsEntry("retrievalMode", "VECTOR");
        assertThat(vectorOutput.data()).containsKeys("results", "evidences", "message", "fallbackUsed",
                "totalKeywordMatches", "totalVectorMatches");
        assertThat((List<?>) vectorOutput.data().get("evidences")).isNotEmpty();
        @SuppressWarnings("unchecked")
        Map<String, Object> vectorEvidence =
                (Map<String, Object>) ((List<?>) vectorOutput.data().get("evidences")).get(0);
        assertThat(vectorEvidence.get("source")).isEqualTo("VECTOR_CHUNK");
        assertThat(vectorEvidence.get("retrievalMode")).isEqualTo("VECTOR");
        assertThat(vectorEvidence.get("chunkId")).isNotNull();
        assertThat(vectorEvidence.get("documentId")).isNotNull();
        assertThat(vectorEvidence.get("score")).isNotNull();
        assertThat(vectorEvidence)
                .containsKeys("evidenceId", "documentTitle", "category", "snippet", "score", "source");
        assertThat(vectorEvidence.toString())
                .doesNotContain("apiKey", "password", "token", "D:\\", "rawText");

        assertThat(hybridOutput.status()).isEqualTo(ToolExecutionStatus.SUCCEEDED);
        assertThat(hybridOutput.data()).containsEntry("retrievalMode", "HYBRID");
        assertThat(hybridOutput.data()).containsEntry("fallbackUsed", false);
        List<?> hybridEvidences = (List<?>) hybridOutput.data().get("evidences");
        assertThat(hybridEvidences).isNotEmpty();
        assertThat(hybridEvidences)
                .allSatisfy(item -> {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> evidence = (Map<String, Object>) item;
                    assertThat(evidence.get("retrievalMode")).isEqualTo("HYBRID");
                    assertThat(evidence.get("source")).isEqualTo("MERGED_HYBRID");
                    assertThat(evidence).containsKey("score");
                });
        assertThat(hybridEvidences)
                .anySatisfy(item -> {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> evidence = (Map<String, Object>) item;
                    assertThat(evidence).containsKey("keywordScore");
                });
        assertThat(hybridOutput.data().get("totalVectorMatches")).isEqualTo(1);
    }

    @Test
    void searchPolicyToolRejectsUnknownModeAndInvalidBounds() {
        ToolOutput unknownMode = toolRegistry.execute("search_aftersale_policy", ToolInput.of(Map.of(
                "query", "商品有质量问题",
                "retrievalMode", "semantic")));
        ToolOutput invalidTopK = toolRegistry.execute("search_aftersale_policy", ToolInput.of(Map.of(
                "query", "商品有质量问题",
                "topK", 21)));
        ToolOutput invalidMinScore = toolRegistry.execute("search_aftersale_policy", ToolInput.of(Map.of(
                "query", "商品有质量问题",
                "minScore", 1.5d)));

        assertThat(unknownMode.status()).isEqualTo(ToolExecutionStatus.FAILED);
        assertThat(unknownMode.errorCode()).isEqualTo("INVALID_POLICY_SEARCH_INPUT");
        assertThat(unknownMode.message()).contains("Unknown retrievalMode");
        assertThat(invalidTopK.status()).isEqualTo(ToolExecutionStatus.FAILED);
        assertThat(invalidTopK.message()).contains("topK must be between 1 and 20");
        assertThat(invalidMinScore.status()).isEqualTo(ToolExecutionStatus.FAILED);
        assertThat(invalidMinScore.message()).contains("minScore must be between 0.0 and 1.0");
    }

    @Test
    void searchPolicyToolReturnsClearEmptyResultWhenNothingMatches() {
        ToolOutput output = toolRegistry.execute("search_aftersale_policy", ToolInput.of(Map.of(
                "query", "会员积分规则")));

        assertThat(output.status()).isEqualTo(ToolExecutionStatus.SUCCEEDED);
        assertThat(output.data()).containsEntry("message", "No after-sale policy matched the query.");
        assertThat((List<?>) output.data().get("results")).isEmpty();
    }

    @Test
    void unsupportedPolicyQueryReturnsStructuredEmptyResult() {
        PolicySearchResult result = policyApplicationService.search("会员积分和生日权益");

        assertThat(result.hasMatches()).isFalse();
        assertThat(result.snippets()).isEmpty();
        assertThat(result.message()).isEqualTo("No after-sale policy matched the query.");
    }

    private static ToolRegistry fakeVectorToolRegistry() {
        FakeEmbeddingClient embeddingClient = new FakeEmbeddingClient(4);
        InMemoryPolicyVectorRepository vectorRepository = new InMemoryPolicyVectorRepository();
        saveVectorPolicy(vectorRepository, embeddingClient);
        RagPolicySearchApplicationService ragService = new RagPolicySearchApplicationService(
                new PolicyApplicationService(new InMemoryPolicyRepository()),
                List.of(embeddingClient),
                List.of(vectorRepository));
        return new ToolRegistry(
                List.of(new SearchAfterSalePolicyToolExecutor(ragService)),
                record -> { });
    }

    private static void saveVectorPolicy(
            InMemoryPolicyVectorRepository repository,
            EmbeddingClient embeddingClient) {
        String documentId = "doc-quality";
        String chunkId = "chunk-quality";
        String content = "商品存在质量问题时，可申请退货、退款或换货。";
        repository.saveDocument(new PolicyDocument(
                documentId,
                "Quality Return Policy",
                "质量问题退换货规则",
                "通用商品",
                "v1",
                PolicyDocumentSourceType.MARKDOWN,
                "policy://doc-quality",
                "checksum-doc-quality",
                LocalDate.parse("2026-01-01"),
                null,
                NOW,
                NOW));
        repository.saveChunk(new PolicyChunk(chunkId, documentId, 0, content, 20, "{}", NOW));
        EmbeddingResponse embedding = embeddingClient.embed(new EmbeddingRequest(
                RagPolicySearchApplicationService.DEFAULT_EMBEDDING_MODEL,
                content));
        repository.saveEmbedding(new PolicyEmbedding(
                "embedding-" + chunkId,
                chunkId,
                embedding.model(),
                embedding.dimension(),
                embedding.vector(),
                NOW));
    }
}
