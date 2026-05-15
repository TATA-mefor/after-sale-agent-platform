package com.example.aftersale.agent.application.handler;

import com.example.aftersale.agent.application.planner.AgentSubtask;
import com.example.aftersale.agent.application.planner.PlannedToolCall;
import com.example.aftersale.tool.application.ToolRegistry;
import com.example.aftersale.tool.application.ToolTraceContext;
import com.example.aftersale.tool.domain.ToolExecutionStatus;
import com.example.aftersale.tool.domain.ToolInput;
import com.example.aftersale.tool.domain.ToolOutput;
import com.example.aftersale.tool.domain.ToolRiskLevel;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

abstract class AbstractSpecialistAgentHandler implements SpecialistAgentHandler {

    protected static final String ADD_TICKET_NOTE_TOOL = "add_ticket_note";
    protected static final String GET_ORDER_BY_ID_TOOL = "get_order_by_id";
    protected static final String SEARCH_POLICY_TOOL = "search_aftersale_policy";

    private final ToolRegistry toolRegistry;

    @SuppressFBWarnings(
            value = "EI_EXPOSE_REP2",
            justification = "Spring constructor injection intentionally stores the ToolRegistry collaborator.")
    AbstractSpecialistAgentHandler(ToolRegistry toolRegistry) {
        this.toolRegistry = toolRegistry;
    }

    @Override
    public final SubtaskExecutionResult handle(SubtaskExecutionContext context) {
        if (!supports(context.subtask().type())) {
            return SubtaskExecutionResult.failed(
                    context.subtask().subtaskId(),
                    context.subtask().type(),
                    handlerName() + " does not support " + context.subtask().type());
        }
        if (context.subtask().riskLevel() == ToolRiskLevel.HIGH) {
            return SubtaskExecutionResult.requiresApproval(
                    context.subtask().subtaskId(),
                    context.subtask().type(),
                    "Subtask " + context.subtask().subtaskId() + " " + context.subtask().type().name()
                            + " requires human approval before specialist execution.");
        }
        return executeKnownTools(context, toolPlan(context));
    }

    protected abstract List<String> requiredToolNames();

    protected List<PlannedToolCall> toolPlan(SubtaskExecutionContext context) {
        LinkedHashSet<String> toolNames = new LinkedHashSet<>();
        List<PlannedToolCall> plannedTools = new ArrayList<>();
        for (PlannedToolCall plannedTool : context.subtask().plannedTools()) {
            toolNames.add(plannedTool.toolName());
            plannedTools.add(plannedTool);
        }
        for (String requiredToolName : requiredToolNames()) {
            if (toolNames.add(requiredToolName)) {
                plannedTools.add(new PlannedToolCall(requiredToolName, handlerName() + " required tool."));
            }
        }
        return orderPolicyBeforeActionTools(plannedTools);
    }

    private static List<PlannedToolCall> orderPolicyBeforeActionTools(List<PlannedToolCall> plannedTools) {
        List<PlannedToolCall> orderedTools = new ArrayList<>();
        appendToolIfPresent(plannedTools, orderedTools, GET_ORDER_BY_ID_TOOL);
        appendToolIfPresent(plannedTools, orderedTools, SEARCH_POLICY_TOOL);
        for (PlannedToolCall plannedTool : plannedTools) {
            if (!GET_ORDER_BY_ID_TOOL.equals(plannedTool.toolName())
                    && !SEARCH_POLICY_TOOL.equals(plannedTool.toolName())) {
                orderedTools.add(plannedTool);
            }
        }
        return orderedTools;
    }

    private static void appendToolIfPresent(
            List<PlannedToolCall> plannedTools,
            List<PlannedToolCall> orderedTools,
            String toolName) {
        plannedTools.stream()
                .filter(plannedTool -> toolName.equals(plannedTool.toolName()))
                .findFirst()
                .ifPresent(orderedTools::add);
    }

    private SubtaskExecutionResult executeKnownTools(
            SubtaskExecutionContext context,
            List<PlannedToolCall> plannedTools) {
        List<String> evidence = new ArrayList<>();
        List<String> toolCalls = new ArrayList<>();
        for (PlannedToolCall plannedTool : plannedTools) {
            try {
                executePlannedTool(context, plannedTool, evidence, toolCalls);
            } catch (RuntimeException exception) {
                return new SubtaskExecutionResult(
                        context.subtask().subtaskId(),
                        context.subtask().type(),
                        com.example.aftersale.agent.application.planner.SubtaskStatus.FAILED,
                        failureSummary(context.subtask(), exception),
                        evidence,
                        toolCalls,
                        failureMessage(exception),
                        false);
            }
        }
        return SubtaskExecutionResult.succeeded(
                context.subtask().subtaskId(),
                context.subtask().type(),
                successSummary(context.subtask(), evidence),
                evidence,
                toolCalls);
    }

    private void executePlannedTool(
            SubtaskExecutionContext context,
            PlannedToolCall plannedTool,
            List<String> evidence,
            List<String> toolCalls) {
        switch (plannedTool.toolName()) {
            case GET_ORDER_BY_ID_TOOL -> executeOrderLookup(context, evidence, toolCalls);
            case SEARCH_POLICY_TOOL -> executePolicySearch(context, evidence, toolCalls);
            case ADD_TICKET_NOTE_TOOL -> executeTicketNote(context, evidence, toolCalls);
            default -> throw new IllegalArgumentException(
                    "Specialist handler does not support executing planned tool: " + plannedTool.toolName());
        }
    }

    private void executeOrderLookup(
            SubtaskExecutionContext context,
            List<String> evidence,
            List<String> toolCalls) {
        ToolOutput orderOutput = executeTool(context.runId(), GET_ORDER_BY_ID_TOOL, tracedInput(Map.of(
                "orderId", context.ticket().getOrderId()), context.subtask()));
        toolCalls.add(GET_ORDER_BY_ID_TOOL);
        ensureToolSucceeded(orderOutput);
        evidence.add(evidencePrefix(context.subtask()) + orderEvidence(orderOutput));
    }

    private void executePolicySearch(
            SubtaskExecutionContext context,
            List<String> evidence,
            List<String> toolCalls) {
        ToolOutput policyOutput = executeTool(context.runId(), SEARCH_POLICY_TOOL, tracedInput(Map.of(
                "query", context.subtask().policyQuery()), context.subtask()));
        toolCalls.add(SEARCH_POLICY_TOOL);
        ensureToolSucceeded(policyOutput);
        evidence.addAll(extractEvidence(policyOutput).stream()
                .map(item -> evidencePrefix(context.subtask()) + item)
                .toList());
    }

    private void executeTicketNote(
            SubtaskExecutionContext context,
            List<String> evidence,
            List<String> toolCalls) {
        ToolOutput noteOutput = executeTool(context.runId(), ADD_TICKET_NOTE_TOOL, tracedInput(Map.of(
                "ticketId", context.ticket().getTicketId(),
                "note", buildSubtaskNote(context, evidence)), context.subtask()));
        toolCalls.add(ADD_TICKET_NOTE_TOOL);
        ensureToolSucceeded(noteOutput);
    }

    private ToolOutput executeTool(String runId, String toolName, ToolInput input) {
        List<ToolOutput> outputs = new ArrayList<>();
        ToolTraceContext.runWith(runId, () -> outputs.add(toolRegistry.execute(toolName, input)));
        return outputs.get(0);
    }

    private String buildSubtaskNote(SubtaskExecutionContext context, List<String> evidence) {
        String note = "Subtask " + context.subtask().subtaskId()
                + " " + context.subtask().type().name()
                + " target=" + context.subtask().target()
                + ". " + context.agentPlan().noteToAdd();
        if (evidence.isEmpty()) {
            return note;
        }
        return note + " Evidence: " + String.join("; ", evidence);
    }

    private static ToolInput tracedInput(Map<String, Object> arguments, AgentSubtask subtask) {
        Map<String, Object> tracedArguments = new LinkedHashMap<>(arguments);
        tracedArguments.put("subtaskId", subtask.subtaskId());
        tracedArguments.put("subtaskType", subtask.type().name());
        tracedArguments.put("subtaskTarget", subtask.target());
        return ToolInput.of(tracedArguments);
    }

    private static void ensureToolSucceeded(ToolOutput output) {
        if (output.status() != ToolExecutionStatus.SUCCEEDED) {
            throw new IllegalStateException(output.toolName() + " failed: " + output.message());
        }
    }

    private static List<String> extractEvidence(ToolOutput policyOutput) {
        Object results = policyOutput.data().get("results");
        if (!(results instanceof List<?> resultList) || resultList.isEmpty()) {
            return List.of("No matching after-sale policy was found.");
        }
        return resultList.stream()
                .filter(Map.class::isInstance)
                .map(Map.class::cast)
                .map(result -> result.get("policyId") + ": " + result.get("category"))
                .toList();
    }

    private static String orderEvidence(ToolOutput orderOutput) {
        Map<String, Object> data = orderOutput.data();
        return "Order " + data.get("orderId")
                + ": " + data.get("productName")
                + ", status=" + data.get("orderStatus")
                + ", aftersaleWindow=" + data.get("whetherInAftersaleWindow")
                + ", deadline=" + data.get("aftersaleDeadline");
    }

    private static String evidencePrefix(AgentSubtask subtask) {
        return "[" + subtask.subtaskId() + " " + subtask.type().name() + "] ";
    }

    private static String successSummary(AgentSubtask subtask, List<String> evidence) {
        return subtask.subtaskId() + " " + subtask.type().name()
                + " succeeded with " + evidence.size() + " evidence item(s).";
    }

    private static String failureSummary(AgentSubtask subtask, RuntimeException exception) {
        return "Subtask " + subtask.subtaskId() + " " + subtask.type().name()
                + " failed: " + failureMessage(exception);
    }

    private static String failureMessage(RuntimeException exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return exception.getClass().getSimpleName();
        }
        return message;
    }

    private String handlerName() {
        return getClass().getSimpleName();
    }
}
