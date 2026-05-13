package com.example.aftersale.trace.api;

import com.example.aftersale.agent.application.AgentApplicationService;
import com.example.aftersale.common.api.ApiResponse;
import com.example.aftersale.trace.application.ToolCallTraceApplicationService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/agent-runs/{runId}/traces")
public class AgentTraceController {

    private final AgentApplicationService agentApplicationService;
    private final ToolCallTraceApplicationService traceApplicationService;

    @SuppressFBWarnings(
            value = "EI_EXPOSE_REP2",
            justification = "Spring constructor injection intentionally stores the application service dependencies.")
    public AgentTraceController(
            AgentApplicationService agentApplicationService,
            ToolCallTraceApplicationService traceApplicationService) {
        this.agentApplicationService = agentApplicationService;
        this.traceApplicationService = traceApplicationService;
    }

    @GetMapping
    public ApiResponse<List<ToolCallTraceResponse>> getTraces(@PathVariable String runId) {
        agentApplicationService.getAgentRun(runId);
        List<ToolCallTraceResponse> traces = traceApplicationService.findByRunId(runId).stream()
                .map(ToolCallTraceResponse::from)
                .toList();
        return ApiResponse.success(traces);
    }
}
