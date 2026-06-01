# Validation Commands

Status: V4 final default offline validation gate.

This file records the repository validation boundary. The default commands are offline, deterministic, and must not
require real providers or external services.

## Default Commands

Run these commands for normal development and review:

```bash
mvn test
mvn checkstyle:check
mvn spotbugs:check
mvn test -Dtest=ArchitectureTest
```

The default validation path must not require:

- real LLM;
- API Key;
- PostgreSQL;
- PGvector;
- Docker;
- MySQL;
- Redis;
- external network;
- real embedding provider;
- Spring AI live provider calls.

If any default command requires one of those dependencies, treat it as a regression.

## V4 Final Validation

The V4 final default validation gate is:

```bash
mvn test
mvn checkstyle:check
mvn spotbugs:check
mvn test -Dtest=ArchitectureTest
```

This gate is the expected final verification set for V4 completion. It covers default offline tests, docs harness
tests, architecture boundaries, style checks, static analysis, RAG evaluation tests, RAG health tests, OpenAPI docs
tests, and AgentRun regression tests.

Live checks are explicit opt-in and are not part of this default gate. The final V4 default gate does not require API
keys, PostgreSQL, PGvector, Docker, MySQL, Redis, real LLMs, real embedding providers, Spring AI VectorStore, or
external network.

## 项目审查后的事实口径

V4 final default validation gate 证明默认路径离线、确定性、边界可检查；它不证明生产部署、生产认证、生产监控、
live PGvector、真实 provider 或真实外部业务系统可用。

- 当前 observability 已覆盖 MDC / structured logs、ToolCallTrace、Execution Tree、Actuator health 和 RAG readiness
  diagnostics。
- 当前 observability 未覆盖 Prometheus registry、metrics dashboard、distributed tracing 或 cross-service trace-id
  propagation；这些是 V5 / future work。
- 当前 Spring AI 是 adapter foundation，不是 ChatMemory、Advisors、Tool Calling API 或 bulk embedding 深度使用。
- 当前 RAG 支持 KEYWORD / VECTOR / HYBRID policy evidence retrieval，但没有 reranking、query rewriting、RRF 或
  chunk window expansion。
- 当前 API 是 demo/backend API surface，不是完整生产 CRUD 平台。
- PGvector 当前是 foundation / opt-in profile，不是默认 live vector persistence。

中文整改方案见 `docs/quality/PROJECT_REMEDIATION_PLAN.md`。该文档只做项目审查结论的事实核验与阶段化整改路线，
不改变 runtime 行为。对应 docs harness 可用以下命令单独验证：

```bash
mvn test -Dtest=ProjectRemediationPlanDocsTest
```

## Interview Safe Validation Commands

Use this command set before or during an interview when the goal is to show the repository can be verified locally
without live services:

```bash
mvn test
mvn checkstyle:check
mvn spotbugs:check
mvn test -Dtest=ArchitectureTest
```

These commands exercise unit, integration-style offline, docs harness, quality, and architecture checks. They are the
recommended path for interview demos because they do not require live LLMs, API keys, PostgreSQL, PGvector, Docker,
MySQL, Redis, real embedding providers, or external network. Live validation remains explicit opt-in only.

## Architecture Boundary Coverage

`ArchitectureTest` is the mechanical boundary gate for the modular monolith:

- Planner plans; it does not execute tools.
- ToolRegistry remains the Agent tool execution boundary.
- Skill composes safe capability steps and does not replace ToolRegistry.
- Agent, Handler, and Skill code do not directly depend on repositories, embedding clients, vector repositories,
  PGvector infrastructure, Spring AI model APIs, JDBC, DataSource, OpenAPI config, or Actuator health indicators.
- RAG retrieval is policy evidence only.
- Policy Ingestion is an admin/offline pipeline, not an Agent runtime tool.
- RAG evaluation, RAG health, OpenAPI docs, Workspace, ToolCallTrace, and Execution Tree keep separate boundaries.

## Default Offline Closure

Default Spring context validation checks that the app can start without live infrastructure:

- no `DataSource` bean is required;
- no PGvector live connection is created;
- no Spring AI `ChatModel`, `EmbeddingModel`, or `VectorStore` bean is required;
- no live chat or embedding gateway bean is created;
- `/actuator/health` is an offline readiness signal and does not execute live provider checks.

## Optional Live Validation

Live validation is explicit opt-in and is not part of default `mvn test`.

Examples:

```bash
mvn test -Dtest=LlmPlannerLiveSmokeTest -Dlive.llm=true
mvn test -Dtest=SpringAiLlmClientLiveSmokeTest -Dlive.spring-ai=true -Dlive.llm=true
mvn test -Dtest=SpringAiEmbeddingClientLiveSmokeTest -Dlive.spring-ai=true -Dlive.embedding=true
mvn test -Dtest=RealAgentValidationLiveTest -Dlive.llm=true -Dlive.mysql=true
```

PGvector validation remains an optional live path and must use an explicit opt-in flag if added or expanded later. It
must not run during default validation.

## Failure Handling

- If default validation tries to call a real LLM, real embedding provider, database, Docker, Redis, PGvector, or the
  external network, fix the default profile or test boundary.
- If live validation is missing provider credentials or database configuration, it should skip through assumptions or
  fail with a clear setup message.
- Do not commit real API keys, database passwords, tokens, local absolute paths, raw prompts, or raw datasets.

## V4.7.2 Boundary

V4.7.2 closes architecture and offline validation coverage only. It does not add runtime behavior, does not modify
`search_aftersale_policy`, does not change retrieval algorithms, does not modify RAG evaluation, Actuator health,
OpenAPI behavior, ToolRegistry, ToolCallTrace, Workspace, or Execution Tree runtime.

## V4.7.4 Final Closure Boundary

V4.7.4 closes final V4 documentation status only. It adds the final completion record and release summary, then keeps
the same default validation gate and live opt-in boundary. It does not add runtime behavior, does not modify
`search_aftersale_policy`, does not change retrieval algorithms, does not modify RAG evaluation, Actuator health,
OpenAPI behavior, ToolRegistry, ToolCallTrace, Workspace, or Execution Tree runtime.
