package com.example.aftersale.agent.api;

import com.example.aftersale.agent.application.executiontree.ExecutionTreeApplicationService;
import com.example.aftersale.agent.application.executiontree.ExecutionTreeResponse;
import com.example.aftersale.common.api.ApiResponse;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/agent-runs/{runId}/execution-tree")
public class AgentExecutionTreeController {

    private final ExecutionTreeApplicationService executionTreeApplicationService;

    @SuppressFBWarnings(
            value = "EI_EXPOSE_REP2",
            justification = "Spring constructor injection intentionally stores the application service dependency.")
    public AgentExecutionTreeController(ExecutionTreeApplicationService executionTreeApplicationService) {
        this.executionTreeApplicationService = executionTreeApplicationService;
    }

    @GetMapping
    public ApiResponse<ExecutionTreeResponse> getExecutionTree(@PathVariable String runId) {
        return ApiResponse.success(executionTreeApplicationService.getExecutionTree(runId));
    }
}
