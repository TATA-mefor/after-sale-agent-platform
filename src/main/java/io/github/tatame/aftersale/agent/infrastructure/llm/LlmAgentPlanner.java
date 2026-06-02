package io.github.tatame.aftersale.agent.infrastructure.llm;

import io.github.tatame.aftersale.agent.application.planner.AgentPlan;
import io.github.tatame.aftersale.agent.application.planner.AgentPlanValidator;
import io.github.tatame.aftersale.agent.application.planner.AgentPlanner;
import io.github.tatame.aftersale.agent.application.planner.AgentPlanningContext;
import io.github.tatame.aftersale.agent.prompt.AgentPlannerPromptFactory;
import io.github.tatame.aftersale.agent.prompt.PromptBuildResult;
import io.github.tatame.aftersale.agent.prompt.PromptUsageTelemetry;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 使用 LLM Provider 基于紧凑 Planner Prompt 生成 AgentPlan。
 *
 * <p>边界：LLM 只能返回结构化计划数据。本 Planner 负责解析和校验响应，所有工具执行和业务状态变更
 * 都保留在 Java 服务中。
 */
public class LlmAgentPlanner implements AgentPlanner {

    private static final Logger LOGGER = LoggerFactory.getLogger(LlmAgentPlanner.class);

    private final String model;
    private final int timeoutSeconds;
    private final LlmClient llmClient;
    private final AgentPlanParser agentPlanParser;
    private final AgentPlannerPromptFactory promptFactory;

    @SuppressFBWarnings(
            value = "EI_EXPOSE_REP2",
            justification = "Planner stores injected collaborators; configuration values are copied.")
    public LlmAgentPlanner(
            AgentPlannerProperties.Llm properties,
            LlmClient llmClient,
            AgentPlanParser agentPlanParser,
            AgentPlannerPromptFactory promptFactory) {
        this.model = properties.getModel();
        this.timeoutSeconds = properties.getTimeoutSeconds();
        this.llmClient = llmClient;
        this.agentPlanParser = agentPlanParser;
        this.promptFactory = promptFactory;
    }

    /**
     * 构建 prompt，调用已配置 Provider，并返回经过 Java 校验的 AgentPlan。
     *
     * <p>Provider 原始内容会立即解析，本类不会把完整 prompt 或 completion 作为历史持久化。
     */
    @Override
    public AgentPlan plan(AgentPlanningContext context) {
        PromptBuildResult prompt = promptFactory.build(context);
        logPromptTelemetry(prompt.telemetry());
        LlmResponse response = llmClient.complete(new LlmRequest(
                model,
                prompt.systemPrompt(),
                prompt.userPrompt(),
                timeoutSeconds));
        AgentPlan plan = agentPlanParser.parse(response.rawContent());
        AgentPlanValidator.validate(plan, context.availableTools());
        return plan;
    }

    private static void logPromptTelemetry(PromptUsageTelemetry telemetry) {
        LOGGER.info(
                "llm_prompt_budget systemPromptTokens={} plannerContractTokens={} toolCatalogTokens={} "
                        + "ticketContextTokens={} orderContextTokens={} historyTokens={} ragContextTokens={} "
                        + "optionalTokensDropped={} totalInputTokens={} maxOutputTokens={} budgetExceeded={} "
                        + "budgetAction={} outputTokens={} cacheReadTokens={}",
                telemetry.systemPromptTokens(),
                telemetry.plannerContractTokens(),
                telemetry.toolCatalogTokens(),
                telemetry.ticketContextTokens(),
                telemetry.orderContextTokens(),
                telemetry.historyTokens(),
                telemetry.ragContextTokens(),
                telemetry.optionalTokensDropped(),
                telemetry.totalInputTokens(),
                telemetry.maxOutputTokens(),
                telemetry.budgetExceeded(),
                telemetry.budgetAction(),
                telemetry.outputTokens(),
                telemetry.cacheReadTokens());
    }
}
