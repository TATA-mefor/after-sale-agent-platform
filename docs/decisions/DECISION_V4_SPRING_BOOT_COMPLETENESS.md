
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
