## Core API List

Current HTTP APIs are a demo/backend API surface, not a complete production CRUD platform. Stage 3.1 records this in
[API Completeness Decision](docs/decisions/DECISION_PROJECT_REVIEW_API_COMPLETENESS.md). Stage 3.2 adds bounded
Ticket list/query pagination. Stage 3.3 adds a read-only AgentRun get/status endpoint. Stage 3.4 records the
[Async / Streaming / Batch API Decision](docs/decisions/DECISION_PROJECT_REVIEW_ASYNC_STREAMING_BATCH_API.md):
current synchronous create/start plus status polling remains the safe path, while async AgentRun runtime,
SSE / WebSocket streaming, batch APIs, cancel / retry, AgentRun list pagination, and production auth / RBAC remain
future follow-ups; they are not completed runtime behavior.

Health:

```bash
GET /api/health
GET /actuator/health
```

Tickets:

```bash
POST /api/tickets
GET /api/tickets?page=0&size=20&sort=createdAt,desc
GET /api/tickets/{ticketId}
```

Ticket list supports bounded pagination and read-only filters for `status`, `userId`, `orderId`, `intentType`,
`createdFrom`, and `createdTo`. It does not start AgentRun execution and does not expose a public RAG search endpoint.

Agent execution:

```bash
POST /api/tickets/{ticketId}/agent-runs
GET /api/agent-runs/{runId}
GET /api/agent-runs/{runId}/traces
GET /api/agent-runs/{runId}/execution-tree
```

`GET /api/agent-runs/{runId}` is a read-only status polling view. It returns safe summary fields and links to trace
and execution-tree endpoints. It does not run the planner, execute tools, call ToolRegistry, write ToolCallTrace,
mutate Workspace, or inline execution-tree details.

Approval:

```bash
GET /api/approval-requests/pending
GET /api/approval-requests/{approvalRequestId}
POST /api/approval-requests/{approvalRequestId}/approve
POST /api/approval-requests/{approvalRequestId}/reject
```

The execution tree endpoint is read-only. It aggregates AgentRun, subtasks, ToolCallTrace records, ApprovalRequest
records, and workspace-derived summaries without mutating business state.

