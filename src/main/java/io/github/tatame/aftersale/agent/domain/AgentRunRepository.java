package io.github.tatame.aftersale.agent.domain;

import java.util.Optional;

public interface AgentRunRepository {

    AgentRun save(AgentRun agentRun);

    Optional<AgentRun> findById(String runId);
}
