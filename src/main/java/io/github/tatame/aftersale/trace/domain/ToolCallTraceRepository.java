package io.github.tatame.aftersale.trace.domain;

import java.util.List;

public interface ToolCallTraceRepository {

    ToolCallTrace save(ToolCallTrace trace);

    List<ToolCallTrace> findByRunId(String runId);
}
