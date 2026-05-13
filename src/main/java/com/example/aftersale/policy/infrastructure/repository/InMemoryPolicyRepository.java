package com.example.aftersale.policy.infrastructure.repository;

import com.example.aftersale.policy.domain.AfterSalePolicy;
import com.example.aftersale.policy.domain.PolicyRepository;
import java.time.Instant;
import java.util.List;
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

    private static AfterSalePolicy policy(String policyId, String category, String productType, String policyText) {
        return new AfterSalePolicy(policyId, category, productType, policyText, EFFECTIVE_FROM, EFFECTIVE_TO);
    }
}
