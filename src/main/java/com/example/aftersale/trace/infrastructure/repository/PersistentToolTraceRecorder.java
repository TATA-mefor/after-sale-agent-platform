package com.example.aftersale.trace.infrastructure.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.aftersale.tool.application.ToolTraceRecord;
import com.example.aftersale.tool.application.ToolTraceRecorder;
import com.example.aftersale.tool.domain.ToolExecutionStatus;
import com.example.aftersale.trace.domain.ToolCallTrace;
import com.example.aftersale.trace.domain.ToolCallTraceRepository;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.UUID;
import org.springframework.stereotype.Component;

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
