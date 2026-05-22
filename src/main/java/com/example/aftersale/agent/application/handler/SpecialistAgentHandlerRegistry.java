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

/**
 * 按 SubtaskType 为 Agent 子任务选择唯一的专业 Handler。
校验通过后，系统按子任务类型找 handler：
return 类子任务 → ReturnAgentHandler
exchange 类子任务 → ExchangeAgentHandler
Registry 只负责“找谁处理”，不做业务执行本身。
 * <p>边界：注册表只负责路由和 unsupported fallback，不执行业务工具，也不补偿缺失的风险策略。
 */
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

    /**
     * 将子任务交给匹配的 Handler；没有匹配项时返回结构化失败并写入 Workspace。
     */
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
