# DECISION V5.B.2 Config / Secret / Migration Boundary

Date: 2026-06-03

Status: Completed for V5.B.2.1 documentation boundary.

## Context

V5.B.1 已完成 Dockerfile、`.dockerignore`、CI Maven quality gate 和 Docker build validation。下一步进入
V5.B.2 配置、密钥和迁移治理。V5.B.2.1 只做事实基线、profile matrix、secret boundary 和 migration follow-up
方案，不修改 runtime 配置语义，不引入迁移框架，不接入 secret manager。

## Current Configuration Baseline

- `src/main/resources/application.yml` 是 default offline / local baseline。
- `src/main/resources/application-prod.example.yml` 是生产配置模板，不会被默认加载，也不是部署清单。
- `src/main/resources/application-mysql.yml` 是显式 opt-in MySQL profile。
- `src/main/resources/application-rag-postgres.yml` 是显式 opt-in PostgreSQL / PGvector profile。
- `.env.rag.example` 是本地 PGvector placeholder 示例，不应提交真实 `.env`。
- `src/main/resources/schema-rag-postgres.sql` 是 PGvector schema baseline reference，版本为
  `2026-06-01-001`。

## Profile Matrix

| Profile | 用途 | 默认验证 | 外部依赖 |
|---|---|---|---|
| default | in-memory / fake 本地开发和默认测试 | 默认使用 | 无 |
| mysql | 本地 MySQL persistence 验证 | 显式启用 | MySQL |
| rag-postgres | opt-in PGvector evidence repository | 显式启用 | PostgreSQL / PGvector |
| prod-template | 生产配置模板参考 | 不加载 | 由部署环境决定 |

## Secret Boundary

- 真实 API Key、数据库密码、访问令牌、private endpoint 和证书不得提交到仓库。
- 示例值只能使用 placeholder 或本地开发 placeholder。
- `Dockerfile` 不 bake secrets。
- CI default gate 不注入 live secrets。
- logs、health details、OpenAPI examples 和 docs 不应暴露密钥、raw prompt、raw provider response 或本地路径。
- secret manager 是 future path，本阶段未实现。

## Production Config Boundary

`application-prod.example.yml` 只说明生产配置应如何通过环境变量或部署平台注入。它不代表 production
deployment、production auth、production monitoring、secret manager、readiness / liveness runtime 或外部业务系统
已经完成。

## Migration Framework Boundary

V5.B.2.1 不实现 Flyway 或 Liquibase。当前 `schema-rag-postgres.sql` 仍是 baseline reference，用于
docker-compose-rag fresh-volume init、手动 SQL 导入和 V5.A.3 live smoke 的 schema setup。正式 migration framework
留到 V5.B.2.2。

## MySQL Migration Plan

- 以 `schema-mysql.sql` 和 `data-mysql.sql` 为当前 MySQL baseline reference。
- V5.B.2.2 决定 Flyway 或 Liquibase 后，再拆分 schema migration、demo seed 和 production data migration。
- migration 不进入 default offline gate，不得让默认 `mvn test` 连接 MySQL。

## PGvector Migration Plan

- 以 schema version `2026-06-01-001` 为当前 PGvector baseline reference。
- 后续 migration 需要覆盖 extension setup、table/index creation、vector dimension、schema rollback 和 live
  smoke setup。
- V5.B.2.1 不修改 `schema-rag-postgres.sql`，不实现 migration directory，也不改变 `JdbcPolicyVectorRepository`。

## CI / Validation Boundary

- 默认 CI / Maven gate 仍是 `mvn test`、`mvn checkstyle:check`、`mvn spotbugs:check`、
  `mvn test -Dtest=ArchitectureTest`。
- `ConfigSecretMigrationPlanDocsTest` 只读文档，不启动 Spring，不连接数据库，不调用 Docker、LLM、embedding
  provider 或外部网络。
- live PGvector smoke 仍需显式 `-Dlive.rag=true`，缺配置应 skip。

## Docker / Runtime Config Boundary

- Docker image 只包含应用 artifact，不包含真实密钥。
- Runtime configuration 应通过环境变量、Spring profile、部署平台或 future secret manager 注入。
- V5.B.2.1 不修改 `Dockerfile`、Compose 文件、GitHub Actions workflow 或 application yml runtime 语义。

## Security / Logging Boundary

- OpenAPI、Actuator health、日志、metrics 规划和 docs 示例不得展示密钥、raw prompt、raw provider response、
  raw dataset path 或本地绝对路径。
- 生产 auth / RBAC、secret manager、audit log hardening 和 production monitoring 仍是后续任务。

## Non-goals

- 不实现 Flyway / Liquibase。
- 不实现 secret manager。
- 不修改 runtime 配置语义。
- 不修改 Dockerfile、CI workflow 或 compose。
- 不修改 Agent、Tool、RAG、ingestion、health、OpenAPI 或业务 runtime。
- 不接入真实退款、换货、补偿、支付或物流。

## Follow-ups

- V5.B.2.2：Flyway / Liquibase migration framework decision and baseline。
- V5.B.2.3：profile matrix runtime validation。
- V5.B.3：observability runtime hardening。
- V5.B.4：auth、Kubernetes / Helm、release / rollback hardening。

## Completion Signal

TASK_COMPLETE
