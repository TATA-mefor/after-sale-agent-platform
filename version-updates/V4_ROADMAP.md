## V4 Roadmap: RAG, Spring AI, Tool / Skill Layer

V4 focuses on interview-critical AI engineering capabilities:

- Spring AI provider adapter;
- RAG / vectorized after-sale policy retrieval;
- PostgreSQL + PGvector opt-in profile;
- Policy document ingestion, chunking, embedding, and evidence retrieval;
- Tool / Skill capability layer;
- Execution Tree evidence visualization;
- Spring Boot completeness improvements.

V4.0 pre-flight fixes, V4.1 Tool / Skill Layer Foundation, V4.2 Spring AI Adapter, V4.3 PGvector profile/schema/fake
vector/compose docs, V4.4 Policy Ingestion Foundation, V4.5 Hybrid RAG Policy Search Tool, V4.6 evaluation/demo/
Actuator/OpenAPI docs, and V4.7 documentation / architecture / final closure are completed. The final V4 completion
record is [EXEC_PLAN_V4_FINAL_COMPLETION_RECORD.md](version-updates/EXEC_PLAN_V4_FINAL_COMPLETION_RECORD.md),
and the reviewer-facing release summary is [V4_RELEASE_SUMMARY.md](version-updates/V4_RELEASE_SUMMARY.md). Skill is now a
first-class Java contract and registry concept, while the current AgentRun execution path still uses the existing
Specialist Handler dispatch. Spring AI is available as an optional provider adapter and is disabled by default.

V4 preserves the existing Agent safety model:

```text
LLM plans only.
Skill orchestrates safely.
ToolRegistry executes atomic tools.
RAG retrieves policy evidence.
ToolCallTrace records tool calls.
Approval blocks high-risk actions.
```

### Planned V4 Profiles

```text
default       -> in-memory / fake embedding / no external dependency
mysql         -> existing V3 MySQL persistence
rag-postgres  -> PostgreSQL + PGvector for policy RAG
spring-ai-live -> explicit live provider validation
```

### V4 Demo Flow Boundary

```text
→ ticket creation
→ AgentRun
→ search_aftersale_policy with HYBRID retrieval
→ ToolCallTrace
→ AgentWorkspace.PolicyEvidence
→ Execution Tree evidence view
→ final suggestion with policy evidence
→ RAG evaluation metrics
```

Policy ingestion, fake-provider embedding pipeline, and optional PGvector local setup are documented separately. They
are not Agent runtime tools and do not make real PGvector or real embedding providers part of the default demo path.

V4 demo documents:

- [V4 Interview Demo Checklist](docs/demo/V4_INTERVIEW_DEMO_CHECKLIST.md) gives the 5 / 10 / 10 / 5 minute interview
  walkthrough, likely questions, answer points, and fallback paths.
- [V4 Project Highlights](docs/demo/V4_PROJECT_HIGHLIGHTS.md) summarizes the V4 technical stack, quality gates,
  current capabilities, and future work.
- [V4 RAG Demo Script](docs/demo/V4_RAG_DEMO_SCRIPT.md) is the local interview / project review demo for HYBRID
  policy evidence, ToolCallTrace, Workspace, Execution Tree, and RAG evaluation.
- [V4 Policy Ingestion Pipeline](docs/demo/V4_POLICY_INGESTION_PIPELINE.md) explains the offline ingestion foundation
  and future live-provider path.
- [V4 PGvector Local Setup](docs/demo/V4_PGVECTOR_LOCAL_SETUP.md) documents the optional local PGvector profile.
- [Evaluation Docs](docs/evaluation/EVALUATION.md) describe V2.9 Agent evaluation and V4.6.1 RAG retrieval
  evaluation.
- [OpenAPI Docs](docs/api/OPENAPI.md) describe Swagger UI, `/v3/api-docs`, core API groups, `/actuator/health`, and
  the V4.6.4 no-runtime-change boundary.
- [V4 Final Completion Record](version-updates/EXEC_PLAN_V4_FINAL_COMPLETION_RECORD.md) closes V4.0 through
  V4.7.4 with architecture boundaries, default offline validation, limitations, and future work.
- [V4 Release Summary](version-updates/V4_RELEASE_SUMMARY.md) gives the concise reviewer-facing V4 summary.
- V4 RAG health indicators expose offline readiness through `/actuator/health` without requiring API keys, Docker,
  PGvector, PostgreSQL, or live embedding providers.

The default V4 RAG demo does not require API keys, Docker, or PGvector. Optional live provider and PGvector paths are
configured separately and are not part of the default local interview / project review demo. The demo is for local
development and project explanation, not production deployment.

### V4.1 Tool / Skill Foundation

Implemented V4.1 foundation:

- `AgentSkill`, `SkillDefinition`, `SkillRegistry`, `SkillExecutionContext`, and `SkillExecutionResult`;
- `SpecialistHandlerSkillAdapter` for wrapping existing Specialist Handlers without changing their ToolRegistry path;
- Skill definitions for return eligibility, exchange recommendation, coupon consultation, logistics analysis, general
  consultation, and human approval routing;
- Skill risk validation so a Skill cannot claim a lower risk than its required tools;
- Architecture checks that prevent Skill code from depending directly on repositories, Spring Web, LLM infrastructure,
  Spring AI, vector/RAG infrastructure, or concrete tool executors.

V4.1 does not implement Spring AI, RAG, PGvector, policy ingestion, Execution Tree skill nodes, or full runtime migration
from `SpecialistAgentHandlerRegistry` to `SkillRegistry`. `plannedSkills` remains a documented future extension and is
not generated, parsed, or executed by default.

### V4.2 Spring AI Adapter

Implemented V4.2 adapter foundation:

- `spring-ai-chat` LLM provider routes through `SpringAiLlmClient`, then returns plain text to the existing
  `LlmAgentPlanner`;
- provider output still passes through `AgentPlanParser` and `AgentPlanValidator`;
- Spring AI `ChatClient` is kept inside `agent.infrastructure.springai` and is not exposed to Agent, Handler, Skill,
  ToolRegistry, Repository, or domain code;
- `EmbeddingClient`, `FakeEmbeddingClient`, and `SpringAiEmbeddingClient` establish the embedding provider boundary
  for later RAG work;
- default configuration disables Spring AI model auto-creation with `spring.ai.model.*=none` unless explicitly enabled;
- live Spring AI smoke tests are opt-in and do not create tickets, AgentRuns, traces, vector stores, or database rows.

V4.2 does not implement RAG, VectorStore, PGvector, policy ingestion, Spring AI tool/function calling, or any direct
tool execution by the provider.

Spring AI chat live example:

```powershell
$env:AFTERSALE_LLM_PROVIDER="spring-ai-chat"
$env:SPRING_AI_ENABLED="true"
$env:SPRING_AI_CHAT_ENABLED="true"
$env:SPRING_AI_MODEL_CHAT="openai"
# Set SPRING_AI_OPENAI_API_KEY in your local shell before running.
$env:SPRING_AI_OPENAI_CHAT_OPTIONS_MODEL="gpt-4.1-mini"
mvn test "-Dtest=SpringAiLlmClientLiveSmokeTest" "-Dlive.spring-ai=true" "-Dlive.llm=true"
```

Spring AI embedding live example:

```powershell
$env:SPRING_AI_ENABLED="true"
$env:SPRING_AI_EMBEDDING_ENABLED="true"
$env:SPRING_AI_MODEL_EMBEDDING="openai"
# Set SPRING_AI_OPENAI_API_KEY in your local shell before running.
mvn test "-Dtest=SpringAiEmbeddingClientLiveSmokeTest" "-Dlive.spring-ai=true" "-Dlive.embedding=true"
```

Spring AI provider only supplies planner text or embedding vectors through project-owned adapters. Project tools are
still executed only by Java through `ToolRegistry`; do not register `ToolRegistry` tools as Spring AI tool callbacks.

### V4.3.1 PostgreSQL / PGvector Profile Boundary

Implemented V4.3.1 dependency and profile boundary:

- PostgreSQL JDBC driver is available for later opt-in PGvector work;
- `agent.rag.vector-store.pgvector.*` properties are default-off in `application.yml`;
- `application-rag-postgres.yml` defines the explicit `rag-postgres` profile;
- `PgVectorProperties` and `PgVectorProfileGuard` validate opt-in configuration without creating a PostgreSQL
  `DataSource`, `JdbcTemplate`, Spring AI `VectorStore`, schema, repository, or database connection;
- MySQL profile remains separate and is not polluted by PGvector properties;
- architecture checks prevent Agent, Handler, and Skill layers from depending directly on PGvector, VectorStore,
  `DataSource`, or `JdbcTemplate`.

Opt-in `rag-postgres` configuration shape:

```powershell
$env:AFTERSALE_RAG_ENABLED="true"
$env:AFTERSALE_VECTOR_STORE_PROVIDER="pgvector"
$env:AFTERSALE_PGVECTOR_ENABLED="true"
$env:AFTERSALE_PGVECTOR_URL="jdbc:postgresql://localhost:5433/after_sale_agent_rag"
$env:AFTERSALE_PGVECTOR_USERNAME="aftersale_rag"
$env:AFTERSALE_PGVECTOR_PASSWORD="你的本地 PostgreSQL 密码"
$env:AFTERSALE_PGVECTOR_SCHEMA="public"
$env:AFTERSALE_EMBEDDING_DIMENSION="1536"
```

V4.3.1 does not implement policy schema, VectorStore repository, similarity search, policy ingestion, hybrid RAG,
or PostgreSQL Docker Compose service.

### V4.3.2 Vector Schema / Repository Contract

Implemented V4.3.2 schema and contract boundary:

- `schema-rag-postgres.sql` defines `policy_documents`, `policy_chunks`, and `policy_embeddings` for the future
  opt-in `rag-postgres` path;
- schema includes PGvector extension setup, primary keys, foreign keys, uniqueness constraints, retrieval indexes, and
  a default `vector(1536)` column aligned with the configurable embedding dimension boundary;
- `PolicyDocument`, `PolicyChunk`, `PolicyEmbedding`, `VectorSearchQuery`, `VectorSearchResult`, and
  `VectorSearchMatch` are pure domain models with no Spring, JDBC, PGvector, or Spring AI dependency;
- `PolicyVectorRepository` is an interface-only contract for later fake and JDBC implementations;
- schema harness, domain model, repository contract, and architecture tests run without PostgreSQL, PGvector, Docker,
  MySQL, Redis, real LLMs, API keys, or external network.

V4.3.2 does not implement JDBC repository, Spring AI `VectorStore` search, EmbeddingClient calls, Policy Ingestion,
RAG / HYBRID retrieval, fake vector store, PostgreSQL Docker Compose service, or `search_aftersale_policy` behavior
changes. V4.3.3 remains the fake vector store / default offline vector test phase; V4.3.4 remains Docker Compose /
opt-in integration docs; V4.4 remains Policy Ingestion; V4.5 remains HYBRID RAG tool integration.

### V4.3.3 Fake Vector Store / Default Offline Vector Tests

Implemented V4.3.3 fake vector repository boundary:

- `CosineSimilarityCalculator` provides deterministic cosine similarity for evidence scores;
- `InMemoryPolicyVectorRepository` implements `PolicyVectorRepository` for save / find / search contract tests;
- repository search supports `topK`, `minScore`, category, productType, effectiveAt, and embeddingModel filters;
- duplicate document / chunk / embedding writes are rejected clearly;
- fake provider wiring is opt-in with `agent.rag.vector-store.provider=fake`;
- default tests do not create PostgreSQL `DataSource`, `JdbcTemplate`, Spring AI `VectorStore`, real embedding
  provider, or real LLM beans.

V4.3.3 does not implement JDBC repository, PGvector live search, Spring AI `VectorStore` search, EmbeddingClient calls,
Policy Ingestion, RAG / HYBRID retrieval, PostgreSQL Docker Compose service, or `search_aftersale_policy` behavior
changes. V4.3.4 remains Docker Compose / opt-in integration docs; V4.4 remains Policy Ingestion; V4.5 remains HYBRID
RAG tool integration.

### V4.3.4 PGvector Local Development Compose

Implemented V4.3.4 opt-in local development boundary:

- `docker-compose-rag.yml` starts a local development only PGvector-capable PostgreSQL service named `pgvector`;
- `.env.rag.example` documents placeholder local settings for the `rag-postgres` profile and compose service;
- `docs/demo/V4_PGVECTOR_LOCAL_SETUP.md` documents start, stop, cleanup, health check, schema initialization, and FAQ
  steps;
- `schema-rag-postgres.sql` is mounted into `/docker-entrypoint-initdb.d/01-schema-rag-postgres.sql` for new local
  PGvector volumes;
- default `docker-compose.yml` app + MySQL path does not depend on PGvector;
- compose/docs harness tests verify the opt-in boundary, secret safety, default compose non-regression, and docs
  non-goals.

Start local PGvector:

```bash
docker compose -f docker-compose-rag.yml up -d
```

Stop or clean the local PGvector volume:

```bash
docker compose -f docker-compose-rag.yml down
docker compose -f docker-compose-rag.yml down -v
```

Health and schema checks:

```bash
docker compose -f docker-compose-rag.yml ps
docker compose -f docker-compose-rag.yml exec pgvector pg_isready -U aftersale_rag -d after_sale_agent_rag
docker compose -f docker-compose-rag.yml exec pgvector psql -U aftersale_rag -d after_sale_agent_rag -c "\\dt"
```

RAG profile environment variables:

```text
AFTERSALE_RAG_ENABLED=true
AFTERSALE_VECTOR_STORE_PROVIDER=pgvector
AFTERSALE_PGVECTOR_ENABLED=true
AFTERSALE_PGVECTOR_URL=jdbc:postgresql://localhost:5433/after_sale_agent_rag
AFTERSALE_PGVECTOR_USERNAME=aftersale_rag
AFTERSALE_PGVECTOR_PASSWORD=aftersale_rag
AFTERSALE_PGVECTOR_SCHEMA=public
AFTERSALE_EMBEDDING_DIMENSION=1536
```

For an app running inside the same Docker Compose network, use
`jdbc:postgresql://pgvector:5432/after_sale_agent_rag`. Local `.env` files must not be committed.

V4.3.4 does not add a `JdbcPolicyVectorRepository`, Policy Ingestion, HYBRID retrieval, RAG runtime, Spring AI
`VectorStore` usage, EmbeddingClient calls, or `search_aftersale_policy` vector wiring. Default validation does not
start Docker and does not require PostgreSQL, PGvector, MySQL, Redis, real LLMs, API keys, embedding providers, or
external network.

### V4.4.1 Policy Ingestion Domain Model

Implemented V4.4.1 ingestion foundation:

- `PolicyIngestionRun`, `PolicyIngestionStatus`, `PolicyIngestionSource`, `PolicyIngestionDocument`,
  `PolicyIngestionChunk`, and `PolicyIngestionError` define the admin/pipeline ingestion domain model;
- `PolicyIngestionStateMachine` validates CREATED, RUNNING, CHUNKED, EMBEDDING, COMPLETED, FAILED,
  PARTIALLY_FAILED, and CANCELLED transitions, with terminal states locked;
- `PolicyIngestionRepository` defines the ingestion run/document/chunk/error contract separately from
  `PolicyVectorRepository`;
- `InMemoryPolicyIngestionRepository` provides default offline persistence for tests without PostgreSQL, PGvector,
  Docker, MySQL, Redis, real LLMs, API keys, embedding providers, or external network;
- ingestion error text is sanitized and bounded so API keys, passwords, tokens, prompts, and local paths are not
  persisted in error details;
- architecture tests keep ingestion domain pure and prevent Agent, Handler, and Skill layers from depending on
  ingestion repositories or memory infrastructure.

V4.4.1 does not implement chunking, checksum deduplication, embedding pipeline, vector repository writes,
`JdbcPolicyIngestionRepository`, `JdbcPolicyVectorRepository`, Admin ingestion API, ingestion tools, RAG / HYBRID
retrieval, or `search_aftersale_policy` behavior changes. Policy Ingestion remains an admin/pipeline capability, not
an Agent runtime tool. V4.4.2 handles chunking and checksum dedup, V4.4.3 handles embedding pipeline with fake provider,
V4.4.4 handles ingestion docs / completion record, and V4.5 wires HYBRID RAG into `search_aftersale_policy`.

### V4.4.2 Chunking and Checksum Dedup

Implemented V4.4.2 ingestion processing boundary:

- `PolicyChunkingOptions`, `PolicyChunkingStrategy`, `PolicyChunkingResult`, and `PolicyChunkingService` provide
  deterministic chunking for `PolicyIngestionDocument.rawText`;
- chunk index starts at `0`, overlap is supported, paragraph boundaries are preferred when safe, and token estimate is
  `ceil(chars / tokenEstimateDivisor)`;
- `PolicyContentChecksumService` computes SHA-256 checksums for document raw text and chunk content using line-ending
  normalization plus trim;
- `PolicyIngestionDedupService` returns `NEW_CONTENT`, `DUPLICATE_DOCUMENT`, or `DUPLICATE_CHUNK` from repository
  checksum lookups without exposing raw text in reasons;
- `PolicyIngestionRepository` and `InMemoryPolicyIngestionRepository` now support checksum query methods for default
  offline dedup tests;
- architecture tests keep ingestion application services away from Spring Web, JDBC, DataSource, Spring AI,
  VectorStore, PGvector infrastructure, business repositories, ToolRegistry bypasses, Handler, and Skill packages.

V4.4.2 does not call `EmbeddingClient`, does not call Spring AI, does not write `PolicyVectorRepository`, does not
implement `JdbcPolicyIngestionRepository` or `JdbcPolicyVectorRepository`, does not add Admin ingestion API or Agent
ingestion tools, does not implement RAG / HYBRID retrieval, and does not change `search_aftersale_policy`. Policy
Ingestion remains an admin/pipeline capability. V4.4.3 handles embedding pipeline with fake provider, and V4.5 wires
HYBRID RAG into `search_aftersale_policy`.

### V4.4.3 Embedding Pipeline with Fake Provider

Implemented V4.4.3 fake-provider embedding boundary:

- `PolicyEmbeddingPipelineOptions`, `PolicyEmbeddingPipelineResult`, `PolicyEmbeddingPipelineFailure`, and
  `PolicyEmbeddingPipelineService` define the offline embedding pipeline;
- the pipeline reads ingestion run/document/chunk state, calls the `EmbeddingClient` abstraction, and writes
  `PolicyDocument`, `PolicyChunk`, and `PolicyEmbedding` through the `PolicyVectorRepository` contract;
- default tests use `FakeEmbeddingClient` and `InMemoryPolicyVectorRepository`, then verify direct repository search
  can find the saved evidence chunk;
- pipeline result handling covers expected dimension checks, duplicate embedding skip/fail behavior,
  `maxChunksPerRun`, partial failure, all failure, and sanitized failure details;
- ingestion run status moves from CHUNKED to EMBEDDING and then to COMPLETED, PARTIALLY_FAILED, or FAILED;
- architecture tests keep the pipeline away from Spring AI adapter classes, Spring AI `VectorStore`, JDBC, DataSource,
  PGvector infrastructure, vector memory infrastructure, business repositories, Tool, Handler, and Skill packages.

V4.4.3 does not call a real Spring AI `EmbeddingModel`, does not call `SpringAiEmbeddingClient` in default tests, does
not call Spring AI `VectorStore`, does not implement `JdbcPolicyIngestionRepository` or `JdbcPolicyVectorRepository`,
does not connect PostgreSQL / PGvector, does not add Admin ingestion API or Agent ingestion tools, does not implement
RAG / HYBRID retrieval, and does not change `search_aftersale_policy`. Policy Ingestion remains an admin/pipeline
capability. V4.4.4 handles ingestion docs / final V4.4 completion record, and V4.5 wires HYBRID RAG into
`search_aftersale_policy`.

### V4.4.4 Policy Ingestion Docs / Completion Record

Implemented V4.4.4 documentation closeout:

- `docs/demo/V4_POLICY_INGESTION_PIPELINE.md` describes the V4.4 ingestion foundation, offline example, failure
  handling, security boundary, and future real-provider path;
- `version-updates/EXEC_PLAN_V4_POLICY_INGESTION_FOUNDATION.md` records the total V4.4 completion signal;
- V4 roadmap, RAG contract, vector-store decision, and quality summary now mark V4.4.1 through V4.4.4 completed.

V4.4 is an ingestion foundation, not a production ingestion API. It does not add an Admin Controller,
`ingest_policy_document` tool, ToolRegistry wiring, real Spring AI embedding default path, JDBC repositories,
PGvector live writes, RAG / HYBRID retrieval, AgentRun runtime usage, or `search_aftersale_policy` vector wiring.
Default tests remain offline and do not require PostgreSQL, PGvector, Docker, MySQL, Redis, API keys, real LLMs, real
embedding providers, or external network. V4.5 is the phase that wires HYBRID RAG into `search_aftersale_policy`.

### V4.5.1 RAG Search Contract

Implemented V4.5.1 contract preparation:

- `RetrievalMode` defines `KEYWORD`, `VECTOR`, and `HYBRID`, with default mode `KEYWORD`;
- `RagPolicySearchQuery` defines bounded query options for future RAG policy search;
- `RagPolicyEvidence`, `RagPolicyEvidenceSource`, and `RagPolicySearchResult` define evidence-only retrieval output;
- keyword and vector mappers convert existing result models into RAG evidence without repository calls;
- docs and architecture tests record that this is schema preparation only.

V4.5.1 does not change `search_aftersale_policy` runtime, does not implement keyword + vector merge service, does not
call `EmbeddingClient`, does not call `PolicyVectorRepository.search`, does not connect PostgreSQL / PGvector, and does
not modify AgentRun, ToolCallTrace, AgentWorkspace, Skill runtime, ToolRegistry, or Execution Tree. `search_aftersale_policy`
remains a LOW-risk read-only tool and RAG output remains evidence only. V4.5.2 handles keyword + vector merge service,
V4.5.3 handles HYBRID mode wiring into `search_aftersale_policy`, and V4.5.4 handles ToolCallTrace / Workspace evidence
wiring.

### V4.5.2 Keyword + Vector Merge Service

Implemented V4.5.2 merge preparation:

- `RagPolicyEvidenceMergeOptions` defines bounded topK, minScore, keyword/vector weights, tie behavior, and dedup flags;
- `RagPolicyEvidenceMergeService` merges supplied KEYWORD and VECTOR evidence into HYBRID evidence;
- score merge is deterministic, normalized to 0.0-1.0, and keeps keywordScore/vectorScore as retrieval evidence scores;
- dedup supports chunkId, policyId, and normalized snippet matching;
- fallback behavior covers keyword-only, vector-only, both-empty, and null-input merge cases.

V4.5.2 does not change `search_aftersale_policy` runtime, does not call `EmbeddingClient`, does not call
`PolicyVectorRepository.search`, does not access keyword or vector repositories, does not connect PostgreSQL /
PGvector, and does not modify ToolRegistry, ToolCallTrace, AgentWorkspace, AgentRun, Skill runtime, or Execution Tree.
RAG output remains evidence only. V4.5.3 handles HYBRID mode runtime wiring, and V4.5.4 handles ToolCallTrace /
Workspace evidence wiring.

### V4.5.3 search_aftersale_policy HYBRID Runtime

Implemented V4.5.3 runtime wiring:

- `search_aftersale_policy` supports `retrievalMode` values `KEYWORD`, `VECTOR`, and `HYBRID`;
- old input without `retrievalMode` still defaults to KEYWORD and keeps legacy `results` output compatibility;
- KEYWORD mode uses existing deterministic keyword policy retrieval;
- VECTOR mode uses the `EmbeddingClient` abstraction and `PolicyVectorRepository.search` contract when available;
- HYBRID mode combines keyword and vector evidence through `RagPolicyEvidenceMergeService`;
- default VECTOR / HYBRID tests use `FakeEmbeddingClient` and `InMemoryPolicyVectorRepository`.

V4.5.3 does not connect real PostgreSQL / PGvector, does not implement `JdbcPolicyVectorRepository`, does not call a
real Spring AI EmbeddingModel in default tests, and does not call Spring AI `VectorStore`. `search_aftersale_policy`
remains a LOW-risk read-only tool, does not need approval, and returns evidence only; it does not execute refunds,
exchanges, coupon compensation, payment changes, logistics changes, or dispute closure. V4.5.4 now completes
ToolCallTrace / Workspace evidence visibility. Default tests remain offline and do not require real LLMs, API keys, PostgreSQL,
PGvector, Docker, MySQL, Redis, real embedding providers, or external network.

### V4.5.4 ToolCallTrace / Workspace Evidence Wiring

Implemented V4.5.4 evidence observability:

- `search_aftersale_policy` output keeps legacy `results` compatibility and exposes stable RAG `evidences`,
  `retrievalMode`, `fallbackUsed`, `totalKeywordMatches`, and `totalVectorMatches` fields for ToolCallTrace JSON;
- `AgentWorkspace.PolicyEvidence` stores single-AgentRun policy evidence summaries with RAG identifiers, score,
  retrievalMode, and source when available;
- AgentRun final summary includes concise policy evidence references instead of full evidence JSON or long chunk text;
- Execution Tree read-only output can display policy evidence summaries and associate them with subtask/tool call
  metadata when available.

V4.5.4 does not change the KEYWORD / VECTOR / HYBRID retrieval algorithms, does not change ToolCallTrace table schema,
does not connect real PostgreSQL / PGvector, does not implement `JdbcPolicyVectorRepository`, does not call real Spring
AI EmbeddingModel, and does not call Spring AI `VectorStore`. `search_aftersale_policy` remains a LOW-risk read-only
tool, RAG evidence remains evidence only, and no refund, exchange, coupon compensation, payment, logistics, or dispute
closure action is executed by retrieval evidence. Default tests remain offline and do not require real LLMs, API keys,
PostgreSQL, PGvector, Docker, MySQL, Redis, real embedding providers, or external network. Real PGvector and real
embedding providers remain opt-in / future paths. V4.6 can continue with evaluation, demo, and Spring Boot
completeness work.

### V4.6.1 RAG Evaluation Cases and Metrics

Implemented V4.6.1 offline retrieval evaluation:

- `docs/evaluation/rag_policy_cases.jsonl` defines 15 deterministic RAG policy cases for KEYWORD / VECTOR / HYBRID
  retrieval.
- The RAG evaluation runner measures evidence recall, evidence source coverage, retrieval mode correctness, fallback
  accuracy, empty-result accuracy, citation completeness, safety, and average evidence count.
- The runner uses fake / in-memory dependencies: `FakeEmbeddingClient`, `InMemoryPolicyVectorRepository`, and
  in-memory keyword policy data.
- V2.9 evaluation still evaluates Agent planning; V4.6.1 evaluates policy evidence retrieval.

V4.6.1 does not add runtime features, does not modify `search_aftersale_policy` retrieval logic, does not use
LLM-as-judge, does not create Ticket / AgentRun / ToolCallTrace / Workspace / Execution Tree state, and does not call
real LLMs, real embedding providers, Spring AI, PostgreSQL, PGvector, Docker, MySQL, Redis, API keys, raw datasets, or
external network.

### V4.6.2 V4 RAG Demo Script

Implemented V4.6.2 demo documentation:

- `docs/demo/V4_RAG_DEMO_SCRIPT.md` provides a curl-oriented walkthrough for app startup, ticket creation, AgentRun,
  ToolCallTrace, Execution Tree, and RAG evaluation.
- Scenario A documents the `search_aftersale_policy` HYBRID ToolRegistry input/output shape with short expected
  evidence snippets.
- Scenario B shows AgentRun policy evidence visibility in final summary, ToolCallTrace output JSON, and
  AgentWorkspace policy evidence summary.
- Scenario C shows the read-only Execution Tree evidence view.
- Scenario D links V4.6.1 RAG evaluation and expected deterministic metrics.
- Docs harness tests verify demo script content, README and evaluation links, completion record, offline boundary,
  evidence-only boundary, and secret / local-path safety.

V4.6.2 only adds demo script / expected output / docs harness coverage. It does not add runtime behavior, does not
modify `search_aftersale_policy`, does not change retrieval algorithms, does not change ToolRegistry semantics, does
not change ToolCallTrace schema, does not change Workspace writing, and does not change Execution Tree runtime. The
default demo remains offline and does not require real LLMs, API keys, PostgreSQL, PGvector, Docker, MySQL, Redis, real
embedding providers, or external network.

### V4.6.3 Actuator Health Indicators for RAG Components

Implemented V4.6.3 offline readiness diagnostics:

- `/actuator/health` includes RAG search, vector-store, embedding, and ingestion health components when RAG health is
  enabled.
- RAG search health reports the configured search service and supported retrieval modes without executing a search.
- Vector-store health reports `none`, `fake`, or `pgvector` configuration status without opening a database connection
  and without running vector similarity search.
- Embedding health reports disabled / fake / Spring AI configuration readiness without calling `EmbeddingClient` or a
  real Spring AI embedding provider.
- Ingestion health reports ingestion contract readiness without reading files, chunking content, embedding text, or
  writing repositories.
- Health details are disabled by default and, when enabled, report only sanitized configuration signals such as
  `configured=true/false`; secrets and local paths are not exposed by RAG health details.

V4.6.3 does not add runtime business behavior, does not modify `search_aftersale_policy`, does not change retrieval
algorithms, does not change the RAG evaluation runner, and does not change ToolCallTrace, Workspace, or Execution Tree
runtime semantics. Health is an offline readiness signal, not proof of live PGvector or live provider connectivity. The
default path still does not require real LLMs, API keys, PostgreSQL, PGvector, Docker, MySQL, Redis, real embedding
providers, or external network.

### V4.6.4 OpenAPI / API Docs Polish

Implemented V4.6.4 API documentation polish:

- `springdoc-openapi` exposes `/v3/api-docs` and Swagger UI at `/swagger-ui/index.html` / `/swagger-ui.html`.
- OpenAPI metadata describes the AfterSale-Agent API, ToolRegistry-controlled tools, approval-gated high-risk actions,
  RAG policy evidence retrieval, evidence-only outputs, and the default offline demo path.
- Existing Ticket, AgentRun, Approval, ToolCallTrace, Execution Tree, and platform health APIs are annotated for docs.
- `docs/api/OPENAPI.md` explains local usage, core API groups, RAG evidence boundaries, and actuator health boundaries.
- `/actuator/health` remains the only actuator endpoint exposed by default.

V4.6.4 does not add business runtime behavior, does not add a public policy-search controller, does not modify
`search_aftersale_policy`, does not change retrieval algorithms, and does not modify RAG health, evaluation,
ToolCallTrace, Workspace, or Execution Tree runtime. OpenAPI docs do not require live providers, API keys, Docker,
PostgreSQL, PGvector, MySQL, Redis, real LLMs, real embedding providers, or external network. OpenAPI docs are for
local development and review; they are not a production deployment guide.

### V4.7.1 Documentation Consistency / Secret Safety Audit

Implemented V4.7.1 documentation audit:

- V4 roadmap, active plan, README, quality docs, agent contracts, decision docs, demo docs, evaluation docs, API docs,
  and V4 completed plans were checked for current implementation boundaries.
- Documentation keeps future capabilities such as Admin ingestion APIs, production monitoring, production deployment,
  and live PGvector / live embedding validation separate from the default completed path.
- V4 docs continue to state that RAG evidence is policy evidence only, `search_aftersale_policy` is LOW-risk
  read-only, ToolRegistry remains the Agent tool execution boundary, and Skill does not replace ToolRegistry.
- Docs consistency harness tests check V4 completion records, secret / local-path safety, evidence-only wording,
  default offline testing, and no completed-action overclaims.

V4.7.1 does not add runtime behavior, does not modify `search_aftersale_policy`, does not change retrieval algorithms,
does not change ToolRegistry, ToolCallTrace, Workspace, Execution Tree, RAG evaluation, Actuator health, or OpenAPI
runtime behavior, and does not require real LLMs, API keys, PostgreSQL, PGvector, Docker, MySQL, Redis, real embedding
providers, or external network.

### V4.7.2 Architecture Boundary / Offline Validation Closure

Implemented V4.7.2 architecture and default offline validation closure:

- ArchitectureTest now closes additional Agent / Handler / Skill, Tool executor, diagnostics, OpenAPI, RAG, ingestion,
  and provider-infrastructure dependency boundaries.
- `DefaultOfflineValidationTest` verifies the default Spring context starts without `DataSource`, PGvector profile
  guard, Spring AI model, VectorStore, or live provider gateway beans.
- `DefaultOfflineValidationTest` also verifies `/actuator/health` remains available, does not expose broad actuator
  endpoints, and reports offline-readiness details without live provider checks.
- `LiveTestSkipClosureTest` checks live smoke tests remain gated by explicit system properties and credential or
  environment assumptions.
- [Validation Commands](docs/quality/VALIDATION_COMMANDS.md) records default offline commands, live opt-in examples,
  and failure handling rules.

V4.7.2 does not add runtime behavior, does not modify `search_aftersale_policy`, does not change retrieval algorithms,
does not change RAG evaluation, Actuator health behavior, OpenAPI behavior, ToolRegistry, ToolCallTrace, Workspace, or
Execution Tree runtime. Default validation remains offline and does not require real LLMs, API keys, PostgreSQL,
PGvector, Docker, MySQL, Redis, real embedding providers, or external network.

### V4.7.3 Interview Demo / README Polish

Implemented V4.7.3 interview-facing documentation polish:

- [V4 Interview Demo Checklist](docs/demo/V4_INTERVIEW_DEMO_CHECKLIST.md) provides the recommended project review
  sequence, pre-demo checks, common interview questions, suggested answer points, and fallback paths.
- [V4 Project Highlights](docs/demo/V4_PROJECT_HIGHLIGHTS.md) summarizes the V4 technology stack, RAG / Tool / Skill
  capabilities, quality gates, and explicit future work.
- README, RAG demo docs, OpenAPI docs, validation command docs, V4 plans, and quality notes now link the interview
  materials and keep default offline validation commands visible.
- Docs harness tests verify interview docs, README links, evidence-only wording, and no completed-action or production
  deployment overclaims.

V4.7.3 is documentation polish only. It does not add runtime behavior, does not modify `search_aftersale_policy`, does
not change retrieval algorithms, does not change RAG evaluation, Actuator health behavior, OpenAPI behavior,
ToolRegistry, ToolCallTrace, Workspace, or Execution Tree runtime. The interview demo path remains offline by default
and does not require real LLMs, API keys, PostgreSQL, PGvector, Docker, MySQL, Redis, real embedding providers, or
external network.

### V4.7.4 V4 Final Completion Record

Implemented V4.7.4 final V4 documentation closure:

- [V4 Final Completion Record](version-updates/EXEC_PLAN_V4_FINAL_COMPLETION_RECORD.md) summarizes V4.0
  through V4.7.4, key capabilities, architecture boundaries, default offline validation, known limitations,
  recommended demo path, and future work.
- [V4 Release Summary](version-updates/V4_RELEASE_SUMMARY.md) gives reviewers a concise summary of V4 delivery,
  validation, demo path, non-production boundaries, and roadmap.
- V4 roadmap, historical active plan, quality notes, validation commands, interview checklist, and project highlights
  now point to the final record and release summary.
- Final docs harness tests verify final status, links, default offline claims, RAG evidence-only wording,
  ToolRegistry boundary, live opt-in boundary, and no production / real external integration overclaims.

V4.7.4 is documentation closure only. It does not add runtime behavior, does not modify `search_aftersale_policy`, does
not change retrieval algorithms, does not change RAG evaluation, Actuator health behavior, OpenAPI behavior,
ToolRegistry, ToolCallTrace, Workspace, or Execution Tree runtime. V4 completed means the enterprise-grade Agent
platform foundation, RAG policy evidence path, and Spring Boot completeness documentation are closed; it does not mean
production external systems are integrated.

### V4 Default Test Boundary

Default validation remains:

```bash
mvn test
mvn checkstyle:check
mvn spotbugs:check
mvn test -Dtest=ArchitectureTest
```

Default validation must not require real LLMs, API keys, PostgreSQL, PGvector, Docker, MySQL, Redis, or external network.

### V4 Non-goals

V4 does not implement real refund, real exchange, real payment, real logistics, real inventory mutation, real coupon compensation, production authentication, microservices, or a LangChain sidecar main path.
