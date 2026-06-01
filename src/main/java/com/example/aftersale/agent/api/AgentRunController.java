package com.example.aftersale.agent.api;

import com.example.aftersale.agent.application.AgentApplicationService;
import com.example.aftersale.agent.application.AgentRunResult;
import com.example.aftersale.common.api.ApiResponse;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 暴露为工单触发 AgentRun 的 HTTP 入口。
 *
 * <p>边界：Controller 只做 API 编排和响应转换，不能直接访问 Repository、ToolRegistry 或具体 Planner。
 */
@RestController
@RequestMapping("/api")
@Tag(name = "Agent Runs", description = "Run the Agent for a ticket through application-service orchestration.")
public class AgentRunController {

    private final AgentApplicationService agentApplicationService;

    @SuppressFBWarnings(
            value = "EI_EXPOSE_REP2",
            justification = "Spring constructor injection intentionally stores the application service dependency.")
    public AgentRunController(AgentApplicationService agentApplicationService) {
        this.agentApplicationService = agentApplicationService;
    }

    @PostMapping("/tickets/{ticketId}/agent-runs")
    @Operation(
            summary = "Create an AgentRun for a ticket",
            description = "Runs the configured offline planner and handlers through ToolRegistry. RAG evidence is "
                    + "policy support only and high-risk business actions remain approval-gated.")
    public ResponseEntity<ApiResponse<AgentRunResponse>> createAgentRun(
            @Parameter(description = "Ticket id to process.", example = "T-DEMO-1001")
            @PathVariable String ticketId) {
        AgentRunResult result = agentApplicationService.runForTicket(ticketId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(AgentRunResponse.from(result)));
    }

    @GetMapping("/agent-runs/{runId}")
    @Operation(
            summary = "Get AgentRun status",
            description = "Returns a read-only AgentRun status summary for polling. This endpoint does not start "
                    + "a planner, execute tools, call ToolRegistry, mutate tickets, write ToolCallTrace records, "
                    + "or inline execution-tree and workspace details.")
    public ApiResponse<AgentRunStatusResponse> getAgentRun(
            @Parameter(description = "AgentRun id.", example = "RUN-DEMO-1001")
            @PathVariable String runId) {
        return ApiResponse.success(AgentRunStatusResponse.from(agentApplicationService.getAgentRun(runId)));
    }
}
