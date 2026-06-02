package io.github.tatame.aftersale.trace.infrastructure.repository;

import io.github.tatame.aftersale.trace.domain.ToolCallTrace;
import io.github.tatame.aftersale.trace.domain.ToolCallTraceRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("!mysql")
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
