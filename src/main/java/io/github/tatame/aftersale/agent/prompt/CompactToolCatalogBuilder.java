package io.github.tatame.aftersale.agent.prompt;

import io.github.tatame.aftersale.tool.domain.ToolRiskLevel;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;

public class CompactToolCatalogBuilder {

    private static final Map<String, ToolCatalogEntry> CATALOG = Map.of(
            "get_order_by_id", new ToolCatalogEntry(
                    "get_order_by_id",
                    ToolRiskLevel.LOW,
                    List.of("orderId"),
                    "Fetch order facts and item details"),
            "get_user_orders", new ToolCatalogEntry(
                    "get_user_orders",
                    ToolRiskLevel.LOW,
                    List.of("userId"),
                    "Fetch recent user orders"),
            "search_aftersale_policy", new ToolCatalogEntry(
                    "search_aftersale_policy",
                    ToolRiskLevel.LOW,
                    List.of("query"),
                    "Search controlled after-sale policy snippets"),
            "add_ticket_note", new ToolCatalogEntry(
                    "add_ticket_note",
                    ToolRiskLevel.LOW,
                    List.of("ticketId", "note"),
                    "Add a safe internal note to a ticket"),
            "create_aftersale_ticket", new ToolCatalogEntry(
                    "create_aftersale_ticket",
                    ToolRiskLevel.LOW,
                    List.of("userId", "orderId", "message"),
                    "Create an after-sale ticket"),
            "update_ticket_status", new ToolCatalogEntry(
                    "update_ticket_status",
                    ToolRiskLevel.MEDIUM,
                    List.of("ticketId", "status", "reason"),
                    "Request a safe ticket status transition"));

    private final ObjectMapper objectMapper;

    @SuppressFBWarnings(
            value = "EI_EXPOSE_REP2",
            justification = "ObjectMapper is an application-wide JSON collaborator injected by Spring.")
    public CompactToolCatalogBuilder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String build(List<String> availableTools) {
        List<ToolCatalogEntry> entries = availableTools.stream()
                .map(CompactToolCatalogBuilder::entryFor)
                .toList();
        try {
            return objectMapper.writeValueAsString(entries);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize compact tool catalog", exception);
        }
    }

    private static ToolCatalogEntry entryFor(String toolName) {
        return CATALOG.getOrDefault(toolName, new ToolCatalogEntry(
                toolName,
                ToolRiskLevel.LOW,
                List.of(),
                "Registered tool available to planner"));
    }

    private record ToolCatalogEntry(
            String name,
            ToolRiskLevel risk,
            List<String> requiredInputFields,
            String purpose) {
    }
}
