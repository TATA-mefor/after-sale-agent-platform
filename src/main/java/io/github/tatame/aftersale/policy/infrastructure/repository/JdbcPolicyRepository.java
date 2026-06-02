package io.github.tatame.aftersale.policy.infrastructure.repository;

import io.github.tatame.aftersale.policy.domain.AfterSalePolicy;
import io.github.tatame.aftersale.policy.domain.PolicyRepository;
import io.github.tatame.aftersale.policy.domain.PolicySearchQuery;
import io.github.tatame.aftersale.policy.domain.PolicySearchResult;
import io.github.tatame.aftersale.policy.domain.PolicySnippet;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * 在 mysql profile 下通过 JDBC 读取售后政策并执行确定性关键词检索。
 *
 * <p>边界：该仓储只负责 infrastructure 映射和本地检索，不引入外部向量库、网络检索或 LLM 判断。
 */
@Repository
@Profile("mysql")
public class JdbcPolicyRepository implements PolicyRepository {

    private final JdbcTemplate jdbcTemplate;

    @SuppressFBWarnings(
            value = "EI_EXPOSE_REP2",
            justification = "JdbcTemplate is a Spring-managed infrastructure collaborator.")
    public JdbcPolicyRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<AfterSalePolicy> findAll() {
        return jdbcTemplate.query("""
                SELECT policy_id, category, product_type, policy_text, effective_from, effective_to
                FROM aftersale_policies
                ORDER BY policy_id ASC
                """, (resultSet, rowNumber) -> mapPolicy(resultSet));
    }

    @Override
    public PolicySearchResult search(PolicySearchQuery query) {
        List<PolicySnippet> snippets = findAll().stream()
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

    private static AfterSalePolicy mapPolicy(ResultSet resultSet) throws SQLException {
        return new AfterSalePolicy(
                resultSet.getString("policy_id"),
                resultSet.getString("category"),
                resultSet.getString("product_type"),
                resultSet.getString("policy_text"),
                instant(resultSet.getTimestamp("effective_from")),
                instant(resultSet.getTimestamp("effective_to")));
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
        if (normalizedQuery.contains("特殊") || normalizedQuery.contains("不支持")
                || normalizedQuery.contains("定制") || normalizedQuery.contains("生鲜")
                || normalizedQuery.contains("拆封")) {
            return List.of("特殊", "不支持", "定制", "生鲜");
        }
        if (normalizedQuery.contains("维修") || normalizedQuery.contains("修")
                || normalizedQuery.contains("保修")) {
            return List.of("维修", "修理", "保修");
        }
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
        if (normalizedQuery.contains("优惠券") || normalizedQuery.contains("券")) {
            return List.of("优惠券");
        }
        return List.of(normalizedQuery);
    }

    private static String normalize(String value) {
        return value.toLowerCase(Locale.ROOT).trim();
    }

    private static Instant instant(Timestamp value) {
        return value.toInstant();
    }

    private record ScoredPolicy(AfterSalePolicy policy, int score) {
    }
}
