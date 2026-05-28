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

- Ticket APIs create and read after-sale tickets.
- AgentRun APIs trigger the configured Agent orchestration for an existing ticket.
- Approval APIs expose human review for high-risk proposed actions.
- Tool Trace APIs expose ToolCallTrace audit JSON for an AgentRun.
- Execution Tree APIs expose a read-only explanation view with subtasks, tools, approvals, and policy evidence nodes.
- Platform health includes `/api/health`; Actuator readiness is exposed separately at `/actuator/health`.

## Ticket Flow

Create a ticket with synthetic demo data, then read it back by `ticketId`. Creating a ticket does not run the Agent and
does not execute refunds, exchanges, compensation, payment, logistics, or dispute closure.

## AgentRun Flow

Trigger an AgentRun with:

```text
POST /api/tickets/{ticketId}/agent-runs
```

The default path uses deterministic local execution unless explicitly configured otherwise. Planner output is validated,
tools are executed only through ToolRegistry, and high-risk actions remain approval-gated.

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
