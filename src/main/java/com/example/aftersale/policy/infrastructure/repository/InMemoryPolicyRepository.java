package com.example.aftersale.policy.infrastructure.repository;

import com.example.aftersale.policy.domain.AfterSalePolicy;
import com.example.aftersale.policy.domain.PolicyRepository;
import com.example.aftersale.policy.domain.PolicySearchQuery;
import com.example.aftersale.policy.domain.PolicySearchResult;
import com.example.aftersale.policy.domain.PolicySnippet;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryPolicyRepository implements PolicyRepository {

    private static final Instant EFFECTIVE_FROM = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant EFFECTIVE_TO = Instant.parse("2026-12-31T23:59:59Z");

    private final List<AfterSalePolicy> policies = List.of(
            policy(
                    "POL-RETURN-7D",
                    "7 天无理由退货规则",
                    "通用商品",
                    "用户签收商品后 7 天内，在商品完好、附件齐全且不影响二次销售时，可申请无理由退货。"),
            policy(
                    "POL-QUALITY-RETURN-EXCHANGE",
                    "质量问题退换货规则",
                    "通用商品",
                    "商品存在质量问题、功能故障或与描述明显不符时，用户可申请退货、退款或换货。"),
            policy(
                    "POL-LOGISTICS-NOT-RECEIVED",
                    "已签收未收到物流争议规则",
                    "通用商品",
                    "物流显示已签收但用户反馈未收到货时，应核验签收凭证、物流轨迹和收货地址后进入争议处理。"),
            policy(
                    "POL-EXCHANGE",
                    "换货规则",
                    "服饰鞋包",
                    "尺码不合适、颜色错发或同款可替换库存充足时，可发起换货流程。"),
            policy(
                    "POL-REPAIR",
                    "维修规则",
                    "电子数码",
                    "保修期内商品出现非人为损坏故障时，可申请维修；超过保修期需告知可能产生费用。"),
            policy(
                    "POL-SPECIAL-NO-RETURN",
                    "特殊商品不支持退货规则",
                    "特殊商品",
                    "定制商品、生鲜易腐商品、拆封后影响安全或卫生的商品，非质量问题通常不支持退货。"));

    @Override
    public List<AfterSalePolicy> findAll() {
        return policies;
    }

    @Override
    public PolicySearchResult search(PolicySearchQuery query) {
        List<PolicySnippet> snippets = policies.stream()
                .map(policy -> new ScoredPolicy(policy, score(policy, query.queryText())))
                .filter(scoredPolicy -> scoredPolicy.score() > 0)
                .sorted(Comparator.comparingInt(ScoredPolicy::score).reversed()
                        .thenComparing(scoredPolicy -> scoredPolicy.policy().getPolicyId()))
                .limit(query.limit())
                .map(scoredPolicy -> PolicySnippet.from(
                        scoredPolicy.policy(),
                        "Matched controlled keyword set against category, product type, and policy text."))
                .toList();
        if (snippets.isEmpty()) {
            return PolicySearchResult.empty(query);
        }
        return PolicySearchResult.matched(query, snippets);
    }

    private static AfterSalePolicy policy(String policyId, String category, String productType, String policyText) {
        return new AfterSalePolicy(policyId, category, productType, policyText, EFFECTIVE_FROM, EFFECTIVE_TO);
    }

    private static int score(AfterSalePolicy policy, String normalizedQuery) {
        String searchableText = normalize(policy.getCategory()
                + " " + policy.getProductType()
                + " " + policy.getPolicyText());
        int score = 0;
        for (String keyword : keywordsFor(normalizedQuery)) {
            if (searchableText.contains(keyword)) {
                score++;
            }
        }
        return score;
    }

    private static List<String> keywordsFor(String normalizedQuery) {
        if (normalizedQuery.contains("质量") || normalizedQuery.contains("坏")
                || normalizedQuery.contains("故障") || normalizedQuery.contains("问题")) {
            return List.of("质量", "故障", "退换货", "退款");
        }
        if (normalizedQuery.contains("退款") || normalizedQuery.contains("仅退款")
                || normalizedQuery.contains("退钱")) {
            return List.of("退款", "退货", "质量");
        }
        if (normalizedQuery.contains("没收到") || normalizedQuery.contains("未收到")
                || normalizedQuery.contains("签收") || normalizedQuery.contains("物流")) {
            return List.of("未收到", "签收", "物流");
        }
        if (normalizedQuery.contains("七天") || normalizedQuery.contains("7天")
                || normalizedQuery.contains("无理由") || normalizedQuery.contains("退货")) {
            return List.of("7天", "无理由", "退货");
        }
        if (normalizedQuery.contains("换货") || normalizedQuery.contains("换")
                || normalizedQuery.contains("尺码")) {
            return List.of("换货", "尺码");
        }
        if (normalizedQuery.contains("维修") || normalizedQuery.contains("修")) {
            return List.of("维修", "修理");
        }
        if (normalizedQuery.contains("优惠券") || normalizedQuery.contains("券")) {
            return List.of("优惠券");
        }
        if (normalizedQuery.contains("特殊") || normalizedQuery.contains("不支持")
                || normalizedQuery.contains("定制") || normalizedQuery.contains("生鲜")) {
            return List.of("特殊", "不支持", "定制", "生鲜");
        }
        return List.of(normalizedQuery);
    }

    private static String normalize(String value) {
        return value.toLowerCase(Locale.ROOT).trim();
    }

    private record ScoredPolicy(AfterSalePolicy policy, int score) {
    }
}
