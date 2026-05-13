package com.example.aftersale.tool.application.policy;

import com.example.aftersale.policy.application.PolicyApplicationService;
import com.example.aftersale.policy.application.PolicySearchResult;
import com.example.aftersale.tool.application.ToolExecutor;
import com.example.aftersale.tool.domain.ToolDefinition;
import com.example.aftersale.tool.domain.ToolInput;
import com.example.aftersale.tool.domain.ToolOutput;
import com.example.aftersale.tool.domain.ToolRiskLevel;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class SearchAfterSalePolicyTool implements ToolExecutor {

    private static final ToolDefinition DEFINITION = ToolDefinition.of(
            "search_aftersale_policy",
            "Search after-sale policies by simple in-memory keyword matching.",
            "{\"query\":\"string\"}",
            "{\"results\":[{\"policyId\":\"string\",\"category\":\"string\","
                    + "\"matchedText\":\"string\",\"matchReason\":\"string\"}]}",
            ToolRiskLevel.LOW);

    private final PolicyApplicationService policyApplicationService;

    @SuppressFBWarnings(
            value = "EI_EXPOSE_REP2",
            justification = "Spring constructor injection intentionally stores the application service dependency.")
    public SearchAfterSalePolicyTool(PolicyApplicationService policyApplicationService) {
        this.policyApplicationService = policyApplicationService;
    }

    @Override
    public ToolDefinition definition() {
        return DEFINITION;
    }

    @Override
    public ToolOutput execute(ToolInput input) {
        List<PolicySearchResult> results = policyApplicationService.search(input.requireString("query"));
        if (results.isEmpty()) {
            return ToolOutput.succeeded(DEFINITION.toolName(), Map.of(
                    "results", List.of(),
                    "message", "No after-sale policy matched the query."));
        }
        return ToolOutput.succeeded(DEFINITION.toolName(), Map.of(
                "results", results.stream()
                        .map(SearchAfterSalePolicyTool::toResultMap)
                        .toList()));
    }

    private static Map<String, Object> toResultMap(PolicySearchResult result) {
        return Map.of(
                "policyId", result.policyId(),
                "category", result.category(),
                "matchedText", result.matchedText(),
                "matchReason", result.matchReason());
    }
}
