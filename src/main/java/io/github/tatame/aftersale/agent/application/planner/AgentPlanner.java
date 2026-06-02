package io.github.tatame.aftersale.agent.application.planner;

public interface AgentPlanner {

    /**
     * 为工单生成结构化计划，但不执行工具，也不修改领域状态。
     *Planner 的职责是把输入转成结构化计划，例如：
先查订单
再查售后政策
再写工单备注
最后给出建议
     * <p>边界：实现可以使用确定性规则或 LLM，但返回的计划仍必须经过 Java 校验，并且只能通过
     * ToolRegistry 执行。
     */
    AgentPlan plan(AgentPlanningContext context);
}
