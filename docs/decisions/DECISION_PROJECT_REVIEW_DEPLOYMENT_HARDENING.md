# 部署加固路线决策

Date: 2026-06-01

Status: Completed

## Context

本决策回应项目审查中关于部署能力的评价：当前仓库已经具备本地开发、默认离线验证、Actuator health、
OpenAPI docs、MySQL profile 和 PGvector profile 说明，但仍不是生产部署完成态。

阶段 6 只做 deployment hardening decision / roadmap / docs harness test，不实现 Dockerfile、CI/CD、
Kubernetes、Helm、secret manager、production monitoring、production auth/RBAC、live PGvector validation 或
production external integrations。

## Current Deployment Baseline

当前已有部署相关基础：

- `docker-compose.yml`：本地 app + MySQL 开发路径。
- `docker-compose-rag.yml`：本地 PGvector infrastructure 调试路径。
- `.env.rag.example`：RAG 本地环境变量 placeholder 示例。
- `application-prod.example.yml`：生产配置模板，不会被默认 profile 加载。
- `application-mysql.yml`：显式 MySQL profile。
- `application-rag-postgres.yml`：显式 RAG / PostgreSQL / PGvector profile。
- Actuator health：默认 `/actuator/health`。
- OpenAPI docs：`/v3/api-docs` 和 Swagger UI。
- 默认验证命令：`mvn test`、`mvn checkstyle:check`、`mvn spotbugs:check`、
  `mvn test -Dtest=ArchitectureTest`。
- 默认测试离线：不需要 real LLM、API Key、PostgreSQL、PGvector、Docker、MySQL、Redis 或 external network。

## Current Gaps

当前明确未完成的部署能力：

- Dockerfile is not implemented。
- CI/CD is not implemented。
- Kubernetes / Helm is not implemented。
- secret manager is not implemented。
- production deployment is not completed。
- live PGvector validation is not completed。
- JdbcPolicyVectorRepository is not implemented。
- production auth/RBAC is not completed。
- production monitoring is not completed。
- Flyway / Liquibase migration strategy is not implemented。
- readiness/liveness 独立探针策略未完成。
- release/rollback checklist 未完成。
- production external integrations 未完成。

## Decision

阶段 6 的决策是：把部署问题拆成后续 production hardening 路线，而不是在当前阶段新增 runtime 或部署产物。
仓库继续保持默认离线验证；所有 live provider、live database、Docker、PGvector、CI/CD 和生产部署路径都必须
显式 opt-in。

## Dockerfile Strategy

后续 Dockerfile hardening 应单独实现并验证：

- 使用多阶段构建或 Spring Boot layertools 策略。
- 使用非 root 用户运行应用。
- 固定基础镜像系列并记录升级策略。
- 避免把 API Key、数据库密码或 `.env` 文件写入镜像。
- 明确 JVM memory / container resource 约束。
- 增加镜像构建和本地 smoke validation。

阶段 6 不新增 Dockerfile，也不声明镜像瘦身已完成。

## Docker Compose Strategy

现有 compose 文件继续定位为本地开发 / 本地调试：

- `docker-compose.yml` 是 app + MySQL local development path。
- `docker-compose-rag.yml` 是 PGvector local infrastructure path。
- Compose 不等于 production deployment。
- 后续可为 app service 增加 local healthcheck，但不能把 compose 写成生产可用方案。

## Profile / Configuration Strategy

配置策略保持显式 profile：

- 默认 profile 使用 in-memory / fake 路径。
- `mysql` profile 才启用 MySQL。
- `rag-postgres` profile 才启用 PostgreSQL / PGvector 相关配置。
- `application-prod.example.yml` 只是模板，不是默认加载的生产配置。
- 生产配置必须由部署系统、外部配置中心或未提交的环境变量注入。

## Secret Management Strategy

后续 secret management 需要独立设计：

- 真实 API Key、数据库密码、tokens 和 private endpoints 不得提交到仓库。
- 当前文档只能使用 placeholder。
- secret manager is not implemented。
- 未来接入 secret manager 前，默认验证仍不能依赖真实 secret。

## Database Migration Strategy

当前 MySQL schema / seed 和 PGvector schema 更偏本地开发与 profile foundation。后续生产数据库策略需要：

- 选择 Flyway 或 Liquibase。
- 定义 migration versioning、rollback 和 repeatable seed 边界。
- 区分 demo seed 与 production data migration。
- 明确 migration 不进入默认离线测试依赖。

阶段 6 不引入 Flyway / Liquibase。

## PGvector Deployment Strategy

PGvector 当前是 opt-in foundation：

- 已有 `application-rag-postgres.yml`、`docker-compose-rag.yml`、schema/docs、repository contract 和 fake /
  in-memory 默认路径。
- `JdbcPolicyVectorRepository` is not implemented。
- live PGvector validation is not completed。
- Spring AI VectorStore production path 仍是 future / opt-in。

后续 live PGvector deployment 必须显式 opt-in，并保持默认 `mvn test` 不连接 PostgreSQL / PGvector。

## CI / Quality Gate Strategy

后续 CI/CD 应以默认离线 gate 为基础：

```bash
mvn test
mvn checkstyle:check
mvn spotbugs:check
mvn test -Dtest=ArchitectureTest
```

CI/CD is not implemented。阶段 6 只记录 quality gate strategy，不新增 workflow 文件。

## Readiness / Liveness Strategy

当前默认健康入口是 `/actuator/health`。后续生产 readiness / liveness 应区分：

- liveness：应用进程是否可响应。
- readiness：依赖配置、数据库连接、队列、provider opt-in 状态是否满足部署环境要求。
- health details 必须 sanitize，不暴露 secrets、local paths、raw prompts 或 provider config。

阶段 6 不新增 readiness/liveness endpoint，也不改变 Actuator health behavior。

## Observability / Metrics Strategy

当前 baseline 是 MDC / structured logs、`X-Request-Id`、ToolCallTrace、ApprovalRequest、Execution Tree、
Actuator health、RAG readiness diagnostics、OpenAPI docs 和 offline RAG evaluation metrics。

Prometheus registry、Grafana dashboard、OpenTelemetry collector、distributed tracing、provider latency / cost metrics
和 production monitoring is not completed。后续 metrics 不得把 API keys、tokens、full prompts、raw provider
responses 或 high-cardinality user content 写入 labels / spans。

## Security / Auth Boundary

production auth/RBAC is not completed。阶段 6 不新增 security runtime，也不绕过现有安全边界。

后续 production auth/RBAC 至少需要：

- API authentication。
- Role / permission model。
- Approval operation audit。
- Trace / execution-tree access control。
- Rate limit 和 abuse protection。

## Rollback / Release Strategy

后续 release / rollback 需要独立 checklist：

- artifact versioning。
- config versioning。
- database migration rollback。
- feature flag 或 profile rollback。
- health check gate。
- release notes 和 known limitations。

阶段 6 只记录路线，不完成 release automation。

## Default Offline Boundary

默认验证继续不需要：

- real LLM；
- API Key；
- PostgreSQL；
- PGvector；
- Docker；
- MySQL；
- Redis；
- external network；
- real embedding provider；
- Spring AI live provider calls；
- secret manager；
- CI runner；
- Kubernetes / Helm；
- Prometheus / Grafana / OpenTelemetry collector。

## Non-goals

阶段 6 不做：

- Dockerfile implementation；
- CI/CD pipeline；
- Kubernetes / Helm manifests；
- secret manager integration；
- production deployment；
- production auth/RBAC；
- production monitoring；
- live PGvector validation；
- `JdbcPolicyVectorRepository`；
- Flyway / Liquibase；
- production readiness/liveness runtime change；
- real refund / exchange / compensation / payment / logistics integration。

## Alternatives Considered

- 立即实现 Dockerfile / CI/CD：拒绝。当前阶段限定为文档路线，避免把生产 hardening 混入 docs-only 阶段。
- 把 Docker Compose 当作生产部署：拒绝。Compose 继续作为本地开发和调试路径。
- 默认开启 live PGvector validation：拒绝。默认验证必须离线、确定性。
- 在阶段 6 接入 production auth/RBAC：拒绝。该能力需要独立设计、API 权限模型和测试。

## Consequences

- 项目审查中的部署缺口被转化为可执行路线。
- 文档明确当前 deployment baseline 与未完成能力。
- 后续 production hardening 可以按 Dockerfile、CI/CD、profile、secret、migration、PGvector、health、
  observability、security、release/rollback 分阶段推进。
- 当前 runtime、配置资源、compose 文件和默认测试行为保持不变。

## Follow-ups

- 阶段 7 或 V5：实现 Dockerfile hardening。
- 阶段 7 或 V5：新增 CI workflow 并运行默认离线 gate。
- 阶段 7 或 V5：设计 production auth/RBAC。
- 阶段 7 或 V5：选择 Flyway / Liquibase migration strategy。
- 阶段 7 或 V5：实现 `JdbcPolicyVectorRepository` 和 live PGvector opt-in validation。
- 阶段 7 或 V5：规划 production monitoring / metrics / tracing。

## Completion Signal

TASK_COMPLETE
