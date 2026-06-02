package io.github.tatame.aftersale.policy.rag.evaluation;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.tatame.aftersale.policy.rag.search.RagPolicyEvidenceSource;
import io.github.tatame.aftersale.policy.rag.search.RagPolicySearchQuery;
import io.github.tatame.aftersale.policy.rag.search.RagPolicySearchResult;
import io.github.tatame.aftersale.policy.rag.search.RetrievalMode;
import java.util.List;
import org.junit.jupiter.api.Test;

class RagEvaluationFixtureTest {

    @Test
    void fixtureTextsAreDeterministicAndSafe() {
        List<String> first = RagEvaluationFixture.fixtureTexts();
        List<String> second = RagEvaluationFixture.fixtureTexts();

        assertThat(first).isEqualTo(second);
        assertThat(first).hasSize(7);
        assertThat(first).allSatisfy(text -> assertThat(text)
                .doesNotContain("已退款")
                .doesNotContain("已换货")
                .doesNotContain("已补偿")
                .doesNotContain("已关闭争议"));
    }

    @Test
    void fakeVectorEvidenceCanBeFoundWithoutExternalProvider() {
        RagPolicySearchResult result = RagEvaluationFixture.searchService().search(new RagPolicySearchQuery(
                "物流显示签收但用户未收到货时，应核验签收凭证、物流轨迹和收货地址后进入争议处理。",
                RetrievalMode.VECTOR,
                5,
                0.0d,
                "已签收未收到物流争议规则",
                "通用商品",
                null,
                null,
                false,
                true));

        assertThat(result.retrievalMode()).isEqualTo(RetrievalMode.VECTOR);
        assertThat(result.evidences()).isNotEmpty();
        assertThat(result.evidences().get(0).source()).isEqualTo(RagPolicyEvidenceSource.VECTOR_CHUNK);
        assertThat(result.evidences().get(0).chunkId()).isEqualTo("chunk-logistics-not-received");
    }
}
