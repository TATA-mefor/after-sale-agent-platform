package com.example.aftersale.agent.application.executiontree;

import com.example.aftersale.agent.application.AgentApplicationService;
import com.example.aftersale.agent.domain.AgentRun;
import com.example.aftersale.approval.application.ApprovalApplicationService;
import com.example.aftersale.approval.domain.ApprovalRequest;
import com.example.aftersale.common.observability.MdcScope;
import com.example.aftersale.common.observability.ObservabilityConstants;
import com.example.aftersale.trace.application.ToolCallTraceApplicationService;
import com.example.aftersale.trace.domain.ToolCallTrace;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ExecutionTreeApplicationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExecutionTreeApplicationService.class);

    private static final String SUBTASK_ID_FIELD = "subtaskId";

    private final AgentApplicationService agentApplicationService;
    private final ToolCallTraceApplicationService traceApplicationService;
    private final ApprovalApplicationService approvalApplicationService;
    private final ObjectMapper objectMapper;

    @SuppressFBWarnings(
            value = "EI_EXPOSE_REP2",
            justification = "Spring constructor injection intentionally stores application collaborators.")
    public ExecutionTreeApplicationService(
            AgentApplicationService agentApplicationService,
            ToolCallTraceApplicationService traceApplicationService,
            ApprovalApplicationService approvalApplicationService,
            ObjectMapper objectMapper) {
        this.agentApplicationService = agentApplicationService;
        this.traceApplicationService = traceApplicationService;
        this.approvalApplicationService = approvalApplicationService;
        this.objectMapper = objectMapper;
    }

    public ExecutionTreeResponse getExecutionTree(String runId) {
        AgentRun agentRun = agentApplicationService.getAgentRun(runId);
        try (MdcScope ignored = MdcScope.putAll(Map.of(
                ObservabilityConstants.AGENT_RUN_ID, agentRun.getRunId(),
                ObservabilityConstants.TICKET_ID, agentRun.getTicketId()))) {
            LOGGER.info("execution_tree.queried started status={}", agentRun.getStatus());
            List<String> errors = new ArrayList<>();
            JsonNode planRoot = parsePlan(agentRun, errors);
            Map<String, MutableSubtaskNode> subtaskNodes = subtaskNodes(planRoot);
            List<ExecutionTreeToolCallNode> rootToolCalls = attachToolCalls(agentRun, subtaskNodes, errors);
            List<ExecutionTreeApprovalNode> rootApprovalRequests = attachApprovalRequests(agentRun, subtaskNodes);
            addRunError(agentRun, errors);

            ExecutionTreeResponse response = new ExecutionTreeResponse(
                    agentRun.getRunId(),
                    agentRun.getTicketId(),
                    agentRun.getStatus(),
                    agentRun.getFinalAnswer(),
                    rootSummary(agentRun, planRoot),
                    subtaskNodes.values().stream()
                            .map(MutableSubtaskNode::toResponse)
                            .toList(),
                    rootToolCalls,
                    rootApprovalRequests,
                    errors,
                    agentRun.getStartedAt(),
                    agentRun.getFinishedAt());
            LOGGER.info("""
                    execution_tree.queried completed subtaskCount={} rootToolCallCount={} rootApprovalCount={} \
                    errorCount={}\
                    """,
                    response.subtasks().size(),
                    response.toolCalls().size(),
                    response.approvalRequests().size(),
                    response.errors().size());
            return response;
        }
    }

    private JsonNode parsePlan(AgentRun agentRun, List<String> errors) {
        String planJson = agentRun.getPlanJson();
        if (planJson == null || planJson.isBlank()) {
            return objectMapper.createObjectNode();
        }
        try {
            return objectMapper.readTree(planJson);
        } catch (JsonProcessingException exception) {
            errors.add("Failed to parse AgentRun planJson: " + exception.getOriginalMessage());
            return objectMapper.createObjectNode();
        }
    }

    private Map<String, MutableSubtaskNode> subtaskNodes(JsonNode planRoot) {
        Map<String, MutableSubtaskNode> nodes = new LinkedHashMap<>();
        JsonNode plannedSubtasks = planRoot.path("subtasks");
        if (plannedSubtasks.isArray()) {
            for (JsonNode subtask : plannedSubtasks) {
                String subtaskId = text(subtask, SUBTASK_ID_FIELD);
                if (!subtaskId.isBlank()) {
                    nodes.put(subtaskId, MutableSubtaskNode.fromPlannedSubtask(subtask));
                }
            }
        }

        JsonNode completedSubtasks = planRoot.path("completedSubtasks");
        if (completedSubtasks.isArray()) {
            for (JsonNode completedSubtask : completedSubtasks) {
                String subtaskId = text(completedSubtask, SUBTASK_ID_FIELD);
                if (!subtaskId.isBlank()) {
                    nodes.computeIfAbsent(
                                    subtaskId,
                                    ignored -> MutableSubtaskNode.fromCompletedSubtask(completedSubtask))
                            .applyCompletedSubtask(completedSubtask);
                }
            }
        }
        return nodes;
    }

    private List<ExecutionTreeToolCallNode> attachToolCalls(
            AgentRun agentRun,
            Map<String, MutableSubtaskNode> subtaskNodes,
            List<String> errors) {
        List<ExecutionTreeToolCallNode> rootToolCalls = new ArrayList<>();
        for (ToolCallTrace trace : traceApplicationService.findByRunId(agentRun.getRunId())) {
            ExecutionTreeToolCallNode node = toolCallNode(trace);
            if (trace.getErrorMessage() != null && !trace.getErrorMessage().isBlank()) {
                errors.add(trace.getToolName() + ": " + trace.getErrorMessage());
            }
            String subtaskId = subtaskIdFromTrace(trace, errors);
            MutableSubtaskNode subtaskNode = subtaskNodes.get(subtaskId);
            if (subtaskNode == null) {
                rootToolCalls.add(node);
            } else {
                subtaskNode.addToolCall(node);
            }
        }
        return List.copyOf(rootToolCalls);
    }

    private List<ExecutionTreeApprovalNode> attachApprovalRequests(
            AgentRun agentRun,
            Map<String, MutableSubtaskNode> subtaskNodes) {
        List<ExecutionTreeApprovalNode> rootApprovalRequests = new ArrayList<>();
        for (ApprovalRequest request : approvalApplicationService.findByRunId(agentRun.getRunId())) {
            ExecutionTreeApprovalNode node = approvalNode(request);
            MutableSubtaskNode subtaskNode = subtaskNodes.get(request.getSubtaskId());
            if (subtaskNode == null) {
                rootApprovalRequests.add(node);
            } else {
                subtaskNode.addApprovalRequest(node);
            }
        }
        return List.copyOf(rootApprovalRequests);
    }

    private String subtaskIdFromTrace(ToolCallTrace trace, List<String> errors) {
        try {
            return text(objectMapper.readTree(trace.getInputJson()), SUBTASK_ID_FIELD);
        } catch (JsonProcessingException exception) {
            errors.add("Failed to parse trace inputJson for " + trace.getTraceId()
                    + ": " + exception.getOriginalMessage());
            return "";
        }
    }

    private static ExecutionTreeToolCallNode toolCallNode(ToolCallTrace trace) {
        return new ExecutionTreeToolCallNode(
                trace.getTraceId(),
                trace.getToolName(),
                trace.getStatus(),
                trace.getLatencyMs(),
                trace.getInputJson(),
                trace.getOutputJson(),
                trace.getErrorMessage(),
                trace.getCreatedAt());
    }

    private static ExecutionTreeApprovalNode approvalNode(ApprovalRequest request) {
        return new ExecutionTreeApprovalNode(
                request.getApprovalId(),
                request.getStatus(),
                request.getRequestedAction(),
                request.getDecisionReason(),
                request.getRequestedAt(),
                request.getReviewedAt());
    }

    private static void addRunError(AgentRun agentRun, List<String> errors) {
        String errorMessage = agentRun.getErrorMessage();
        if (errorMessage != null && !errorMessage.isBlank()) {
            errors.add(errorMessage);
        }
    }

    private static String rootSummary(AgentRun agentRun, JsonNode planRoot) {
        List<String> parts = new ArrayList<>();
        if (agentRun.getFinalAnswer() != null && !agentRun.getFinalAnswer().isBlank()) {
            parts.add(agentRun.getFinalAnswer());
        }
        JsonNode workspace = planRoot.path("workspace");
        if (!workspace.isMissingNode()) {
            parts.add("Workspace counts: orderFacts=" + workspace.path("orderFacts").size()
                    + ", policyEvidence=" + workspace.path("policyEvidence").size()
                    + ", subtaskMemories=" + workspace.path("subtaskMemories").size()
                    + ", toolResultSummaries=" + workspace.path("toolResultSummaries").size()
                    + ", riskFlags=" + workspace.path("riskFlags").size());
        }
        if (parts.isEmpty()) {
            return "AgentRun " + agentRun.getRunId() + " is " + agentRun.getStatus().name();
        }
        return String.join(" ", parts);
    }

    private static String text(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        if (value.isMissingNode() || value.isNull()) {
            return "";
        }
        return value.asText("");
    }

    private static int integer(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        if (value.isMissingNode() || value.isNull()) {
            return 0;
        }
        return value.asInt(0);
    }

    private static final class MutableSubtaskNode {

        private final String subtaskId;
        private String type;
        private String target;
        private int priority;
        private String riskLevel;
        private String status;
        private String summary;
        private final List<ExecutionTreeToolCallNode> toolCalls = new ArrayList<>();
        private final List<ExecutionTreeApprovalNode> approvalRequests = new ArrayList<>();

        private MutableSubtaskNode(
                String subtaskId,
                String type,
                String target,
                int priority,
                String riskLevel,
                String status,
                String summary) {
            this.subtaskId = subtaskId;
            this.type = type;
            this.target = target;
            this.priority = priority;
            this.riskLevel = riskLevel;
            this.status = status;
            this.summary = summary;
        }

        private static MutableSubtaskNode fromPlannedSubtask(JsonNode subtask) {
            return new MutableSubtaskNode(
                    text(subtask, SUBTASK_ID_FIELD),
                    text(subtask, "type"),
                    text(subtask, "target"),
                    integer(subtask, "priority"),
                    text(subtask, "riskLevel"),
                    text(subtask, "status"),
                    "");
        }

        private static MutableSubtaskNode fromCompletedSubtask(JsonNode completedSubtask) {
            return new MutableSubtaskNode(
                    text(completedSubtask, SUBTASK_ID_FIELD),
                    text(completedSubtask, "type"),
                    "",
                    0,
                    "",
                    text(completedSubtask, "status"),
                    text(completedSubtask, "summary"));
        }

        private void applyCompletedSubtask(JsonNode completedSubtask) {
            if (!text(completedSubtask, "type").isBlank()) {
                type = text(completedSubtask, "type");
            }
            if (!text(completedSubtask, "status").isBlank()) {
                status = text(completedSubtask, "status");
            }
            if (!text(completedSubtask, "summary").isBlank()) {
                summary = text(completedSubtask, "summary");
            }
        }

        private void addToolCall(ExecutionTreeToolCallNode toolCall) {
            toolCalls.add(toolCall);
        }

        private void addApprovalRequest(ExecutionTreeApprovalNode approvalRequest) {
            approvalRequests.add(approvalRequest);
        }

        private ExecutionTreeSubtaskNode toResponse() {
            return new ExecutionTreeSubtaskNode(
                    subtaskId,
                    type,
                    target,
                    priority,
                    riskLevel,
                    status,
                    summary,
                    toolCalls,
                    approvalRequests);
        }
    }
}
