package com.example.aftersale.policy.application;

import com.example.aftersale.policy.domain.AfterSalePolicy;
import com.example.aftersale.policy.domain.PolicyRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class PolicyApplicationService {

    private final PolicyRepository policyRepository;

    public PolicyApplicationService(PolicyRepository policyRepository) {
        this.policyRepository = policyRepository;
    }

    public List<AfterSalePolicy> listPolicies() {
        return policyRepository.findAll();
    }

    public List<PolicySearchResult> search(String query) {
        String normalizedQuery = normalize(query);
        return policyRepository.findAll().stream()
                .map(policy -> new ScoredPolicy(policy, score(policy, normalizedQuery)))
                .filter(scoredPolicy -> scoredPolicy.score() > 0)
                .sorted(Comparator.comparingInt(ScoredPolicy::score).reversed()
                        .thenComparing(scoredPolicy -> scoredPolicy.policy().getPolicyId()))
                .map(scoredPolicy -> PolicySearchResult.from(
                        scoredPolicy.policy(),
                        "Matched query keywords against category and policy text."))
                .toList();
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
            return List.of("质量", "故障", "退换货");
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
        if (normalizedQuery.contains("特殊") || normalizedQuery.contains("不支持")
                || normalizedQuery.contains("定制") || normalizedQuery.contains("生鲜")) {
            return List.of("特殊", "不支持", "定制", "生鲜");
        }
        return List.of(normalizedQuery);
    }

    private static String normalize(String value) {
        Objects.requireNonNull(value, "query must not be null");
        String normalized = value.toLowerCase(Locale.ROOT).trim();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("query must not be blank");
        }
        return normalized;
    }

    private record ScoredPolicy(AfterSalePolicy policy, int score) {
    }
}
