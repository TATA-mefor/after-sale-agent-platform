package com.example.aftersale.tool.application.policy;

import com.example.aftersale.policy.application.PolicyApplicationService;
import com.example.aftersale.policy.domain.PolicySearchResult;
import com.example.aftersale.policy.domain.PolicySnippet;
import com.example.aftersale.tool.application.ToolExecutor;
import com.example.aftersale.tool.domain.ToolDefinition;
import com.example.aftersale.tool.domain.ToolInput;
import com.example.aftersale.tool.domain.ToolOutput;
import com.example.aftersale.tool.domain.ToolRiskLevel;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class SearchAfterSalePolicyToolExecutor implements ToolExecutor {

    private static final ToolDefinition DEFINITION = ToolDefinition.of(
            "search_aftersale_policy",
            "Search after-sale policy snippets by controlled in-memory keyword retrieval.",
            "{\"query\":\"string\"}",
            "{\"results\":[{\"policyId\":\"string\",\"category\":\"string\","
                    + "\"productType\":\"string\",\"matchedText\":\"string\",\"matchReason\":\"string\"}],"
                    + "\"message\":\"string\"}",
            ToolRiskLevel.LOW);

    private final PolicyApplicationService policyApplicationService;

    @SuppressFBWarnings(
            value = "EI_EXPOSE_REP2",
            justification = "Spring constructor injection intentionally stores the application service dependency.")
    public SearchAfterSalePolicyToolExecutor(PolicyApplicationService policyApplicationService) {
        this.policyApplicationService = policyApplicationService;
    }

    @Override
    public ToolDefinition definition() {
        return DEFINITION;
    }

    @Override
    public ToolOutput execute(ToolInput input) {
        PolicySearchResult result = policyApplicationService.search(input.requireString("query"));
        return ToolOutput.succeeded(DEFINITION.toolName(), Map.of(
                "results", result.snippets().stream()
                        .map(SearchAfterSalePolicyToolExecutor::toResultMap)
                        .toList(),
                "message", result.message()));
    }

    private static Map<String, Object> toResultMap(PolicySnippet snippet) {
        return Map.of(
                "policyId", snippet.policyId(),
                "category", snippet.category(),
                "productType", snippet.productType(),
                "matchedText", snippet.snippetText(),
                "matchReason", snippet.matchReason());
    }
}
