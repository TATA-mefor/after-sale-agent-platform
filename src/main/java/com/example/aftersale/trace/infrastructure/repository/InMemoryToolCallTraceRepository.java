package com.example.aftersale.trace.infrastructure.repository;

import com.example.aftersale.trace.domain.ToolCallTrace;
import com.example.aftersale.trace.domain.ToolCallTraceRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryToolCallTraceRepository implements ToolCallTraceRepository {

    private final Map<String, ToolCallTrace> traces = new ConcurrentHashMap<>();

    @Override
    public ToolCallTrace save(ToolCallTrace trace) {
        traces.put(trace.getTraceId(), trace);
        return trace;
    }

    @Override
    public List<ToolCallTrace> findByRunId(String runId) {
        return traces.values().stream()
                .filter(trace -> trace.getRunId().equals(runId))
                .sorted(Comparator.comparing(ToolCallTrace::getCreatedAt))
                .toList();
    }
}
