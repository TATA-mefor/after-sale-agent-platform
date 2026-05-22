package com.example.aftersale.agent.api;

import com.example.aftersale.agent.application.AgentApplicationService;
import com.example.aftersale.agent.application.AgentRunResult;
import com.example.aftersale.common.api.ApiResponse;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
@RequestMapping("/api/tickets/{ticketId}/agent-runs")
public class AgentRunController {

    private final AgentApplicationService agentApplicationService;

    @SuppressFBWarnings(
            value = "EI_EXPOSE_REP2",
            justification = "Spring constructor injection intentionally stores the application service dependency.")
    public AgentRunController(AgentApplicationService agentApplicationService) {
        this.agentApplicationService = agentApplicationService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AgentRunResponse>> createAgentRun(@PathVariable String ticketId) {
        AgentRunResult result = agentApplicationService.runForTicket(ticketId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(AgentRunResponse.from(result)));
    }
}
