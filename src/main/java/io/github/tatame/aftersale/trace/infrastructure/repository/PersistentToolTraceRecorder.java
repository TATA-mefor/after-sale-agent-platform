package io.github.tatame.aftersale.trace.infrastructure.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.tatame.aftersale.tool.application.ToolTraceRecord;
import io.github.tatame.aftersale.tool.application.ToolTraceRecorder;
import io.github.tatame.aftersale.tool.domain.ToolExecutionStatus;
import io.github.tatame.aftersale.trace.domain.ToolCallTrace;
import io.github.tatame.aftersale.trace.domain.ToolCallTraceRepository;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * 将 ToolRegistry 调用元数据持久化为 ToolCallTrace 记录。
 *
 * <p>边界：trace 记录用于审计和执行树重建，但不是业务状态来源，不能替代所属应用服务或 Repository。
 */
@Component
public class PersistentToolTraceRecorder implements ToolTraceRecorder {

    private final ToolCallTraceRepository toolCallTraceRepository;
    private final ObjectMapper objectMapper;

    @SuppressFBWarnings(
            value = "EI_EXPOSE_REP2",
            justification = "Spring constructor injection intentionally stores trace persistence collaborators.")
    public PersistentToolTraceRecorder(ToolCallTraceRepository toolCallTraceRepository, ObjectMapper objectMapper) {
        this.toolCallTraceRepository = toolCallTraceRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * 记录一次工具调用结果，并保留成功、失败或需要审批的结构化状态。
     */
    @Override
    public void record(ToolTraceRecord record) {
        ToolCallTrace trace = ToolCallTrace.start(
                "TRACE-" + UUID.randomUUID(),
                record.runId(),
                record.toolName(),
                toJson(record.input().arguments()),
                record.recordedAt());
        if (record.output().status() == ToolExecutionStatus.SUCCEEDED) {
            trace.markSucceeded(toJson(record.output().data()), record.latencyMs());
        } else if (record.output().status() == ToolExecutionStatus.REQUIRES_APPROVAL) {
            trace.markRequiresApproval(toJson(record.output().data()), record.latencyMs());
        } else {
            trace.markFailed(record.output().message(), record.latencyMs());
        }
        toolCallTraceRepository.save(trace);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize tool trace payload", exception);
        }
    }
}
