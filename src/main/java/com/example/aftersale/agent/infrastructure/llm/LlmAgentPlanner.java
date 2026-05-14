package com.example.aftersale.agent.infrastructure.llm;

import com.example.aftersale.agent.application.planner.AgentPlan;
import com.example.aftersale.agent.application.planner.AgentPlanner;
import com.example.aftersale.agent.application.planner.AgentPlanningContext;

public class LlmAgentPlanner implements AgentPlanner {

    private final String provider;

    public LlmAgentPlanner(AgentPlannerProperties.Llm properties) {
        this.provider = properties.getProvider();
    }

    @Override
    public AgentPlan plan(AgentPlanningContext context) {
        throw new UnsupportedOperationException(
                "Real LLM planner call is not implemented yet. TODO: call provider "
                        + provider + " and parse AgentPlan.");
    }
}
