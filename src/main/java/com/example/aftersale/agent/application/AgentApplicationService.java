package com.example.aftersale.agent.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.aftersale.agent.application.planner.AgentPlan;
import com.example.aftersale.agent.application.planner.AgentPlanValidator;
import com.example.aftersale.agent.application.planner.AgentPlanner;
import com.example.aftersale.agent.application.planner.AgentPlanningContext;
import com.example.aftersale.agent.application.planner.AgentSubtask;
import com.example.aftersale.agent.application.planner.PlannedToolCall;
import com.example.aftersale.agent.application.planner.SubtaskStatus;
import com.example.aftersale.agent.domain.AgentRun;
import com.example.aftersale.agent.domain.AgentRunRepository;
import com.example.aftersale.common.exception.ResourceNotFoundException;
import com.example.aftersale.ticket.application.TicketApplicationService;
import com.example.aftersale.ticket.domain.IntentType;
import com.example.aftersale.ticket.domain.Ticket;
import com.example.aftersale.ticket.domain.TicketStatus;
import com.example.aftersale.tool.application.ToolRegistry;
import com.example.aftersale.tool.application.ToolTraceContext;
import com.example.aftersale.tool.domain.ToolExecutionStatus;
import com.example.aftersale.tool.domain.ToolDefinition;
import com.example.aftersale.tool.domain.ToolInput;
import com.example.aftersale.tool.domain.ToolOutput;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class AgentApplicationService {

    private static final String SEARCH_POLICY_TOOL = "search_aftersale_policy";
    private static final String GET_ORDER_BY_ID_TOOL = "get_order_by_id";
    private static final String ADD_TICKET_NOTE_TOOL = "add_ticket_note";
    private static final String RISK_POLICY_SUMMARY =
            "LOW tools may execute directly. HIGH actions such as refund, compensation, payment mutation, "
                    + "or dispute closure require human approval.";

    private final AgentRunRepository agentRunRepository;
    private final TicketApplicationService ticketApplicationService;
    private final ToolRegistry toolRegistry;
    private final AgentPlanner agentPlanner;
    private final ObjectMapper objectMapper;

    @SuppressFBWarnings(
            value = "EI_EXPOSE_REP2",
            justification = "Spring constructor injection intentionally stores application collaborators.")
    public AgentApplicationService(
            AgentRunRepository agentRunRepository,
            TicketApplicationService ticketApplicationService,
            ToolRegistry toolRegistry,
            AgentPlanner agentPlanner,
            ObjectMapper objectMapper) {
        this.agentRunRepository = agentRunRepository;
        this.ticketApplicationService = ticketApplicationService;
        this.toolRegistry = toolRegistry;
        this.agentPlanner = agentPlanner;
        this.objectMapper = objectMapper;
    }

    public AgentRunResult runForTicket(String ticketId) {
        Ticket ticket = ticketApplicationService.getTicket(ticketId);
        AgentRun agentRun = AgentRun.start("RUN-" + UUID.randomUUID(), ticket.getTicketId(), Instant.now());
        agentRunRepository.save(agentRun);

        IntentType intent = IntentType.UNKNOWN;
        List<String> toolCalls = new ArrayList<>();
        try {
            AgentPlanningContext planningContext = planningContext(ticket);
            AgentPlan plan = agentPlanner.plan(planningContext);
            AgentPlanValidator.validate(plan, planningContext.availableTools());
            intent = plan.intent();

            ticketApplicationService.classifyTicketIntent(ticket.getTicketId(), intent);
            ticketApplicationService.updateTicketStatus(ticket.getTicketId(), TicketStatus.AGENT_RUNNING, null);

            List<String> evidence = new ArrayList<>();
            List<SubtaskExecutionResult> subtaskResults = List.of();
            String finalSuggestion;
            if (plan.hasSubtasks()) {
                subtaskResults = executeSubtasks(agentRun.getRunId(), ticket, plan, evidence, toolCalls);
                finalSuggestion = buildMultiIntentFinalSuggestion(plan, subtaskResults, evidence);
            } else {
                for (PlannedToolCall plannedTool : plan.plannedTools()) {
                    executePlannedTool(agentRun.getRunId(), ticket, plan, plannedTool, evidence, toolCalls);
                }
                finalSuggestion = buildFinalSuggestion(plan, evidence);
            }

            String completedPlanJson = completedPlanJson(plan, finalSuggestion, evidence, toolCalls, subtaskResults);
            agentRun.succeed(completedPlanJson, finalSuggestion, Instant.now());
            agentRunRepository.save(agentRun);
            ticketApplicationService.updateTicketStatus(ticket.getTicketId(), TicketStatus.RESOLVED, finalSuggestion);
            return new AgentRunResult(agentRun, intent, completedPlanJson, finalSuggestion, evidence, toolCalls);
        } catch (RuntimeException exception) {
            String failureMessage = failureMessage(exception);
            agentRun.fail(failureMessage, Instant.now());
            agentRunRepository.save(agentRun);
            return new AgentRunResult(agentRun, intent, failurePlan(intent, failureMessage), failureMessage,
                    List.of(), toolCalls);
        }
    }

    public AgentRun getAgentRun(String runId) {
        return agentRunRepository.findById(runId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "AGENT_RUN_NOT_FOUND",
                        "AgentRun not found: " + runId));
    }

    private AgentPlanningContext planningContext(Ticket ticket) {
        return new AgentPlanningContext(
                ticket.getTicketId(),
                ticket.getUserId(),
                ticket.getOrderId(),
                ticket.getRawUserMessage(),
                ticket.getStatus(),
                availableToolNames(),
                RISK_POLICY_SUMMARY,
                ticket.getCreatedAt());
    }

    private List<String> availableToolNames() {
        return toolRegistry.listDefinitions().stream()
                .map(ToolDefinition::toolName)
                .toList();
    }

    private void executePlannedTool(
            String runId,
            Ticket ticket,
            AgentPlan plan,
            PlannedToolCall plannedTool,
            List<String> evidence,
            List<String> toolCalls) {
        executePlannedTool(runId, ticket, plan, null, plannedTool, evidence, toolCalls);
    }

    private void executePlannedTool(
            String runId,
            Ticket ticket,
            AgentPlan plan,
            AgentSubtask subtask,
            PlannedToolCall plannedTool,
            List<String> evidence,
            List<String> toolCalls) {
        switch (plannedTool.toolName()) {
            case GET_ORDER_BY_ID_TOOL -> executeOrderLookup(runId, ticket, subtask, evidence, toolCalls);
            case SEARCH_POLICY_TOOL -> executePolicySearch(runId, plan, subtask, evidence, toolCalls);
            case ADD_TICKET_NOTE_TOOL -> executeTicketNote(runId, ticket, plan, subtask, evidence, toolCalls);
            default -> throw new IllegalArgumentException(
                    "AgentRun does not support executing planned tool: " + plannedTool.toolName());
        }
    }

    private List<SubtaskExecutionResult> executeSubtasks(
            String runId,
            Ticket ticket,
            AgentPlan plan,
            List<String> evidence,
            List<String> toolCalls) {
        List<SubtaskExecutionResult> results = new ArrayList<>();
        List<AgentSubtask> sortedSubtasks = plan.subtasks().stream()
                .sorted(Comparator.comparingInt(AgentSubtask::priority)
                        .thenComparing(AgentSubtask::subtaskId))
                .toList();
        for (AgentSubtask subtask : sortedSubtasks) {
            List<String> subtaskEvidence = new ArrayList<>();
            for (PlannedToolCall plannedTool : subtask.plannedTools()) {
                executePlannedTool(runId, ticket, plan, subtask, plannedTool, subtaskEvidence, toolCalls);
            }
            evidence.addAll(subtaskEvidence);
            AgentSubtask completedSubtask = subtask.withStatus(SubtaskStatus.SUCCEEDED);
            results.add(new SubtaskExecutionResult(
                    completedSubtask,
                    subtaskEvidence,
                    completedSubtask.subtaskId() + " " + completedSubtask.type().name()
                            + " succeeded with " + subtaskEvidence.size() + " evidence item(s)."));
        }
        return results;
    }

    private void executeOrderLookup(
            String runId,
            Ticket ticket,
            AgentSubtask subtask,
            List<String> evidence,
            List<String> toolCalls) {
        ToolOutput orderOutput = executeTool(runId, GET_ORDER_BY_ID_TOOL, tracedInput(Map.of(
                "orderId", ticket.getOrderId()), subtask));
        toolCalls.add(GET_ORDER_BY_ID_TOOL);
        ensureToolSucceeded(orderOutput);
        evidence.add(evidencePrefix(subtask) + orderEvidence(orderOutput));
    }

    private void executePolicySearch(
            String runId,
            AgentPlan plan,
            AgentSubtask subtask,
            List<String> evidence,
            List<String> toolCalls) {
        String policyQuery = subtask == null ? plan.policyQuery() : subtask.policyQuery();
        ToolOutput policyOutput = executeTool(runId, SEARCH_POLICY_TOOL, tracedInput(Map.of(
                "query", policyQuery), subtask));
        toolCalls.add(SEARCH_POLICY_TOOL);
        ensureToolSucceeded(policyOutput);
        evidence.addAll(extractEvidence(policyOutput).stream()
                .map(item -> evidencePrefix(subtask) + item)
                .toList());
    }

    private void executeTicketNote(
            String runId,
            Ticket ticket,
            AgentPlan plan,
            AgentSubtask subtask,
            List<String> evidence,
            List<String> toolCalls) {
        String note = subtask == null ? buildNoteToAdd(plan, evidence) : buildSubtaskNote(plan, subtask, evidence);
        ToolOutput noteOutput = executeTool(runId, ADD_TICKET_NOTE_TOOL, tracedInput(Map.of(
                "ticketId", ticket.getTicketId(),
                "note", note), subtask));
        toolCalls.add(ADD_TICKET_NOTE_TOOL);
        ensureToolSucceeded(noteOutput);
    }

    private static ToolInput tracedInput(Map<String, Object> arguments, AgentSubtask subtask) {
        if (subtask == null) {
            return ToolInput.of(arguments);
        }
        Map<String, Object> tracedArguments = new LinkedHashMap<>(arguments);
        tracedArguments.put("subtaskId", subtask.subtaskId());
        tracedArguments.put("subtaskType", subtask.type().name());
        tracedArguments.put("subtaskTarget", subtask.target());
        return ToolInput.of(tracedArguments);
    }

    private ToolOutput executeTool(String runId, String toolName, ToolInput input) {
        List<ToolOutput> outputs = new ArrayList<>();
        ToolTraceContext.runWith(runId, () -> outputs.add(toolRegistry.execute(toolName, input)));
        return outputs.get(0);
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

    private static String buildNoteToAdd(AgentPlan plan, List<String> evidence) {
        if (evidence.isEmpty()) {
            return plan.noteToAdd();
        }
        return plan.noteToAdd() + " Evidence: " + String.join("; ", evidence);
    }

    private static String buildSubtaskNote(AgentPlan plan, AgentSubtask subtask, List<String> evidence) {
        String note = "Subtask " + subtask.subtaskId()
                + " " + subtask.type().name()
                + " target=" + subtask.target()
                + ". " + plan.noteToAdd();
        if (evidence.isEmpty()) {
            return note;
        }
        return note + " Evidence: " + String.join("; ", evidence);
    }

    private static String buildFinalSuggestion(AgentPlan plan, List<String> evidence) {
        if (evidence.isEmpty()) {
            return plan.finalSuggestion();
        }
        return plan.finalSuggestion() + " Evidence: " + String.join("; ", evidence);
    }

    private static String buildMultiIntentFinalSuggestion(
            AgentPlan plan,
            List<SubtaskExecutionResult> subtaskResults,
            List<String> evidence) {
        String subtaskSummary = subtaskResults.stream()
                .map(SubtaskExecutionResult::summary)
                .toList()
                .stream()
                .reduce((left, right) -> left + " | " + right)
                .orElse("No subtasks executed.");
        if (evidence.isEmpty()) {
            return plan.finalSuggestion() + " Subtask summary: " + subtaskSummary;
        }
        return plan.finalSuggestion()
                + " Subtask summary: " + subtaskSummary
                + " Evidence: " + String.join("; ", evidence);
    }

    private static String evidencePrefix(AgentSubtask subtask) {
        if (subtask == null) {
            return "";
        }
        return "[" + subtask.subtaskId() + " " + subtask.type().name() + "] ";
    }

    private String completedPlanJson(
            AgentPlan plan,
            String finalSuggestion,
            List<String> evidence,
            List<String> toolCalls,
            List<SubtaskExecutionResult> subtaskResults) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("intent", plan.intent().name());
        value.put("riskLevel", plan.riskLevel().name());
        value.put("policyQuery", plan.policyQuery());
        value.put("noteToAdd", plan.noteToAdd());
        value.put("finalSuggestion", finalSuggestion);
        value.put("evidenceHints", plan.evidenceHints());
        value.put("plannedTools", plan.plannedTools());
        value.put("subtasks", plan.subtasks());
        value.put("completedSubtasks", subtaskResults.stream()
                .map(SubtaskExecutionResult::subtask)
                .toList());
        value.put("subtaskSummaries", subtaskResults.stream()
                .map(SubtaskExecutionResult::summary)
                .toList());
        value.put("evidence", evidence);
        value.put("toolCalls", toolCalls);
        return toJson(value);
    }

    private String failurePlan(IntentType intent, String failureMessage) {
        return toJson(Map.of(
                "intent", intent.name(),
                "riskLevel", "UNKNOWN",
                "plan", List.of(),
                "finalSuggestion", failureMessage,
                "evidence", List.of(),
                "toolCalls", List.of()));
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize Agent plan", exception);
        }
    }

    private static String failureMessage(RuntimeException exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return exception.getClass().getSimpleName();
        }
        return message;
    }

    private record SubtaskExecutionResult(AgentSubtask subtask, List<String> evidence, String summary) {
    }
}
