# V4.6.1 RAG Evaluation Cases and Metrics

Date: 2026-05-28

Status: Completed

## Goal

Add an offline, deterministic evaluation dataset, metrics model, runner, and tests for KEYWORD / VECTOR / HYBRID RAG
policy evidence retrieval quality after the V4.5 hybrid search, trace, workspace, and execution tree work.

## Scope Completed

- Added a small JSONL RAG policy evaluation dataset for KEYWORD / VECTOR / HYBRID retrieval cases.
- Added RAG evaluation case, expected result, result, report, metric, failure, dataset loader, fixture, and runner models.
- Added deterministic fake / in-memory retrieval fixtures for return, exchange, refund-only, logistics, coupon, special
  goods, repair, unsupported, fallback, empty-result, dedup, low-score, and evidence-only safety cases.
- Added tests for dataset loading, fixture determinism, runner behavior, metrics, failure shape, safety, citation
  completeness, and default offline boundaries.
- Updated V4 roadmap, RAG retrieval contract, evaluation docs, quality score, and README with the V4.6.1 boundary.

## What Changed

- `docs/evaluation/rag_policy_cases.jsonl` now defines deterministic RAG policy retrieval evaluation cases.
- `policy.rag.evaluation` contains the offline evaluation model and runner.
- The runner calls the RAG search application boundary directly and evaluates returned evidence with exact-field and
  substring checks.
- Metrics report pass rate, evidence recall pass rate, evidence source pass rate, retrieval mode pass rate, fallback
  accuracy, empty-result accuracy, citation completeness rate, safety pass rate, and average evidence count.

## Dataset Boundary

The dataset is a small, reviewable JSONL file. It does not use raw datasets and does not contain real user data,
database credentials, API keys, tokens, local absolute paths, or private source paths.

Each case expresses expected retrieval mode, evidence source, category or product type, snippet keyword expectations,
forbidden completed-action text, evidence count bounds, fallback expectation, empty-result expectation, and optional
document or chunk citation requirements.

## Metrics Boundary

All metrics are deterministic. They use exact enum/string checks, bounded evidence counts, required snippet substring
checks, citation presence checks, and forbidden-text checks.

V4.6.1 does not use LLM-as-judge, semantic grading, external evaluation frameworks, live provider calls, PGvector,
Docker, MySQL, Redis, API keys, raw datasets, or external network access.

Scores remain retrieval evidence scores, not business decision confidence.

## Runner Boundary

`RagEvaluationApplicationService` evaluates retrieval quality only. It may call `RagPolicySearchApplicationService`
with fake / in-memory dependencies, but it does not create Ticket, AgentRun, ToolCallTrace, AgentWorkspace, Execution
Tree state, or ApprovalRequest records.

The runner does not modify `search_aftersale_policy` runtime behavior, retrieval algorithms, ToolRegistry semantics,
ToolCallTrace schema, workspace evidence logic, or execution tree runtime.

## Default Offline Test Boundary

Default tests use `FakeEmbeddingClient`, `InMemoryPolicyVectorRepository`, and in-memory keyword policy data.

Default validation does not require real LLMs, real embedding providers, Spring AI provider calls, Spring AI
`VectorStore`, PostgreSQL, PGvector, Docker, MySQL, Redis, API keys, raw datasets, or external network.

## Architecture Boundary

RAG evaluation stays in the policy RAG evaluation boundary. It does not depend on Spring Web, JDBC, `DataSource`,
PGvector infrastructure, Spring AI `VectorStore`, ToolCallTrace persistence, workspace persistence, execution tree
runtime, Agent handlers, or Skill runtime.

Agent, Handler, and Skill layers must not depend on `RagEvaluationApplicationService` or RAG evaluation fixtures.

## Validation Commands

```bash
mvn test
mvn checkstyle:check
mvn spotbugs:check
mvn test -Dtest=ArchitectureTest
```

## Known Limitations

- V4.6.1 evaluates retrieval evidence quality only, not end-to-end Agent flow.
- V4.6.1 does not add runtime features or change `search_aftersale_policy` retrieval logic.
- V4.6.1 does not add a V4 demo script.
- V4.6.1 does not implement Actuator health indicators.
- V4.6.1 does not implement OpenAPI or API docs polish.
- Live PGvector and real embedding provider evaluation remain future opt-in work.

## Follow-ups

- V4.6.2 can add a V4 RAG demo script.
- V4.6.3 can add Actuator health indicators.
- V4.6.4 can polish OpenAPI / API docs.
- Future opt-in evaluation can compare live PGvector / embedding providers without changing the default offline path.

## Completion Signal

TASK_COMPLETE
