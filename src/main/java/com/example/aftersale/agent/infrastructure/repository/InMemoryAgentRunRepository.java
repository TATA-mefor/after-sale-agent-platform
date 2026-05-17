package com.example.aftersale.agent.infrastructure.repository;

import com.example.aftersale.agent.domain.AgentRun;
import com.example.aftersale.agent.domain.AgentRunRepository;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("!mysql")
public class InMemoryAgentRunRepository implements AgentRunRepository {

    private final Map<String, AgentRun> agentRuns = new ConcurrentHashMap<>();

    @Override
    public AgentRun save(AgentRun agentRun) {
        agentRuns.put(agentRun.getRunId(), agentRun);
        return agentRun;
    }

    @Override
    public Optional<AgentRun> findById(String runId) {
        return Optional.ofNullable(agentRuns.get(runId));
    }
}
