package com.example.aftersale.agent.application.evaluation;

import com.example.aftersale.agent.application.planner.AgentPlan;
import com.example.aftersale.agent.application.planner.AgentPlanValidator;
import com.example.aftersale.agent.application.planner.AgentPlanner;
import com.example.aftersale.agent.application.planner.AgentPlanningContext;
import com.example.aftersale.agent.application.planner.AgentSubtask;
import com.example.aftersale.agent.application.planner.PlannedToolCall;
import com.example.aftersale.agent.application.planner.RuleBasedAgentPlanner;
import com.example.aftersale.agent.application.planner.SubtaskType;
import com.example.aftersale.ticket.domain.IntentType;
import com.example.aftersale.ticket.domain.TicketStatus;
import com.example.aftersale.tool.application.ToolRegistry;
import com.example.aftersale.tool.domain.ToolInput;
import com.example.aftersale.tool.domain.ToolOutput;
import com.example.aftersale.tool.domain.ToolRiskLevel;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class EvaluationApplicationService {

    private static final Instant EVALUATION_CONTEXT_TIME = Instant.parse("2026-05-17T00:00:00Z");
    private static final String RISK_POLICY_SUMMARY =
            "LOW tools may execute directly. HIGH actions require human approval.";
    private static final String SEARCH_POLICY_TOOL = "search_aftersale_policy";

    private final ToolRegistry toolRegistry;
    private final ObjectMapper objectMapper;

    @SuppressFBWarnings(
            value = "EI_EXPOSE_REP2",
            justification = "Spring constructor injection intentionally stores application collaborators.")
    public EvaluationApplicationService(ToolRegistry toolRegistry, ObjectMapper objectMapper) {
        this.toolRegistry = toolRegistry;
        this.objectMapper = objectMapper;
    }

    public List<EvaluationCase> loadCases(Path datasetPath) {
        Objects.requireNonNull(datasetPath, "datasetPath must not be null");
        try {
            List<String> lines = Files.readAllLines(datasetPath);
            List<EvaluationCase> cases = new ArrayList<>();
            for (int index = 0; index < lines.size(); index++) {
                String line = lines.get(index);
                if (!line.isBlank()) {
                    cases.add(parseCase(line, index + 1));
                }
            }
            return List.copyOf(cases);
        } catch (IOException exception) {
            throw new IllegalArgumentException("Failed to read evaluation dataset: " + datasetPath, exception);
        }
    }

    public EvaluationReport runRuleBased(Path datasetPath) {
        return run(datasetPath, new RuleBasedAgentPlanner());
    }

    public EvaluationReport run(Path datasetPath, AgentPlanner planner) {
        Objects.requireNonNull(planner, "planner must not be null");
        return evaluate(loadCases(datasetPath), planner);
    }

    public EvaluationReport evaluate(List<EvaluationCase> cases, AgentPlanner planner) {
        Objects.requireNonNull(planner, "planner must not be null");
        List<EvaluationCase> evaluationCases = List.copyOf(Objects.requireNonNull(cases, "cases must not be null"));
        List<String> availableTools = availableToolNames();
        List<EvaluationResult> results = evaluationCases.stream()
                .map(evaluationCase -> evaluateCase(evaluationCase, planner, availableTools))
                .toList();
        return report(results);
    }

    private EvaluationCase parseCase(String line, int lineNumber) {
        try {
            JsonNode node = objectMapper.readTree(line);
            EvaluationExpected expected = new EvaluationExpected(
                    enumValue(node, "expectedIntent", IntentType.class),
                    enumList(node, "expectedSubtaskTypes", SubtaskType.class),
                    stringList(node, "expectedTools"),
                    enumValue(node, "expectedRiskLevel", ToolRiskLevel.class),
                    stringList(node, "expectedPolicyCategories"),
                    requiredBoolean(node, "expectedRequiresApproval"));
            return new EvaluationCase(
                    requiredText(node, "caseId"),
                    requiredText(node, "userId"),
                    requiredText(node, "orderId"),
                    requiredText(node, "input"),
                    expected,
                    optionalText(node, "notes"));
        } catch (RuntimeException | IOException exception) {
            throw new IllegalArgumentException(
                    "Invalid evaluation case at line " + lineNumber + ": " + exception.getMessage(),
                    exception);
        }
    }

    private EvaluationResult evaluateCase(
            EvaluationCase evaluationCase,
            AgentPlanner planner,
            List<String> availableTools) {
        List<EvaluationFailure> failures = new ArrayList<>();
        AgentPlan plan;
        boolean planValid = true;
        try {
            plan = planner.plan(planningContext(evaluationCase, availableTools));
            AgentPlanValidator.validate(plan, availableTools);
        } catch (RuntimeException exception) {
            planValid = false;
            failures.add(failure(
                    evaluationCase,
                    "planValidity",
                    "valid",
                    "invalid",
                    exception.getMessage()));
            return invalidPlanResult(evaluationCase, failures);
        }

        List<SubtaskType> actualSubtaskTypes = plan.subtasks().stream()
                .map(AgentSubtask::type)
                .toList();
        List<String> actualTools = plannedToolNames(plan);
        List<String> actualPolicyCategories = policyCategories(plan);
        boolean actualRequiresApproval = requiresApproval(plan);

        compare(evaluationCase, "intent", evaluationCase.expected().intent(), plan.intent(), failures);
        compare(evaluationCase, "subtaskTypes", evaluationCase.expected().subtaskTypes(), actualSubtaskTypes,
                failures);
        compareAsSet(evaluationCase, "plannedTools", evaluationCase.expected().tools(), actualTools, failures);
        compare(evaluationCase, "riskLevel", evaluationCase.expected().riskLevel(), plan.riskLevel(), failures);
        compareContains(evaluationCase, "policyCategories", evaluationCase.expected().policyCategories(),
                actualPolicyCategories, failures);
        compare(evaluationCase, "approvalRequirement", evaluationCase.expected().requiresApproval(),
                actualRequiresApproval, failures);

        return new EvaluationResult(
                evaluationCase.caseId(),
                failures.isEmpty(),
                planValid,
                plan.intent(),
                actualSubtaskTypes,
                actualTools,
                plan.riskLevel(),
                actualPolicyCategories,
                actualRequiresApproval,
                failures);
    }

    private EvaluationReport report(List<EvaluationResult> results) {
        int totalCases = results.size();
        int passedCases = (int) results.stream().filter(EvaluationResult::passed).count();
        int failedCases = totalCases - passedCases;
        List<EvaluationFailure> failures = results.stream()
                .flatMap(result -> result.failures().stream())
                .toList();
        List<EvaluationMetric> metrics = List.of(
                metric(results, "intentAccuracy", "intent"),
                metric(results, "subtaskTypeAccuracy", "subtaskTypes"),
                metric(results, "toolCallAccuracy", "plannedTools"),
                metric(results, "riskLevelAccuracy", "riskLevel"),
                metric(results, "policyMatchAccuracy", "policyCategories"),
                metric(results, "approvalRequirementAccuracy", "approvalRequirement"),
                planValidityMetric(results));
        return new EvaluationReport(
                totalCases,
                passedCases,
                failedCases,
                metrics.get(0).value(),
                metrics.get(1).value(),
                metrics.get(2).value(),
                metrics.get(3).value(),
                metrics.get(4).value(),
                metrics.get(5).value(),
                metrics.get(6).value(),
                metrics,
                results,
                failures);
    }

    private EvaluationResult invalidPlanResult(
            EvaluationCase evaluationCase,
            List<EvaluationFailure> failures) {
        return new EvaluationResult(
                evaluationCase.caseId(),
                false,
                false,
                IntentType.UNKNOWN,
                List.of(),
                List.of(),
                ToolRiskLevel.LOW,
                List.of(),
                false,
                failures);
    }

    private AgentPlanningContext planningContext(EvaluationCase evaluationCase, List<String> availableTools) {
        return new AgentPlanningContext(
                "EVAL-" + evaluationCase.caseId(),
                evaluationCase.userId(),
                evaluationCase.orderId(),
                evaluationCase.input(),
                TicketStatus.CREATED,
                availableTools,
                RISK_POLICY_SUMMARY,
                EVALUATION_CONTEXT_TIME);
    }

    private List<String> availableToolNames() {
        return toolRegistry.listDefinitions().stream()
                .map(definition -> definition.toolName())
                .toList();
    }

    private List<String> plannedToolNames(AgentPlan plan) {
        Set<String> toolNames = new LinkedHashSet<>();
        plan.plannedTools().stream()
                .map(PlannedToolCall::toolName)
                .forEach(toolNames::add);
        plan.subtasks().stream()
                .flatMap(subtask -> subtask.plannedTools().stream())
                .map(PlannedToolCall::toolName)
                .forEach(toolNames::add);
        return List.copyOf(toolNames);
    }

    private List<String> policyCategories(AgentPlan plan) {
        Set<String> categories = new LinkedHashSet<>();
        policyCategoriesFor(plan.policyQuery()).forEach(categories::add);
        plan.subtasks().stream()
                .map(AgentSubtask::policyQuery)
                .map(this::policyCategoriesFor)
                .flatMap(List::stream)
                .forEach(categories::add);
        return List.copyOf(categories);
    }

    private List<String> policyCategoriesFor(String policyQuery) {
        ToolOutput output = toolRegistry.execute(SEARCH_POLICY_TOOL, ToolInput.of(Map.of("query", policyQuery)));
        Object results = output.data().get("results");
        if (!(results instanceof List<?> resultList)) {
            return List.of();
        }
        return resultList.stream()
                .filter(Map.class::isInstance)
                .map(Map.class::cast)
                .map(result -> String.valueOf(result.get("category")))
                .toList();
    }

    private static boolean requiresApproval(AgentPlan plan) {
        return plan.riskLevel().requiresApproval()
                || plan.subtasks().stream().anyMatch(subtask -> subtask.riskLevel().requiresApproval());
    }

    private static EvaluationMetric metric(List<EvaluationResult> results, String metricName, String fieldName) {
        int passed = (int) results.stream()
                .filter(result -> result.failures().stream()
                        .noneMatch(failure -> failure.field().equals(fieldName)))
                .count();
        return EvaluationMetric.of(metricName, passed, results.size());
    }

    private static EvaluationMetric planValidityMetric(List<EvaluationResult> results) {
        int passed = (int) results.stream()
                .filter(EvaluationResult::planValid)
                .count();
        return EvaluationMetric.of("planValidityRate", passed, results.size());
    }

    private static void compare(
            EvaluationCase evaluationCase,
            String fieldName,
            Object expected,
            Object actual,
            List<EvaluationFailure> failures) {
        if (!Objects.equals(expected, actual)) {
            failures.add(failure(evaluationCase, fieldName, expected, actual, "Expected value did not match."));
        }
    }

    private static void compareAsSet(
            EvaluationCase evaluationCase,
            String fieldName,
            List<String> expected,
            List<String> actual,
            List<EvaluationFailure> failures) {
        if (!new LinkedHashSet<>(expected).equals(new LinkedHashSet<>(actual))) {
            failures.add(failure(evaluationCase, fieldName, expected, actual, "Expected tool set did not match."));
        }
    }

    private static void compareContains(
            EvaluationCase evaluationCase,
            String fieldName,
            List<String> expected,
            List<String> actual,
            List<EvaluationFailure> failures) {
        if (!actual.containsAll(expected)) {
            failures.add(failure(
                    evaluationCase,
                    fieldName,
                    expected,
                    actual,
                    "Actual policy categories did not contain all expected categories."));
        }
    }

    private static EvaluationFailure failure(
            EvaluationCase evaluationCase,
            String fieldName,
            Object expected,
            Object actual,
            String message) {
        return new EvaluationFailure(
                evaluationCase.caseId(),
                fieldName,
                String.valueOf(expected),
                String.valueOf(actual),
                message == null || message.isBlank() ? "Evaluation failed." : message);
    }

    private static String requiredText(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        if (value.isMissingNode() || value.isNull() || !value.isTextual() || value.asText().isBlank()) {
            throw new IllegalArgumentException(fieldName + " must be a non-blank string");
        }
        return value.asText();
    }

    private static String optionalText(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        if (value.isMissingNode() || value.isNull()) {
            return "";
        }
        if (!value.isTextual()) {
            throw new IllegalArgumentException(fieldName + " must be a string");
        }
        return value.asText();
    }

    private static boolean requiredBoolean(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        if (!value.isBoolean()) {
            throw new IllegalArgumentException(fieldName + " must be a boolean");
        }
        return value.asBoolean();
    }

    private static List<String> stringList(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        if (!value.isArray()) {
            throw new IllegalArgumentException(fieldName + " must be an array");
        }
        List<String> values = new ArrayList<>();
        for (JsonNode item : value) {
            if (!item.isTextual()) {
                throw new IllegalArgumentException(fieldName + " must contain only strings");
            }
            values.add(item.asText());
        }
        return List.copyOf(values);
    }

    private static <T extends Enum<T>> T enumValue(JsonNode node, String fieldName, Class<T> enumType) {
        String value = requiredText(node, fieldName);
        try {
            return Enum.valueOf(enumType, value);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(fieldName + " has unsupported value", exception);
        }
    }

    private static <T extends Enum<T>> List<T> enumList(JsonNode node, String fieldName, Class<T> enumType) {
        return stringList(node, fieldName).stream()
                .map(value -> Enum.valueOf(enumType, value))
                .toList();
    }
}
