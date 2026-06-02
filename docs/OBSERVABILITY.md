## Observability

V3.3 adds request correlation and structured log fields without introducing an external logging platform. Each HTTP
request returns an `X-Request-Id` response header. If the request already includes `X-Request-Id`, the same value is
returned; otherwise the application generates one.

Example with an explicit request id:

```bash
curl -H "X-Request-Id: demo-request-001" http://localhost:8080/api/health -i
```

Use the returned `X-Request-Id` to search logs for the request. Agent-related logs also include the available business
IDs:

```text
requestId
ticketId
agentRunId
subtaskId
toolName
approvalRequestId
```

Typical troubleshooting flow:

1. Start with `requestId` from the API response header.
2. Find the `ticketId` from ticket creation logs or the API response.
3. Find `agentRunId` from AgentRun logs or the trigger response.
4. Inspect persisted tool audit records:

```bash
curl http://localhost:8080/api/agent-runs/{runId}/traces
```

5. Inspect the read-only execution tree:

```bash
curl http://localhost:8080/api/agent-runs/{runId}/execution-tree
```

Approval flows remain queryable through:

```bash
curl http://localhost:8080/api/approval-requests/pending
curl http://localhost:8080/api/approval-requests/{approvalRequestId}
```

Logs are diagnostic only. ToolCallTrace, ApprovalRequest records, and the Execution Tree API remain the audit and
inspection surfaces. Logs must not contain API keys, database passwords, full LLM prompts, sensitive credentials, or
long raw user text.

Stage 2 of the project review correction records the observability hardening decision in
[Observability Hardening Decision](docs/decisions/DECISION_PROJECT_REVIEW_OBSERVABILITY_HARDENING.md). The current
baseline is MDC / structured logs, ToolCallTrace, ApprovalRequest, Execution Tree, Actuator health, RAG readiness
diagnostics, OpenAPI docs, and offline RAG evaluation metrics. Prometheus, Grafana, OpenTelemetry, collector-based
tracing, production dashboards, provider cost metrics, and external log aggregation remain future / opt-in work.
Default actuator exposure remains limited to `/actuator/health`.

