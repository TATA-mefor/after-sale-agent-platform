package com.example.aftersale.trace.application;

import com.example.aftersale.trace.domain.ToolCallTrace;
import com.example.aftersale.trace.domain.ToolCallTraceRepository;
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
