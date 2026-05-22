package com.example.aftersale.agent.application.skill;

import com.example.aftersale.agent.application.handler.SpecialistAgentHandler;
import com.example.aftersale.agent.application.handler.SubtaskExecutionContext;
import com.example.aftersale.agent.application.handler.SubtaskExecutionResult;
import com.example.aftersale.agent.application.planner.SubtaskStatus;
import java.util.Objects;

/**
 * Bridges existing SpecialistAgentHandler implementations into the V4 Skill contract.
 *
 * <p>The adapter preserves the existing handler execution path, including ToolRegistry, Workspace, Approval and
 * ToolCallTrace behavior.
 */
public class SpecialistHandlerSkillAdapter implements AgentSkill {

    private final SkillDefinition definition;
    private final SpecialistAgentHandler handler;

    public SpecialistHandlerSkillAdapter(SkillDefinition definition, SpecialistAgentHandler handler) {
        this.definition = Objects.requireNonNull(definition, "definition must not be null");
        this.handler = Objects.requireNonNull(handler, "handler must not be null");
    }

    @Override
    public SkillDefinition definition() {
        return definition;
    }

    @Override
    public SkillExecutionResult execute(SkillExecutionContext context) {
        SubtaskExecutionResult result = handler.handle(new SubtaskExecutionContext(
                context.runId(),
                context.ticket(),
                context.plan(),
                context.optionalSubtask().orElseThrow(() -> new SkillExecutionException(
                        "Specialist handler skill requires a subtask.")),
                context.workspace(),
                context.allowedTools(),
                context.riskPolicySummary(),
                context.previousResults()));
        if (result.status() == SubtaskStatus.SUCCEEDED) {
            return SkillExecutionResult.succeeded(
                    definition.skillName(),
                    result.summary(),
                    result.evidence(),
                    result.toolCalls());
        }
        if (result.requiresHumanApproval()) {
            return SkillExecutionResult.waitingApproval(definition.skillName(), result.summary());
        }
        return SkillExecutionResult.failed(
                definition.skillName(),
                "SPECIALIST_HANDLER_FAILED",
                result.errorMessage().isBlank() ? result.summary() : result.errorMessage());
    }
}
