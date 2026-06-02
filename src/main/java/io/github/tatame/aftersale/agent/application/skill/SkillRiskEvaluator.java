package io.github.tatame.aftersale.agent.application.skill;

import io.github.tatame.aftersale.tool.application.ToolRegistry;
import io.github.tatame.aftersale.tool.domain.ToolDefinition;
import io.github.tatame.aftersale.tool.domain.ToolRiskLevel;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Comparator;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class SkillRiskEvaluator {

    private final ToolRegistry toolRegistry;

    @SuppressFBWarnings(
            value = "EI_EXPOSE_REP2",
            justification = "Spring constructor injection intentionally stores the ToolRegistry collaborator.")
    public SkillRiskEvaluator(ToolRegistry toolRegistry) {
        this.toolRegistry = toolRegistry;
    }

    public void validate(SkillDefinition definition) {
        Objects.requireNonNull(definition, "definition must not be null");
        ToolRiskLevel highestRequiredToolRisk = definition.requiredTools().stream()
                .map(this::riskLevelForTool)
                .max(Comparator.comparingInt(Enum::ordinal))
                .orElse(ToolRiskLevel.LOW);
        if (definition.riskLevel().ordinal() < highestRequiredToolRisk.ordinal()) {
            throw new IllegalStateException("Skill " + definition.skillName()
                    + " riskLevel " + definition.riskLevel()
                    + " is lower than required tool risk " + highestRequiredToolRisk);
        }
    }

    private ToolRiskLevel riskLevelForTool(String toolName) {
        return toolRegistry.findDefinition(toolName)
                .map(ToolDefinition::riskLevel)
                .orElseThrow(() -> new IllegalStateException(
                        "Skill required tool is not registered: " + toolName));
    }
}
