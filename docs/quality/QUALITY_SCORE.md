# V1 Quality Score

Date: 2026-05-14

## Current Completion Score

Overall V1 score: 82 / 100.

The project has a complete local demo path, mechanical validation commands, modular package boundaries, traceable
Agent tool execution, and clear Harness documentation. The score is not higher because V1 intentionally avoids a real
LLM, real persistence, real order lookup tools, production security, and a richer evaluation dataset.

## Architecture Quality

Score: 86 / 100.

Strengths:

- Modular monolith package layout is in place under `com.example.aftersale`.
- API classes call application services instead of repositories.
- Domain models are independent from Spring Web.
- Agent orchestration does not directly depend on business repositories.
- Tools are prevented from direct repository access by ArchUnit.
- Business modules do not depend on the Agent module.

Current gaps:

- Infrastructure is in-memory only.
- Approval is represented at domain/tool-risk level, not as a full application flow.
- Order is modeled as a domain concept, but V1 does not include executable order query tools.

## Test Quality

Score: 84 / 100.

Strengths:

- API tests cover ticket creation and lookup.
- Domain tests cover Ticket, AgentRun, ToolCallTrace, and approval-risk behavior.
- Policy tests cover initialized V1 policy data, matching, empty results, and tool execution.
- Tool tests cover registry lookup, unknown tools, low-risk execution, high-risk approval blocking, and failure output.
- Agent flow tests cover the demo path, trace exposure, failure status, and trace cleanup.
- Architecture tests provide mechanical back-pressure for key boundaries.

Current gaps:

- Tests use in-memory collaborators and do not cover real persistence.
- Intent classification has deterministic examples but not a broad evaluation dataset.
- Trace assertions verify key behavior but do not benchmark latency or concurrency.

## Agent Capability Quality

Score: 76 / 100.

Strengths:

- Agent creates an `AgentRun`.
- Intent classification is deterministic and testable.
- Planning output is structured JSON.
- Tools are invoked through `ToolRegistry`.
- Tool calls are recorded as `ToolCallTrace`.
- Final suggestions include policy evidence.
- Tool failures surface as failed outputs and failed traces.

Current gaps:

- No real LLM or model adapter.
- No prompt versioning implementation beyond document guidance.
- No real order lookup or user order history tool.
- No multi-step reasoning beyond the V1 rule-based orchestration.

## Demo Readiness

Score: 88 / 100.

Strengths:

- The app can be started locally with Maven.
- README includes a curl-based demo walkthrough.
- Demo shows ticket creation, AgentRun creation, ticket update, and trace query.
- Tool trace output demonstrates input, output, status, latency, and run linkage.

Current gaps:

- Demo is API-only.
- Demo data resets when the application restarts.
- The expected trace contains policy retrieval and ticket note tools, not order lookup.

## Current Shortcomings

- V1 uses in-memory repositories only.
- Agent behavior is rule-based and intentionally deterministic.
- No production authentication or authorization.
- No real refund, compensation, payment, inventory, logistics, or order system integration.
- Human approval is a boundary and domain concept, not a complete operator workflow.
- No vector RAG, reranking, or retrieval evaluation set.

## V2 Improvement Directions

- Add real MySQL persistence and migration scripts.
- Add executable order query tools such as `get_order_by_id` and `get_user_orders`.
- Add an approval application service and review API for high-risk actions.
- Introduce a real LLM adapter behind a stable Agent interface.
- Add prompt versioning and prompt regression tests.
- Upgrade policy retrieval to vector or hybrid retrieval with cited evidence.
- Add an Agent evaluation dataset for intent, tool selection, and final suggestion quality.
- Add Docker Compose for repeatable local demos.
- Add observability metrics for AgentRun and tool latency.
