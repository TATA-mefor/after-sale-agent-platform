package com.example.aftersale.agent.infrastructure.llm;

import com.example.aftersale.agent.application.planner.AgentPlan;
import com.example.aftersale.agent.application.planner.AgentPlanValidator;
import com.example.aftersale.agent.application.planner.AgentPlanner;
import com.example.aftersale.agent.application.planner.AgentPlanningContext;
import com.example.aftersale.agent.prompt.AgentPlannerPromptFactory;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public class LlmAgentPlanner implements AgentPlanner {

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
        LlmResponse response = llmClient.complete(new LlmRequest(
                model,
                promptFactory.systemPrompt(),
                promptFactory.userPrompt(context),
                timeoutSeconds));
        AgentPlan plan = agentPlanParser.parse(response.rawContent());
        AgentPlanValidator.validate(plan, context.availableTools());
        return plan;
    }
}
