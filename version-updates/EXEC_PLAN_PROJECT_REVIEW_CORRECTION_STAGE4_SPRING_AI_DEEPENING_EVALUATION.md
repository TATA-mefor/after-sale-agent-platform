# Project Review Correction Stage 4: Spring AI Deepening Evaluation

Date: 2026-06-01

Status: Completed

## Goal

Evaluate deeper Spring AI usage after the project review, including ChatMemory, Advisors, Spring AI Tool Calling API,
and bulk embedding, without changing runtime behavior.

## Scope Completed

- Added the Spring AI deepening decision record.
- Updated README, V4 Spring AI adapter decision, remediation plan, active correction plan, quality score, and
  validation commands.
- Added docs harness coverage for Spring AI baseline wording, future capability boundaries, ToolRegistry / Agent
  safety, default offline validation, and secret / path safety.

## What Changed

- `docs/decisions/DECISION_PROJECT_REVIEW_SPRING_AI_DEEPENING.md` records the Stage 4 evaluation.
- Existing docs now state Stage 4 completed decision/evaluation only.
- Docs explicitly keep ChatMemory, Advisors, Spring AI Tool Calling API, bulk embedding runtime, provider behavior
  changes, public RAG HTTP endpoints, and RAG retrieval quality changes as future work.

## Spring AI Baseline Boundary

Current Spring AI usage remains adapter foundation:

- Spring AI Chat adapter foundation through `LlmClient`;
- Spring AI embedding adapter foundation through `EmbeddingClient`;
- `FakeEmbeddingClient` for deterministic offline tests;
- provider output still passes through `AgentPlanParser` and `AgentPlanValidator`;
- live Spring AI smoke tests are opt-in.

## ChatMemory Evaluation Boundary

ChatMemory is not implemented in Stage 4. Future ChatMemory work must not replace `AgentWorkspace`, `ToolCallTrace`,
Execution Tree, or project-owned business state.

## Advisor Evaluation Boundary

Advisors are not implemented in Stage 4. Future Advisor work must not bypass `search_aftersale_policy`, RAG evidence
merge logic, ToolRegistry, raw prompt safety, or evidence-only rules.

## Tool Calling API Boundary

Spring AI Tool Calling API is not enabled in Stage 4. Spring AI Tool Calling API cannot replace ToolRegistry, and LLMs
must not directly execute project tools. High-risk actions still require Approval.

## Bulk Embedding Evaluation Boundary

Bulk embedding runtime is not implemented in Stage 4. Future bulk embedding must stay behind `EmbeddingClient`
abstraction and must keep fake / in-memory default validation available.

## ToolRegistry / Agent Boundary

ToolRegistry remains the Agent tool execution entry. `AgentPlanParser` and `AgentPlanValidator` must not be bypassed.
`search_aftersale_policy` remains LOW-risk read-only and returns policy evidence only.

## Default Offline Boundary

Docs harness tests only read repository files. Default validation does not require real LLMs, API keys, PostgreSQL,
PGvector, Docker, MySQL, Redis, real embedding providers, Spring AI live provider calls, Spring AI VectorStore, or
external network.

## Runtime Non-change Boundary

Stage 4 changes docs and docs harness tests only. It does not modify `src/main/java`, `src/main/resources`, `pom.xml`,
Spring AI clients, provider clients, Planner, AgentPlanParser, AgentPlanValidator, ToolRegistry, RAG runtime,
ingestion pipeline, health indicators, OpenAPI config, ToolCallTrace, Workspace, Execution Tree, or
AgentApplicationService.

## Validation Commands

```bash
mvn test -Dtest=SpringAiDeepeningDecisionDocsTest,AsyncStreamingBatchApiDecisionDocsTest,ObservabilityHardeningDecisionDocsTest,ProductionConfigTemplateDocsTest,ProjectRemediationPlanDocsTest
mvn test
mvn checkstyle:check
mvn spotbugs:check
mvn test -Dtest=ArchitectureTest
```

## Known Limitations

- ChatMemory runtime is not implemented.
- Advisors runtime is not implemented.
- Spring AI Tool Calling API is not enabled.
- Bulk embedding runtime is not implemented.
- RAG reranking, query rewriting, RRF, and chunk window expansion remain future work.
- Production auth, production monitoring, production deployment, and real external business integrations remain
  future work.

## Follow-ups

- Stage 5: RAG retrieval quality improvement planning or implementation.
- Future Spring AI task: evaluate ChatMemory only if it preserves AgentWorkspace and audit boundaries.
- Future Spring AI task: evaluate Advisors only behind project RAG and ToolRegistry boundaries.
- Future ingestion task: add bulk embedding behind `EmbeddingClient` if scale requires it.

## Completion Signal

TASK_COMPLETE
