package com.example.aftersale.agent.api;

import com.example.aftersale.agent.application.executiontree.ExecutionTreeApplicationService;
import com.example.aftersale.agent.application.executiontree.ExecutionTreeResponse;
import com.example.aftersale.common.api.ApiResponse;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
/*
最后把这次 run 组织成树状轨迹，通常是：

根节点：本次 AgentRun
子节点：每个 subtask
叶子：工具调用与结果
这样前端或排障方可以回答三件事：

计划是什么
实际做了什么
为什么得到这个结果

*/
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
