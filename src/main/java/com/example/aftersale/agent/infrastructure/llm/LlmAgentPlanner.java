package com.example.aftersale.agent.infrastructure.llm;

import com.example.aftersale.agent.application.planner.AgentPlan;
import com.example.aftersale.agent.application.planner.AgentPlanValidator;
import com.example.aftersale.agent.application.planner.AgentPlanner;
import com.example.aftersale.agent.application.planner.AgentPlanningContext;
import com.example.aftersale.agent.prompt.AgentPlannerPromptFactory;
import com.example.aftersale.agent.prompt.PromptBuildResult;
import com.example.aftersale.agent.prompt.PromptUsageTelemetry;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
