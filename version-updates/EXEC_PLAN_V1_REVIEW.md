# EXEC_PLAN_V1 Review

Date: 2026-05-14

## V1 Completed Scope

V1 delivered a local Spring Boot after-sale Agent platform that can:

- Create an after-sale ticket.
- Query ticket details.
- Trigger a rule-based AgentRun for a ticket.
- Classify the ticket intent for the supported demo scenarios.
- Retrieve in-memory after-sale policy evidence.
- Execute low-risk tools through `ToolRegistry`.
- Persist the Agent suggestion as an internal ticket note.
- Record and expose `ToolCallTrace` entries.
- Enforce key architecture rules with ArchUnit.
- Run test, Checkstyle, SpotBugs, and architecture verification commands locally.

## Core Demo Flow

```text
POST /api/tickets
  -> creates Ticket with status CREATED

POST /api/tickets/{ticketId}/agent-runs
  -> creates AgentRun
  -> classifies intent
  -> runs search_aftersale_policy
  -> runs add_ticket_note
  -> stores final suggestion
  -> marks Ticket RESOLVED for the happy-path demo

GET /api/tickets/{ticketId}
  -> returns updated intent, status, note, and Agent suggestion

GET /api/agent-runs/{runId}/traces
  -> returns trace records for the Agent tool calls
```

## Architecture Boundaries

V1 keeps the system as a modular monolith:

- `api` classes expose HTTP use cases and call application services.
- `application` classes orchestrate use cases.
- `domain` classes hold entities, enums, and repository contracts.
- `infrastructure` classes provide in-memory adapters.
- Agent orchestration calls tools instead of repositories.
- Tools call application services instead of repositories.
- High-risk actions are blocked at the tool-risk boundary.

ArchUnit enforces the most important boundaries:

- API layer must not access repositories.
- Domain layer must not depend on Spring Web.
- Agent module must not access business repositories directly.
- Tool module must not access repositories directly.
- Business modules must not depend on the Agent module.

## Agent Capabilities

V1 Agent capabilities are intentionally deterministic:

- Rule-based intent classification.
- Structured JSON plan generation.
- Policy evidence extraction from in-memory policies.
- Low-risk tool invocation through `ToolRegistry`.
- AgentRun success and failure state recording.
- Final suggestion generation with evidence references.

V1 does not include a real LLM, multi-Agent orchestration, prompt runtime, or autonomous high-risk actions.

## Tool Call Chain

The final V1 demo chain is:

```text
AgentApplicationService
  -> ToolRegistry
  -> search_aftersale_policy
  -> PolicyApplicationService
  -> InMemoryPolicyRepository

AgentApplicationService
  -> ToolRegistry
  -> add_ticket_note
  -> TicketApplicationService
  -> InMemoryTicketRepository
```

The broader tool system also includes:

- `create_aftersale_ticket`
- `update_ticket_status`

`update_ticket_status` returns `REQUIRES_APPROVAL` for closing a ticket.

## Trace Mechanism

Each tool call executed inside `ToolTraceContext.runWith(runId, ...)` is recorded through the trace recorder.

Trace records include:

- `runId`
- `toolName`
- `inputJson`
- `outputJson`
- `status`
- `latencyMs`
- `errorMessage`
- `createdAt`

Both successful and failed tool calls are covered by tests.

## Validation Results

Final M9 validation commands:

```bash
mvn test
mvn checkstyle:check
mvn spotbugs:check
mvn test -Dtest=ArchitectureTest
```

Result: passed in the final local M9 validation run.

## Known Limitations

- No real LLM provider.
- No real database or Redis.
- No real payment, refund, compensation, inventory, order, or logistics integration.
- No executable order lookup tool in final V1.
- No production authentication or authorization.
- No complex frontend.
- Policy retrieval is keyword/rule based, not vector RAG.
- Human approval is represented as a risk boundary, not a full review UI or workflow.

## V2 Candidate Directions

- Add MySQL persistence.
- Add order query tools and richer order demo data.
- Add approval APIs and operator workflow for high-risk actions.
- Introduce an LLM adapter while preserving deterministic tests.
- Add vector or hybrid policy retrieval.
- Add Agent evaluation datasets.
- Add Docker Compose and observability.
- Expand trace review and audit APIs.

## Completion Signal

TASK_COMPLETE
