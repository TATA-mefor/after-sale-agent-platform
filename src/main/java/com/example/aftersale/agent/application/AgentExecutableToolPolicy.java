package com.example.aftersale.agent.application;

import com.example.aftersale.tool.application.ToolRegistry;
import com.example.aftersale.tool.domain.ToolDefinition;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Defines the tool boundary for one AgentRun.
 *
 * <p>ToolRegistry may contain tools for APIs or future skills that an AgentRun cannot safely map today. Planner
 * context, plan validation, and specialist handlers must share this policy so the LLM only sees tools the current
 * AgentRun execution path can actually run.
 */
@Component
public class AgentExecutableToolPolicy {

    public static final String GET_ORDER_BY_ID_TOOL = "get_order_by_id";
    public static final String SEARCH_POLICY_TOOL = "search_aftersale_policy";
    public static final String ADD_TICKET_NOTE_TOOL = "add_ticket_note";

    private static final List<String> AGENT_RUN_TOOL_ORDER = List.of(
            GET_ORDER_BY_ID_TOOL,
            SEARCH_POLICY_TOOL,
            ADD_TICKET_NOTE_TOOL);

    private final ToolRegistry toolRegistry;

    public AgentExecutableToolPolicy(ToolRegistry toolRegistry) {
        this.toolRegistry = toolRegistry;
    }

    public List<String> allowedToolNames() {
        Set<String> registeredToolNames = toolRegistry.listDefinitions().stream()
                .map(ToolDefinition::toolName)
                .collect(Collectors.toUnmodifiableSet());
        return AGENT_RUN_TOOL_ORDER.stream()
                .filter(registeredToolNames::contains)
                .toList();
    }
}
