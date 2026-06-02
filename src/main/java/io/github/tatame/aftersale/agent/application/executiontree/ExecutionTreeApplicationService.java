package io.github.tatame.aftersale.agent.application.executiontree;

import io.github.tatame.aftersale.agent.application.AgentApplicationService;
import io.github.tatame.aftersale.agent.domain.AgentRun;
import io.github.tatame.aftersale.approval.application.ApprovalApplicationService;
import io.github.tatame.aftersale.approval.domain.ApprovalRequest;
import io.github.tatame.aftersale.common.observability.MdcScope;
import io.github.tatame.aftersale.common.observability.ObservabilityConstants;
import io.github.tatame.aftersale.trace.application.ToolCallTraceApplicationService;
import io.github.tatame.aftersale.trace.domain.ToolCallTrace;
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
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

/**
 * 基于 AgentRun、ToolCallTrace 和 ApprovalRequest 构建只读执行树。
 *
 * <p>边界：本服务只为查看而重建执行过程，不执行工具、不修改 Ticket 状态，也不把 planJson 中的
 * Workspace 快照当作权威审计数据。
 */
@Service
public class ExecutionTreeApplicationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExecutionTreeApplicationService.class);

    private static final String SUBTASK_ID_FIELD = "subtaskId";
    private static final String SEARCH_POLICY_TOOL = "search_aftersale_policy";

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

    /**
     * 返回一次 AgentRun 的执行树，包括子任务、工具 trace、审批和错误。
     *
     * <p>没有 subtaskId 的 trace 会挂在根节点，确保旧数据或根级工具调用不会被丢弃。
     */
    public ExecutionTreeResponse getExecutionTree(String runId) {
        AgentRun agentRun = agentApplicationService.getAgentRun(runId);
        try (MdcScope ignored = MdcScope.putAll(Map.of(
                ObservabilityConstants.AGENT_RUN_ID, agentRun.getRunId(),
                ObservabilityConstants.TICKET_ID, agentRun.getTicketId()))) {
            LOGGER.info("execution_tree.queried started status={}", agentRun.getStatus());
            List<String> errors = new ArrayList<>();
            JsonNode planRoot = parsePlan(agentRun, errors);
            Map<String, MutableSubtaskNode> subtaskNodes = subtaskNodes(planRoot);
            List<ExecutionTreePolicyEvidenceNode> rootPolicyEvidence = new ArrayList<>();
            List<ExecutionTreeToolCallNode> rootToolCalls = attachToolCalls(
                    agentRun,
                    subtaskNodes,
                    rootPolicyEvidence,
                    errors);
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
                    rootPolicyEvidence,
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
            List<ExecutionTreePolicyEvidenceNode> rootPolicyEvidence,
            List<String> errors) {
        List<ExecutionTreeToolCallNode> rootToolCalls = new ArrayList<>();
        for (ToolCallTrace trace : traceApplicationService.findByRunId(agentRun.getRunId())) {
            ExecutionTreeToolCallNode node = toolCallNode(trace);
            if (trace.getErrorMessage() != null && !trace.getErrorMessage().isBlank()) {
                errors.add(trace.getToolName() + ": " + trace.getErrorMessage());
            }
            String subtaskId = subtaskIdFromTrace(trace, errors);
            MutableSubtaskNode subtaskNode = subtaskNodes.get(subtaskId);
            List<ExecutionTreePolicyEvidenceNode> evidenceNodes = policyEvidenceNodes(trace, subtaskId, errors);
            if (subtaskNode == null) {
                // 旧 trace 和根级 fallback 计划不会在 inputJson 中携带 subtaskId。
                rootToolCalls.add(node);
                rootPolicyEvidence.addAll(evidenceNodes);
            } else {
                subtaskNode.addToolCall(node);
                evidenceNodes.forEach(subtaskNode::addPolicyEvidence);
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

    private List<ExecutionTreePolicyEvidenceNode> policyEvidenceNodes(
            ToolCallTrace trace,
            String subtaskId,
            List<String> errors) {
        if (!SEARCH_POLICY_TOOL.equals(trace.getToolName())
                || trace.getOutputJson() == null
                || trace.getOutputJson().isBlank()) {
            return List.of();
        }
        try {
            JsonNode outputRoot = objectMapper.readTree(trace.getOutputJson());
            JsonNode data = outputRoot.has("data") ? outputRoot.path("data") : outputRoot;
            JsonNode evidences = data.path("evidences");
            if (!evidences.isArray()) {
                evidences = data.path("results");
            }
            if (!evidences.isArray()) {
                return List.of();
            }
            List<ExecutionTreePolicyEvidenceNode> nodes = new ArrayList<>();
            for (JsonNode evidence : evidences) {
                String snippet = firstText(evidence, "snippet", "matchedText");
                String category = text(evidence, "category");
                if (!snippet.isBlank() && !category.isBlank()) {
                    nodes.add(new ExecutionTreePolicyEvidenceNode(
                            text(evidence, "evidenceId"),
                            text(evidence, "policyId"),
                            text(evidence, "documentId"),
                            text(evidence, "chunkId"),
                            text(evidence, "documentTitle"),
                            category,
                            text(evidence, "productType"),
                            snippet,
                            nullableDouble(evidence, "score"),
                            text(evidence, "retrievalMode"),
                            text(evidence, "source"),
                            subtaskId,
                            trace.getTraceId()));
                }
            }
            return List.copyOf(nodes);
        } catch (RuntimeException | JsonProcessingException exception) {
            errors.add("Failed to parse policy evidence from trace " + trace.getTraceId()
                    + ": " + exception.getMessage());
            return List.of();
        }
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

    private static String firstText(JsonNode node, String firstFieldName, String secondFieldName) {
        String first = text(node, firstFieldName);
        if (!first.isBlank()) {
            return first;
        }
        return text(node, secondFieldName);
    }

    @Nullable
    private static Double nullableDouble(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        if (value.isNumber()) {
            return value.asDouble();
        }
        if (value.isTextual() && !value.asText().isBlank()) {
            try {
                return Double.parseDouble(value.asText());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
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
        private final List<ExecutionTreePolicyEvidenceNode> policyEvidence = new ArrayList<>();
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

        private void addPolicyEvidence(ExecutionTreePolicyEvidenceNode evidence) {
            policyEvidence.add(evidence);
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
                    policyEvidence,
                    approvalRequests);
        }
    }
}
