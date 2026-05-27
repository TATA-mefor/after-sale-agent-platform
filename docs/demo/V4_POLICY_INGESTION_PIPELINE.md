# V4 Policy Ingestion Pipeline

## Purpose

V4.4 provides the policy ingestion pipeline foundation for later RAG retrieval. It is an admin / offline pipeline
capability, not an Agent automatic tool and not part of the current AgentRun runtime path.

The pipeline prepares policy evidence data that later V4.5 HYBRID retrieval can use. It does not execute business
actions and does not decide refund, exchange, payment, logistics, coupon, or dispute outcomes.

## Current Capabilities

V4.4 now includes:

- ingestion run, status, source, document, chunk, and error domain models;
- `PolicyIngestionRepository` and `InMemoryPolicyIngestionRepository` for default offline tests;
- deterministic chunking with bounded token estimates;
- SHA-256 document and chunk checksum calculation;
- checksum-based document and chunk dedup decisions;
- fake-provider embedding pipeline through the `EmbeddingClient` abstraction;
- vector writes through the `PolicyVectorRepository` contract;
- default tests using `FakeEmbeddingClient`, `InMemoryPolicyIngestionRepository`, and
  `InMemoryPolicyVectorRepository`.

## Current Non-Goals

V4.4 does not include:

- no Admin Controller yet;
- no `ingest_policy_document` tool;
- no ToolRegistry wiring;
- no real Spring AI embedding default path;
- no `JdbcPolicyIngestionRepository`;
- no `JdbcPolicyVectorRepository`;
- no PGvector live writes;
- no RAG / HYBRID retrieval;
- `search_aftersale_policy` not wired to vector search yet;
- no AgentRun runtime usage.

## Pipeline Flow

```text
1. create ingestion run
2. record ingestion document
3. chunk document
4. compute document/chunk checksum
5. check dedup
6. save ingestion chunks
7. embed chunks via EmbeddingClient contract
8. write PolicyDocument / PolicyChunk / PolicyEmbedding through PolicyVectorRepository contract
9. update run status to COMPLETED / PARTIALLY_FAILED / FAILED
```

## Default Offline Example

The default test path is fully offline:

```text
PolicyIngestionRepository -> InMemoryPolicyIngestionRepository
EmbeddingClient -> FakeEmbeddingClient
PolicyVectorRepository -> InMemoryPolicyVectorRepository
```

This path does not require an API key, PostgreSQL, PGvector, Docker, MySQL, Redis, a real LLM, a real embedding
provider, or external network access.

## Future Real Path

Later phases can add opt-in production-like paths without changing the V4.4 boundary:

- real Spring AI embedding opt-in;
- `JdbcPolicyVectorRepository`;
- PGvector compose-backed local validation;
- Admin API with explicit security;
- HYBRID retrieval;
- ToolRegistry `search_aftersale_policy` integration.

These are future capabilities. V4.4 does not claim live PGvector ingestion or Agent runtime vector retrieval.

## Failure Handling

The V4.4 foundation documents and tests these failure cases:

- blank document or chunk content fails clearly;
- dimension mismatch follows pipeline options;
- duplicate embedding can be skipped or failed according to options;
- partial chunk failures move the run to `PARTIALLY_FAILED`;
- all chunk failures move the run to `FAILED`;
- failure details are sanitized and bounded.

Failure messages must not include complete raw text, complete chunk content, API keys, passwords, tokens, local paths,
full prompts, or provider secrets.

## Security And Safety

Policy ingestion remains outside the Agent runtime. It must not be exposed as an Agent tool or registered in
ToolRegistry without a separate security and execution-boundary decision.

The pipeline must not log or persist complete `rawText`, complete chunk content, API keys, passwords, tokens, local
absolute paths, full prompts, or raw dataset paths. It also must not execute refunds, exchanges, payments, logistics
actions, coupon compensation, inventory changes, or dispute closure.

RAG evidence remains evidence only. It is not a business action completion signal.
