package com.example.aftersale.agent.application.handler;

import com.example.aftersale.agent.application.planner.SubtaskType;

/**
 * 表示一种可处理特定 Agent 子任务类型的专业处理器。
 *
 * <p>边界：实现类只能在 SubtaskExecutionContext 内工作，并通过 ToolRegistry 间接获取业务事实；
 * 不应直接访问 Repository、LLM 或外部 HTTP 层。
 */
public interface SpecialistAgentHandler {

    SubtaskType supportedType();

    /**
     * 执行一个已匹配的子任务，并返回结构化状态、证据、工具调用和审批需求。
     */
    SubtaskExecutionResult handle(SubtaskExecutionContext context);

    default boolean supports(SubtaskType type) {
        return supportedType() == type;
    }
}
