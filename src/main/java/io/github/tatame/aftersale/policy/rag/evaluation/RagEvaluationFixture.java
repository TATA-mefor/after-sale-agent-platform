package io.github.tatame.aftersale.policy.rag.evaluation;

import io.github.tatame.aftersale.common.observability.metrics.ApplicationMetricsRecorder;
import io.github.tatame.aftersale.policy.application.PolicyApplicationService;
import io.github.tatame.aftersale.policy.infrastructure.repository.InMemoryPolicyRepository;
import io.github.tatame.aftersale.policy.rag.application.EmbeddingClient;
import io.github.tatame.aftersale.policy.rag.application.EmbeddingRequest;
import io.github.tatame.aftersale.policy.rag.application.EmbeddingResponse;
import io.github.tatame.aftersale.policy.rag.application.FakeEmbeddingClient;
import io.github.tatame.aftersale.policy.rag.application.RagPolicySearchApplicationService;
import io.github.tatame.aftersale.policy.rag.domain.PolicyChunk;
import io.github.tatame.aftersale.policy.rag.domain.PolicyDocument;
import io.github.tatame.aftersale.policy.rag.domain.PolicyDocumentSourceType;
import io.github.tatame.aftersale.policy.rag.domain.PolicyEmbedding;
import io.github.tatame.aftersale.policy.rag.infrastructure.memory.InMemoryPolicyVectorRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

public final class RagEvaluationFixture {

    private static final Instant CREATED_AT = Instant.parse("2026-05-28T00:00:00Z");
    private static final LocalDate EFFECTIVE_FROM = LocalDate.parse("2026-01-01");

    private RagEvaluationFixture() {
    }

    public static RagPolicySearchApplicationService searchService() {
        FakeEmbeddingClient embeddingClient = new FakeEmbeddingClient(8);
        InMemoryPolicyVectorRepository vectorRepository = new InMemoryPolicyVectorRepository();
        seed(vectorRepository, embeddingClient);
        return new RagPolicySearchApplicationService(
                new PolicyApplicationService(new InMemoryPolicyRepository()),
                List.of(embeddingClient),
                List.of(vectorRepository),
                new ApplicationMetricsRecorder(new SimpleMeterRegistry()));
    }

    public static InMemoryPolicyVectorRepository vectorRepository() {
        FakeEmbeddingClient embeddingClient = new FakeEmbeddingClient(8);
        InMemoryPolicyVectorRepository vectorRepository = new InMemoryPolicyVectorRepository();
        seed(vectorRepository, embeddingClient);
        return vectorRepository;
    }

    public static List<String> fixtureTexts() {
        return fixtures().stream().map(FixtureChunk::content).toList();
    }

    private static void seed(
            InMemoryPolicyVectorRepository repository,
            EmbeddingClient embeddingClient) {
        for (FixtureChunk fixture : fixtures()) {
            repository.saveDocument(new PolicyDocument(
                    fixture.documentId(),
                    fixture.documentTitle(),
                    fixture.category(),
                    fixture.productType(),
                    "v1",
                    PolicyDocumentSourceType.MARKDOWN,
                    "policy://" + fixture.documentId(),
                    "checksum-" + fixture.documentId(),
                    EFFECTIVE_FROM,
                    null,
                    CREATED_AT,
                    CREATED_AT));
            repository.saveChunk(new PolicyChunk(
                    fixture.chunkId(),
                    fixture.documentId(),
                    0,
                    fixture.content(),
                    Math.max(1, fixture.content().length() / 4),
                    "{\"fixture\":\"rag-evaluation\"}",
                    CREATED_AT));
            EmbeddingResponse embedding = embeddingClient.embed(new EmbeddingRequest(
                    RagPolicySearchApplicationService.DEFAULT_EMBEDDING_MODEL,
                    fixture.content()));
            repository.saveEmbedding(new PolicyEmbedding(
                    "embedding-" + fixture.chunkId(),
                    fixture.chunkId(),
                    embedding.model(),
                    embedding.dimension(),
                    embedding.vector(),
                    CREATED_AT));
        }
    }

    private static List<FixtureChunk> fixtures() {
        return List.of(
                fixture(
                        "doc-return-7d",
                        "chunk-return-7d",
                        "7 天无理由退货政策",
                        "7 天无理由退货规则",
                        "通用商品",
                        "用户签收商品后 7 天内，在商品完好、附件齐全且不影响二次销售时，可申请无理由退货。"),
                fixture(
                        "doc-quality",
                        "chunk-quality",
                        "质量问题退换货政策",
                        "质量问题退换货规则",
                        "通用商品",
                        "商品存在质量问题、功能故障或与描述明显不符时，用户可申请退货、退款或换货。"),
                fixture(
                        "doc-refund-only",
                        "chunk-refund-only",
                        "仅退款未发货政策",
                        "仅退款未发货规则",
                        "通用商品",
                        "订单未发货且用户申请仅退款时，应核验订单状态，未出库可按售后流程受理退款申请。"),
                fixture(
                        "doc-logistics",
                        "chunk-logistics-not-received",
                        "签收未收到物流政策",
                        "已签收未收到物流争议规则",
                        "通用商品",
                        "物流显示签收但用户未收到货时，应核验签收凭证、物流轨迹和收货地址后进入争议处理。"),
                fixture(
                        "doc-coupon",
                        "chunk-coupon",
                        "优惠券咨询政策",
                        "优惠券咨询规则",
                        "通用商品",
                        "优惠券使用异常咨询应先说明券规则、有效期和适用范围，只能作为客服处理建议。"),
                fixture(
                        "doc-special",
                        "chunk-special-no-return",
                        "特殊商品退货限制政策",
                        "特殊商品不支持退货规则",
                        "特殊商品",
                        "定制商品、生鲜易腐商品、拆封后影响安全或卫生的商品，非质量问题通常不支持退货。"),
                fixture(
                        "doc-repair",
                        "chunk-repair",
                        "维修质量问题政策",
                        "维修规则",
                        "电子数码",
                        "保修期内商品出现非人为损坏故障时，可申请维修；超过保修期需告知可能产生费用。"));
    }

    private static FixtureChunk fixture(
            String documentId,
            String chunkId,
            String documentTitle,
            String category,
            String productType,
            String content) {
        return new FixtureChunk(documentId, chunkId, documentTitle, category, productType, content);
    }

    private record FixtureChunk(
            String documentId,
            String chunkId,
            String documentTitle,
            String category,
            String productType,
            String content) {
    }
}
