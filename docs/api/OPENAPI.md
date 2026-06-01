# OpenAPI / Swagger UI

V4.6.4 adds OpenAPI and Swagger UI documentation for the current Spring Boot HTTP APIs. This is API documentation
polish only; it does not add endpoints, change business runtime behavior, or modify RAG retrieval.

## Local Entry Points

Start the default offline application:

```bash
mvn spring-boot:run
```

Then open:

```text
http://localhost:8080/swagger-ui/index.html
http://localhost:8080/swagger-ui.html
http://localhost:8080/v3/api-docs
http://localhost:8080/actuator/health
```

The default profile uses in-memory repositories and fake / offline paths where applicable. It does not require API
keys, PostgreSQL, PGvector, Docker, MySQL, Redis, a real LLM, a real embedding provider, or external network access.

## API Groups

- Ticket APIs create, read, and list after-sale tickets with bounded pagination.
- AgentRun APIs trigger the configured Agent orchestration for an existing ticket and expose read-only status polling.
- Approval APIs expose human review for high-risk proposed actions.
- Tool Trace APIs expose ToolCallTrace audit JSON for an AgentRun.
- Execution Tree APIs expose a read-only explanation view with subtasks, tools, approvals, and policy evidence nodes.
- Platform health includes `/api/health`; Actuator readiness is exposed separately at `/actuator/health`.

## API Completeness Roadmap

The current OpenAPI document describes the existing demo/backend API surface. It is not a complete production CRUD API
and it is not production API hardening.

- Ticket APIs currently cover create/get and bounded list/query pagination.
- AgentRun APIs currently cover create/start for a ticket and read-only get/status polling.
- Trace and Execution Tree APIs are read-only views. They are not SSE / WebSocket streaming endpoints.
- Approval APIs currently cover pending/get/approve/reject.
- Stage 3.4 evaluates async AgentRun, status polling, SSE / WebSocket, batch API, cancel / retry, and AgentRun list
  pagination without adding runtime endpoints.
- Async AgentRun runtime, SSE / WebSocket runtime, batch APIs, cancel / retry, AgentRun list pagination,
  production auth / RBAC, idempotency, rate limiting, and API audit hardening remain future work.

The decision record for this boundary is
`docs/decisions/DECISION_PROJECT_REVIEW_API_COMPLETENESS.md`. The Stage 3.4 async / streaming / batch evaluation is
recorded in `docs/decisions/DECISION_PROJECT_REVIEW_ASYNC_STREAMING_BATCH_API.md`. `search_aftersale_policy` remains a
LOW-risk read-only ToolRegistry tool, not a public RAG HTTP endpoint.

## Ticket Flow

Create a ticket with synthetic demo data, list tickets with bounded pagination, then read a ticket back by `ticketId`.
Creating or listing a ticket does not run the Agent and does not execute refunds, exchanges, compensation, payment,
logistics, or dispute closure.

```text
POST /api/tickets
GET /api/tickets?page=0&size=20&sort=createdAt,desc
GET /api/tickets?status=CREATED&userId=U-DEMO-1001&orderId=O-DEMO-2001
GET /api/tickets/{ticketId}
```

Ticket list supports `page`, `size`, `sort`, `status`, `userId`, `orderId`, `intentType`, `createdFrom`, and
`createdTo`. Page indexes are zero-based, `size` is bounded, and sort fields are whitelisted.

## AgentRun Flow

Trigger an AgentRun with:

```text
POST /api/tickets/{ticketId}/agent-runs
GET /api/agent-runs/{runId}
```

The default path uses deterministic local execution unless explicitly configured otherwise. Planner output is validated,
tools are executed only through ToolRegistry, and high-risk actions remain approval-gated.

`GET /api/agent-runs/{runId}` is a read-only status polling endpoint. It returns `runId`, `ticketId`, `status`,
`startedAt`, `completedAt`, `finalSummary`, `failureSummary`, `traceAvailable`, `executionTreeAvailable`, `traceUrl`,
and `executionTreeUrl`. It does not run the planner, execute tools, call ToolRegistry, mutate tickets, write
ToolCallTrace, inline workspace data, or replace the trace / execution-tree views.

Stage 3.4 keeps this status polling path as the current safe progress view. It does not implement async AgentRun,
SSE / WebSocket streaming, batch API, cancel / retry, or AgentRun list pagination.

## Approval Flow

Approval APIs list pending approval requests and record approve / reject decisions. Approval records are audit and
control objects; the OpenAPI docs do not claim automatic real refund, exchange, payment, or logistics execution.

## Execution Tree Flow

Execution Tree is a read-only view:

```text
GET /api/agent-runs/{runId}/execution-tree
```

It can show subtasks, tool calls, approval nodes, and concise policy evidence summaries. Querying the tree must not
change Ticket, AgentRun, ToolCallTrace, Workspace, Approval, or retrieval state.

## RAG Evidence / Policy Search Boundary

`search_aftersale_policy` is a LOW-risk read-only ToolRegistry tool, not a public HTTP controller in V4.6.4. When it is
called by Agent runtime, ToolCallTrace output JSON can include policy evidence with `retrievalMode` values `KEYWORD`,
`VECTOR`, or `HYBRID`, plus evidence identifiers, source, score, snippet, and fallback status.

RAG evidence is evidence-only policy support. Evidence score is a retrieval score, not business decision confidence.
RAG evidence does not execute refunds, exchanges, coupon compensation, payment changes, logistics changes, or dispute
closure.

## Interview Swagger UI Walkthrough

Use Swagger UI as an existing API map, not as proof of new runtime behavior:

1. Show Ticket APIs for creating, listing, and reading synthetic after-sale tickets.
2. Show AgentRun APIs for triggering the current Agent orchestration path.
3. Show Approval APIs to explain high-risk action gating.
4. Show ToolCallTrace and Execution Tree APIs to explain audit and read-only interpretation.
5. Mention `/actuator/health` as offline readiness, then use `docs/quality/VALIDATION_COMMANDS.md` for the default
   validation boundary.

OpenAPI docs cover existing HTTP APIs. They do not add a new public RAG policy-search endpoint. `search_aftersale_policy`
remains a ToolRegistry tool and RAG evidence remains evidence-only policy support.

If discussing future API hardening, use the Stage 3.4 decision: AgentRun list pagination should come before async
execution; SSE should be considered before WebSocket for one-way progress; batch APIs require idempotency, rate
limits, permission checks, and partial-failure semantics; streaming must not expose raw prompts, raw LLM responses,
secrets, full tool outputs, or full evidence chunks.

## Actuator Health Boundary

`/actuator/health` remains the only actuator endpoint exposed by default. RAG health indicators are offline readiness
diagnostics, not live PGvector or provider connectivity checks. Health details are disabled by default and must not
expose secrets, prompts, raw text, full chunk content, or local paths.

## Non-goals

- OpenAPI docs are not a production deployment guide.
- V4.6.4 does not add runtime behavior.
- V4.6.4 does not add authentication or authorization.
- V4.6.4 does not expose management `env`, `configprops`, or `beans` endpoints.
- V4.6.4 does not call real LLMs, real embedding providers, PostgreSQL, PGvector, Docker, MySQL, Redis, or external
  network services.
