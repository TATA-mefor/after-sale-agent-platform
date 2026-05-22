package com.example.aftersale.agent.application.skill;

import com.example.aftersale.agent.application.planner.SubtaskType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Read-only registry of composite Agent skills.
 *
 * <p>V4.1 registers skills for discovery and contract validation; AgentRun execution still uses the existing
 * SpecialistAgentHandler path.
 */
@Component
public class SkillRegistry {

    private final Map<String, AgentSkill> skillsByName;
    private final Map<SubtaskType, List<AgentSkill>> skillsBySubtaskType;

    @SuppressFBWarnings(
            value = "CT_CONSTRUCTOR_THROW",
            justification = "The registry rejects duplicate skill names during Spring bean construction.")
    public SkillRegistry(List<AgentSkill> skills, SkillRiskEvaluator riskEvaluator) {
        this.skillsByName = Map.copyOf(indexBySkillName(skills, riskEvaluator));
        this.skillsBySubtaskType = Map.copyOf(indexBySubtaskType(skillsByName.values().stream().toList()));
    }

    public Optional<AgentSkill> findSkill(String skillName) {
        return Optional.ofNullable(skillsByName.get(skillName));
    }

    public List<AgentSkill> findBySubtaskType(SubtaskType type) {
        return skillsBySubtaskType.getOrDefault(type, List.of());
    }

    public List<SkillDefinition> listDefinitions() {
        return skillsByName.values().stream()
                .map(AgentSkill::definition)
                .toList();
    }

    private static Map<String, AgentSkill> indexBySkillName(
            List<AgentSkill> skills,
            SkillRiskEvaluator riskEvaluator) {
        Map<String, AgentSkill> indexedSkills = new LinkedHashMap<>();
        for (AgentSkill skill : skills) {
            riskEvaluator.validate(skill.definition());
            AgentSkill previous = indexedSkills.putIfAbsent(skill.skillName(), skill);
            if (previous != null) {
                throw new IllegalStateException("Duplicate skillName: " + skill.skillName());
            }
        }
        return indexedSkills;
    }

    private static Map<SubtaskType, List<AgentSkill>> indexBySubtaskType(List<AgentSkill> skills) {
        Map<SubtaskType, List<AgentSkill>> indexedSkills = new EnumMap<>(SubtaskType.class);
        for (AgentSkill skill : skills) {
            for (SubtaskType type : skill.supportedSubtaskTypes()) {
                indexedSkills.computeIfAbsent(type, ignored -> new ArrayList<>()).add(skill);
            }
        }
        Map<SubtaskType, List<AgentSkill>> immutableIndex = new EnumMap<>(SubtaskType.class);
        indexedSkills.forEach((type, skillList) -> immutableIndex.put(type, List.copyOf(skillList)));
        return immutableIndex;
    }
}
