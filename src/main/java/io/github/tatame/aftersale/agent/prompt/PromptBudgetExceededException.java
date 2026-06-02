package io.github.tatame.aftersale.agent.prompt;

public class PromptBudgetExceededException extends RuntimeException {

    public PromptBudgetExceededException(String message) {
        super(message);
    }
}
