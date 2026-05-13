package com.example.aftersale.agent.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import com.example.aftersale.tool.domain.ToolInput;
import com.example.aftersale.tool.domain.ToolOutput;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class AgentApplicationService {

    private final AgentRunRepository agentRunRepository;
    private final TicketApplicationService ticketApplicationService;
    private final ToolRegistry toolRegistry;
    private final ObjectMapper objectMapper;

    @SuppressFBWarnings(
            value = "EI_EXPOSE_REP2",
            justification = "Spring constructor injection intentionally stores application collaborators.")
    public AgentApplicationService(
            AgentRunRepository agentRunRepository,
            TicketApplicationService ticketApplicationService,
            ToolRegistry toolRegistry,
            ObjectMapper objectMapper) {
        this.agentRunRepository = agentRunRepository;
        this.ticketApplicationService = ticketApplicationService;
        this.toolRegistry = toolRegistry;
        this.objectMapper = objectMapper;
    }

    public AgentRunResult runForTicket(String ticketId) {
        Ticket ticket = ticketApplicationService.getTicket(ticketId);
        AgentRun agentRun = AgentRun.start("RUN-" + UUID.randomUUID(), ticket.getTicketId(), Instant.now());
        agentRunRepository.save(agentRun);

        IntentType intent = classifyIntent(ticket.getRawUserMessage());
        List<String> toolCalls = new ArrayList<>();
        try {
            ticketApplicationService.classifyTicketIntent(ticket.getTicketId(), intent);
            ticketApplicationService.updateTicketStatus(ticket.getTicketId(), TicketStatus.AGENT_RUNNING, null);

            ToolOutput policyOutput = executeTool(agentRun.getRunId(), "search_aftersale_policy", ToolInput.of(Map.of(
                    "query", ticket.getRawUserMessage())));
            toolCalls.add("search_aftersale_policy");
            ensureToolSucceeded(policyOutput);

            List<String> evidence = extractEvidence(policyOutput);
            String finalSuggestion = buildFinalSuggestion(intent, evidence);

            ToolOutput noteOutput = executeTool(agentRun.getRunId(), "add_ticket_note", ToolInput.of(Map.of(
                    "ticketId", ticket.getTicketId(),
                    "note", finalSuggestion)));
            toolCalls.add("add_ticket_note");
            ensureToolSucceeded(noteOutput);

            String completedPlanJson = toJson(Map.of(
                    "intent", intent.name(),
                    "plan", List.of(
                            Map.of("step", 1, "action", "search_aftersale_policy",
                                    "reason", "Retrieve after-sale policy evidence."),
                            Map.of("step", 2, "action", "add_ticket_note",
                                    "reason", "Persist the rule-based Agent suggestion on the ticket.")),
                    "finalSuggestion", finalSuggestion,
                    "evidence", evidence,
                    "toolCalls", toolCalls));
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

    private static IntentType classifyIntent(String message) {
        String normalized = message.toLowerCase(Locale.ROOT);
        if (normalized.contains("物流") || normalized.contains("没收到") || normalized.contains("未收到")) {
            return IntentType.LOGISTICS_ISSUE;
        }
        if (normalized.contains("换货") || normalized.contains("换大") || normalized.contains("尺码")) {
            return IntentType.EXCHANGE;
        }
        if (normalized.contains("维修") || normalized.contains("修")) {
            return IntentType.REPAIR;
        }
        if (normalized.contains("退货") || normalized.contains("退款")) {
            return IntentType.RETURN_AND_REFUND;
        }
        return IntentType.UNKNOWN;
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

    private static String buildFinalSuggestion(IntentType intent, List<String> evidence) {
        return "Intent " + intent.name() + " identified. Suggested handling is based on evidence: "
                + String.join("; ", evidence);
    }

    private String failurePlan(IntentType intent, String failureMessage) {
        return toJson(Map.of(
                "intent", intent.name(),
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
}
