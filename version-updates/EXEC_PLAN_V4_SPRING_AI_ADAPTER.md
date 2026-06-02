# V4.2 Spring AI Adapter Completion Record

Date: 2026-05-22
Status: Completed

## Goal

Introduce Spring AI as an optional provider adapter for chat and embedding without changing AgentRun, ToolRegistry,
Skill, Approval, Trace, or Workspace semantics.

## Scope Completed

- Added Spring AI dependency management and OpenAI model starter.
- Added default configuration that keeps Spring AI disabled and prevents default model auto-creation.
- Added `spring-ai-chat` LLM provider selection.
- Added Spring AI chat adapter behind the existing `LlmClient` boundary.
- Added embedding client boundary with deterministic fake implementation and Spring AI adapter implementation.
- Added opt-in Spring AI live smoke tests for chat and embedding configuration.
- Added tests for provider selection, disabled-provider errors, request mapping, response parsing, sanitized provider
  errors, and deterministic fake embeddings.
- Extended architecture checks so Agent, Handler, and Skill layers do not depend directly on Spring AI classes.

## What Changed

`SpringAiLlmClient` maps the existing `LlmRequest` system and user prompts to a Spring AI `ChatClient` call and returns
plain text in `LlmResponse`. The text still flows through the existing planner parser and validator.

`EmbeddingClient` defines the project-owned embedding boundary. `FakeEmbeddingClient` supports deterministic offline
tests, while `SpringAiEmbeddingClient` adapts Spring AI `EmbeddingModel` through a small gateway.

## Spring AI Chat Adapter Boundary

Spring AI chat is infrastructure-only:

```text
LlmAgentPlanner
-> LlmClient
-> SpringAiLlmClient
-> Spring AI ChatClient
```

Spring AI does not receive `ToolRegistry` tools as tool/function callbacks. It only returns planner text.

## Spring AI Embedding Adapter Boundary

Embedding is a provider boundary only in V4.2:

```text
Policy/RAG application code
-> EmbeddingClient
-> FakeEmbeddingClient or SpringAiEmbeddingClient
-> Spring AI EmbeddingModel
```

V4.2 does not add VectorStore, PGvector tables, policy ingestion, similarity search, or HYBRID RAG retrieval.

## Provider Selection Boundary

Supported LLM provider values now include:

```text
openai-responses
dashscope-responses
dashscope-chat-compatible
spring-ai-chat
```

Unknown providers fail clearly. Selecting `spring-ai-chat` while Spring AI is disabled fails with a configuration error
instead of a null pointer.

## Live Test Boundary

Spring AI live smoke tests require explicit system properties:

```bash
mvn test -Dtest=SpringAiLlmClientLiveSmokeTest -Dlive.spring-ai=true -Dlive.llm=true
mvn test -Dtest=SpringAiEmbeddingClientLiveSmokeTest -Dlive.spring-ai=true -Dlive.embedding=true
```

These tests do not create tickets, AgentRuns, traces, database records, vector stores, or policy chunks.

## Default Test Boundary

Default validation remains offline and deterministic. It must not require real LLMs, embedding providers, API keys,
PostgreSQL, PGvector, Docker, MySQL, Redis, or external network.

## Validation Commands

Required:

```bash
mvn test
mvn checkstyle:check
mvn spotbugs:check
mvn test -Dtest=ArchitectureTest
```

## Known Limitations

- Spring AI live smoke tests currently validate opt-in configuration boundaries and avoid AgentRun side effects.
- Embedding output is not persisted.
- No vector retrieval, RAG ranking, policy ingestion, or Execution Tree evidence node is implemented in V4.2.
- Runtime Skill migration remains a later phase.

## Follow-ups

- V4.3: PGvector / VectorStore profile and fake vector repository.
- V4.4: Policy ingestion, chunking, checksum deduplication, and embedding run tracking.
- V4.5: Hybrid policy search tool and PolicyEvidence workspace integration.

## Completion Signal

TASK_COMPLETE
