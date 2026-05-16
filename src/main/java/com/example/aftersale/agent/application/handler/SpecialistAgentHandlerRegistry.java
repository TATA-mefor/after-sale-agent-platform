package com.example.aftersale.agent.application.handler;

import com.example.aftersale.agent.application.planner.SubtaskStatus;
import com.example.aftersale.agent.application.planner.SubtaskType;
import com.example.aftersale.agent.application.workspace.SubtaskMemory;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class SpecialistAgentHandlerRegistry {

    private final Map<SubtaskType, SpecialistAgentHandler> handlers;

    @SuppressFBWarnings(
            value = "CT_CONSTRUCTOR_THROW",
            justification = "The registry rejects duplicate handler coverage during Spring bean construction.")
    public SpecialistAgentHandlerRegistry(List<SpecialistAgentHandler> handlerList) {
        this.handlers = Map.copyOf(indexBySupportedType(handlerList));
    }

    public Optional<SpecialistAgentHandler> findHandler(SubtaskType type) {
        return Optional.ofNullable(handlers.get(type));
    }

    public SubtaskExecutionResult handle(SubtaskExecutionContext context) {
        return findHandler(context.subtask().type())
                .map(handler -> handler.handle(context))
                .orElseGet(() -> unsupportedSubtask(context));
    }

    public Set<SubtaskType> supportedTypes() {
        return handlers.keySet();
    }

    private static Map<SubtaskType, SpecialistAgentHandler> indexBySupportedType(
            List<SpecialistAgentHandler> handlerList) {
        Map<SubtaskType, SpecialistAgentHandler> indexedHandlers = new EnumMap<>(SubtaskType.class);
        for (SpecialistAgentHandler handler : handlerList) {
            SubtaskType supportedType = handler.supportedType();
            SpecialistAgentHandler previous = indexedHandlers.putIfAbsent(supportedType, handler);
            if (previous != null) {
                throw new IllegalStateException("Duplicate specialist handler for subtask type: " + supportedType);
            }
        }
        return indexedHandlers;
    }

    private static SubtaskExecutionResult unsupportedSubtask(SubtaskExecutionContext context) {
        String errorMessage = "No specialist handler registered for subtask type: "
                + context.subtask().type().name();
        context.workspace().addSubtaskMemory(new SubtaskMemory(
                context.subtask().subtaskId(),
                context.subtask().type(),
                context.subtask().target(),
                SubtaskStatus.FAILED,
                errorMessage,
                List.of()));
        return SubtaskExecutionResult.failed(
                context.subtask().subtaskId(),
                context.subtask().type(),
                errorMessage);
    }
}
