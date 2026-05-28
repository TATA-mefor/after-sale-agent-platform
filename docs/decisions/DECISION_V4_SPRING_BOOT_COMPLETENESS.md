
# Decision: V4 Spring Boot Completeness

Date: 2026-05-22
Status: Accepted

## Context

V3 已完成 MySQL profile、Docker Compose、structured logging 和 final review。V4 将引入 Spring AI、RAG、PGvector、Policy Ingestion 和 Skill Layer。为了让项目具备更强的面试展示力，V4 需要补齐 Spring Boot 企业级工程完整性，但不能破坏默认离线测试和既有 Agent 边界。

## Decision

V4 的 Spring Boot 完整性围绕 RAG / Skill 能力补齐，不做泛泛堆功能。

优先项：

```text
ConfigurationProperties
Flyway or Liquibase migration
Actuator HealthIndicator
OpenAPI / springdoc
Minimal Security
Opt-in integration tests
Docker Compose local rag profile
```

## Boundaries

- ConfigurationProperties 管理 provider、embedding、vector store、ingestion、security 和 live test 配置；
- migration 必须区分 MySQL 和 PostgreSQL / PGvector；
- health indicator 不得调用昂贵 embedding 或 LLM；
- OpenAPI 只描述 HTTP API，不暴露 secrets；
- Security 最小实现应保护 admin ingestion 和 approval APIs；
- integration tests 必须显式 opt-in。

## Non-goals

- 不做完整 OAuth2 / SSO；
- 不做 Kubernetes；
- 不做生产级 secret manager；
- 不做多租户权限系统；
- 不把 Docker Compose 作为 production deployment；
- 不让默认 `mvn test` 依赖外部服务。

## Consequences

Positive:

- Spring Boot 项目完整度更强；
- RAG 和 provider 配置更可控；
- API 和 health 更适合演示；
- migration 降低 schema 漂移风险。

Costs:

- 需要新增依赖和配置；
- 需要维护 profile / migration / security 测试；
- 需要避免默认 test gate 被 integration test 污染。

## V4.6.3 Health Indicator Completion

V4.6.3 implements RAG health indicators as offline readiness diagnostics:

- RAG search health checks whether the RAG search service is present and reports supported KEYWORD / VECTOR / HYBRID
  modes without executing search.
- Vector-store health reports provider configuration (`none`, `fake`, or `pgvector`) without opening a PostgreSQL /
  PGvector connection and without executing vector search.
- Embedding health reports disabled / fake / Spring AI readiness without calling `EmbeddingClient` or a real Spring AI
  embedding provider.
- Ingestion health reports ingestion contract readiness without reading files, chunking content, embedding text, or
  writing repositories.

Health indicators are not live connectivity checks and do not prove production provider availability. Details are off by
default; when enabled, they expose sanitized readiness details only and do not expose secrets, local paths, prompts, raw
text, or full credential-bearing URLs.

## V4.6.4 OpenAPI Completion

V4.6.4 implements OpenAPI / Swagger UI documentation polish:

- `springdoc-openapi` exposes `/v3/api-docs` and Swagger UI for local development and review.
- OpenAPI metadata describes Ticket, AgentRun, Approval, ToolCallTrace, Execution Tree, platform health, ToolRegistry
  control, approval-gated high-risk actions, RAG policy evidence retrieval, and the default offline demo path.
- `docs/api/OPENAPI.md` documents local entry points, API groups, RAG evidence-only boundaries, and Actuator health
  boundaries.
- `/actuator/health` remains the only default actuator exposure.

V4.6.4 does not add runtime business behavior, does not add authentication, does not add a public policy-search
controller, does not modify `search_aftersale_policy`, and does not change RAG health, evaluation, ToolCallTrace,
Workspace, or Execution Tree behavior. OpenAPI docs do not expose secrets, raw prompts, raw datasets, local paths, or
credential-bearing provider configuration. They are not a production deployment guide.
