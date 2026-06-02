package io.github.tatame.aftersale.agent.application.planner;

public class AgentPlanValidationException extends RuntimeException {

    public AgentPlanValidationException(String message) {
        super(message);
    }

    public AgentPlanValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
