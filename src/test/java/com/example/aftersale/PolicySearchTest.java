package com.example.aftersale;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.aftersale.policy.application.PolicyApplicationService;
import com.example.aftersale.policy.application.PolicySearchResult;
import com.example.aftersale.tool.application.ToolRegistry;
import com.example.aftersale.tool.domain.ToolExecutionStatus;
import com.example.aftersale.tool.domain.ToolInput;
import com.example.aftersale.tool.domain.ToolOutput;
import com.example.aftersale.tool.domain.ToolRiskLevel;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class PolicySearchTest {

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
    void searchesQualityIssuePolicy() {
        List<PolicySearchResult> results = policyApplicationService.search("商品有质量问题，想退换货");

        assertThat(results).isNotEmpty();
        assertThat(results.get(0).policyId()).isEqualTo("POL-QUALITY-RETURN-EXCHANGE");
        assertThat(results.get(0).category()).isEqualTo("质量问题退换货规则");
        assertThat(results.get(0).matchedText()).contains("质量问题");
        assertThat(results.get(0).matchReason()).contains("Matched query keywords");
    }

    @Test
    void searchesLogisticsDisputePolicy() {
        List<PolicySearchResult> results = policyApplicationService.search("物流显示签收但我没收到货");

        assertThat(results).isNotEmpty();
        assertThat(results.get(0).policyId()).isEqualTo("POL-LOGISTICS-NOT-RECEIVED");
        assertThat(results.get(0).category()).isEqualTo("已签收未收到物流争议规则");
        assertThat(results.get(0).matchedText()).contains("未收到货");
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

        List<?> results = (List<?>) output.data().get("results");
        assertThat(results).isNotEmpty();
        Map<?, ?> firstResult = (Map<?, ?>) results.get(0);
        assertThat(firstResult.get("policyId")).isEqualTo("POL-QUALITY-RETURN-EXCHANGE");
        assertThat(firstResult.containsKey("matchReason")).isTrue();
        assertThat(firstResult.containsKey("matchedText")).isTrue();
    }

    @Test
    void searchPolicyToolReturnsClearEmptyResultWhenNothingMatches() {
        ToolOutput output = toolRegistry.execute("search_aftersale_policy", ToolInput.of(Map.of(
                "query", "会员积分规则")));

        assertThat(output.status()).isEqualTo(ToolExecutionStatus.SUCCEEDED);
        assertThat(output.data()).containsEntry("message", "No after-sale policy matched the query.");
        assertThat((List<?>) output.data().get("results")).isEmpty();
    }
}
