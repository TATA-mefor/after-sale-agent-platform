# EXEC_PLAN_V4_RAG_ACTUATOR_HEALTH

Date: 2026-05-28

Status: Completed

## Goal

Add Spring Boot Actuator health indicators for RAG components so local demos and reviewers can inspect offline readiness
for policy search, vector-store configuration, embedding configuration, and ingestion contracts without requiring live
providers.

## Scope Completed

- Added offline-safe RAG health configuration.
- Added RAG search, vector-store, embedding, and ingestion health indicators.
- Exposed only the Actuator health endpoint by default.
- Added tests for health status, sanitized details, endpoint exposure, and architecture boundaries.
- Updated V4 roadmap, quality, Spring Boot completeness decision, README, and RAG retrieval contract docs.

## What Changed

- `src/main/java/com/example/aftersale/policy/rag/health/` now contains the RAG health indicator package.
- `src/main/resources/application.yml` now configures `agent.rag.health` and limits management exposure to health.
- `src/test/java/com/example/aftersale/policy/rag/health/` covers indicator behavior, Actuator endpoint output, and docs.
- `src/test/java/com/example/aftersale/ArchitectureTest.java` includes V4.6.3 health boundary rules.

## Health Indicator Boundary

Health indicators are readiness diagnostics only. They inspect bean presence and sanitized configuration state. They do
not add runtime business behavior, do not execute tools, do not create AgentRun state, and do not mutate Ticket,
ToolCallTrace, Workspace, Execution Tree, or Approval records.

## RAG Search Health Boundary

RAG search health checks whether the RAG policy search service is available and reports KEYWORD / VECTOR / HYBRID as
supported retrieval modes. It does not execute search and does not call ToolRegistry.

## Vector Store Health Boundary

Vector-store health reports `none`, `fake`, or `pgvector` configuration readiness. It does not connect to PostgreSQL,
does not connect to PGvector, does not execute SQL, does not call `PolicyVectorRepository.search`, and does not call
Spring AI `VectorStore`.

## Embedding Health Boundary

Embedding health reports disabled / fake / Spring AI configuration readiness. It does not call `EmbeddingClient.embed`,
does not call `SpringAiEmbeddingClient`, does not call a real Spring AI `EmbeddingModel`, and does not send text to any
provider. It does not call real embedding providers.

## Ingestion Health Boundary

Ingestion health reports ingestion contract and bean presence. It does not read files, does not chunk policy text, does
not calculate embeddings, and does not write repositories.

## Default Offline Test Boundary

Default validation remains offline. It does not require real LLMs, API keys, PostgreSQL, PGvector, Docker, MySQL, Redis,
real embedding providers, or external network.

## Secret Safety Boundary

Health details are disabled by default. When enabled, RAG details use sanitized provider/configuration signals and do
not expose passwords, API keys, tokens, local paths, prompts, raw text, full chunk content, or credential-bearing URLs.
RAG health does not expose secrets.

## Architecture Boundary

Architecture rules keep the health package away from Spring Web controllers, JDBC, DataSource, JdbcTemplate, Spring AI
concrete clients, VectorStore, ToolRegistry runtime, AgentRun runtime, Agent / Handler / Skill dependencies, and
business repository implementations.

## Validation Commands

```bash
mvn test
mvn checkstyle:check
mvn spotbugs:check
mvn test -Dtest=ArchitectureTest
```

## Known Limitations

- Health status is offline readiness, not live PGvector connectivity.
- Health status does not prove real provider availability, embedding latency, vector index freshness, or production
  monitoring coverage.
- OpenAPI / API docs polish remains outside V4.6.3.

## Follow-ups

- V4.6.4: OpenAPI / API docs polish.
- Future opt-in live checks may validate real PGvector and real provider connectivity outside the default test path.

## Completion Signal

TASK_COMPLETE
