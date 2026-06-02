package io.github.tatame.aftersale.tool.application;

import io.github.tatame.aftersale.tool.domain.ToolDefinition;
import io.github.tatame.aftersale.tool.domain.ToolInput;
import io.github.tatame.aftersale.tool.domain.ToolOutput;

/**
 * 定义一个可由 ToolRegistry 调度的 Agent 工具。
 *
 * <p>边界：工具实现可以调用所属业务 ApplicationService，但不应直接访问 Repository；
 * 高风险能力必须通过 ToolDefinition 标记审批需求。
 */
public interface ToolExecutor {

    ToolDefinition definition();

    ToolOutput execute(ToolInput input);
}
