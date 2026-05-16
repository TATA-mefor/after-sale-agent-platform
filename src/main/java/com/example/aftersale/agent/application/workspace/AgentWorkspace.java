package com.example.aftersale.agent.application.workspace;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class AgentWorkspace {

    private final String agentRunId;
    private final String ticketId;
    private final Instant createdAt;
    private final List<OrderFact> orderFacts = new ArrayList<>();
    private final List<PolicyEvidence> policyEvidence = new ArrayList<>();
    private final List<SubtaskMemory> subtaskMemories = new ArrayList<>();
    private final List<ToolResultSummary> toolResultSummaries = new ArrayList<>();
    private final List<RiskFlag> riskFlags = new ArrayList<>();
    private Instant updatedAt;

    private AgentWorkspace(String agentRunId, String ticketId, Instant createdAt) {
        this.agentRunId = requireText(agentRunId, "agentRunId");
        this.ticketId = requireText(ticketId, "ticketId");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.updatedAt = createdAt;
    }

    public static AgentWorkspace start(String agentRunId, String ticketId, Instant createdAt) {
        return new AgentWorkspace(agentRunId, ticketId, createdAt);
    }

    public String agentRunId() {
        return agentRunId;
    }

    public String ticketId() {
        return ticketId;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public void addOrderFact(OrderFact orderFact) {
        orderFacts.add(Objects.requireNonNull(orderFact, "orderFact must not be null"));
        touch();
    }

    public void addPolicyEvidence(List<PolicyEvidence> evidence) {
        policyEvidence.addAll(List.copyOf(Objects.requireNonNull(evidence, "evidence must not be null")));
        touch();
    }

    public void addSubtaskMemory(SubtaskMemory subtaskMemory) {
        subtaskMemories.add(Objects.requireNonNull(subtaskMemory, "subtaskMemory must not be null"));
        touch();
    }

    public void addToolResultSummary(ToolResultSummary toolResultSummary) {
        toolResultSummaries.add(Objects.requireNonNull(toolResultSummary, "toolResultSummary must not be null"));
        touch();
    }

    public void addRiskFlag(RiskFlag riskFlag) {
        riskFlags.add(Objects.requireNonNull(riskFlag, "riskFlag must not be null"));
        touch();
    }

    public List<OrderFact> orderFacts() {
        return List.copyOf(orderFacts);
    }

    public List<PolicyEvidence> policyEvidence() {
        return List.copyOf(policyEvidence);
    }

    public List<SubtaskMemory> subtaskMemories() {
        return List.copyOf(subtaskMemories);
    }

    public List<ToolResultSummary> toolResultSummaries() {
        return List.copyOf(toolResultSummaries);
    }

    public List<RiskFlag> riskFlags() {
        return List.copyOf(riskFlags);
    }

    public List<String> evidenceLines() {
        List<String> evidenceLines = new ArrayList<>();
        orderFacts.stream()
                .map(OrderFact::summary)
                .forEach(evidenceLines::add);
        policyEvidence.stream()
                .map(PolicyEvidence::summary)
                .forEach(evidenceLines::add);
        return List.copyOf(evidenceLines);
    }

    public String summary() {
        List<String> parts = new ArrayList<>();
        if (!orderFacts.isEmpty()) {
            parts.add("Order facts: " + joinSummaries(orderFacts.stream()
                    .map(OrderFact::summary)
                    .toList()));
        }
        if (!policyEvidence.isEmpty()) {
            parts.add("Policy evidence: " + joinSummaries(policyEvidence.stream()
                    .map(PolicyEvidence::summary)
                    .toList()));
        }
        if (!subtaskMemories.isEmpty()) {
            parts.add("Subtasks: " + joinSummaries(subtaskMemories.stream()
                    .map(SubtaskMemory::summary)
                    .toList()));
        }
        if (!riskFlags.isEmpty()) {
            parts.add("Risk flags: " + joinSummaries(riskFlags.stream()
                    .map(RiskFlag::summary)
                    .toList()));
        }
        if (parts.isEmpty()) {
            return "Workspace has no structured evidence.";
        }
        return String.join(" ", parts);
    }

    public Map<String, Object> toSnapshot() {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("agentRunId", agentRunId);
        snapshot.put("ticketId", ticketId);
        snapshot.put("orderFacts", orderFacts());
        snapshot.put("policyEvidence", policyEvidence());
        snapshot.put("subtaskMemories", subtaskMemories());
        snapshot.put("toolResultSummaries", toolResultSummaries());
        snapshot.put("riskFlags", riskFlags());
        snapshot.put("createdAt", createdAt);
        snapshot.put("updatedAt", updatedAt);
        return snapshot;
    }

    private void touch() {
        updatedAt = Instant.now();
    }

    private static String joinSummaries(List<String> values) {
        return String.join("; ", values);
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
