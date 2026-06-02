package io.github.tatame.aftersale.trace.application;

import io.github.tatame.aftersale.trace.domain.ToolCallTrace;
import io.github.tatame.aftersale.trace.domain.ToolCallTraceRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ToolCallTraceApplicationService {

    private final ToolCallTraceRepository toolCallTraceRepository;

    public ToolCallTraceApplicationService(ToolCallTraceRepository toolCallTraceRepository) {
        this.toolCallTraceRepository = toolCallTraceRepository;
    }

    public List<ToolCallTrace> findByRunId(String runId) {
        return toolCallTraceRepository.findByRunId(runId);
    }
}
