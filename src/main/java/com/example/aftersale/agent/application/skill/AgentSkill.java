package com.example.aftersale.agent.application.skill;

import com.example.aftersale.agent.application.planner.SubtaskType;
import java.util.Set;

/**
 * Represents a composite Agent capability.
 *
 * <p>A skill may coordinate multiple tools, but tool execution must still go through ToolRegistry.
 */
public interface AgentSkill {

    SkillDefinition definition();

    SkillExecutionResult execute(SkillExecutionContext context);

    default String skillName() {
        return definition().skillName();
    }

    default Set<SubtaskType> supportedSubtaskTypes() {
        return definition().supportedSubtaskTypes();
    }
}
