# 配置、密钥与迁移治理方案

Date: 2026-06-03

Status: V5.B.2.1 completed for documentation boundary.

## 目标

本方案用于固定 AfterSale-Agent 当前配置基线、profile matrix、secret boundary 和 migration follow-up。它是
V5.B.2 的第一步，只做文档事实口径和后续任务拆分，不修改 runtime，不引入 Flyway / Liquibase，不接入
secret manager。

## 当前配置文件说明

- `src/main/resources/application.yml`：default offline / local baseline。默认测试使用 in-memory / fake 路径。
- `src/main/resources/application-prod.example.yml`：生产配置模板，不默认加载，不是部署清单。
- `src/main/resources/application-mysql.yml`：显式 `mysql` profile，使用 `AFTERSALE_MYSQL_*` 变量。
- `src/main/resources/application-rag-postgres.yml`：显式 `rag-postgres` profile，使用 `AFTERSALE_PGVECTOR_*`
  变量。
- `.env.rag.example`：本地 PGvector placeholder 示例，真实 `.env` 不得提交。
- `src/main/resources/schema-rag-postgres.sql`：PGvector schema baseline reference，版本
  `2026-06-01-001`。

## Profile Matrix

| Profile | 主要用途 | 配置入口 | 默认是否启用 | 说明 |
|---|---|---|---|---|
| default | 本地 demo、默认测试、离线验证 | `application.yml` | 是 | 不需要真实 LLM、数据库、Docker 或外部网络 |
| mysql | 本地 MySQL persistence | `application-mysql.yml` | 否 | 需要显式 profile 和 MySQL 连接信息 |
| rag-postgres | opt-in PGvector policy evidence path | `application-rag-postgres.yml` | 否 | 需要显式 profile 和 PGvector 连接信息 |
| prod-template | 生产配置参考 | `application-prod.example.yml` | 否 | 只作为模板，不代表 production deployment |

## Secret 输入规则

- 真实 API Key、数据库密码、访问令牌、private endpoint、证书和本地绝对路径不得进入仓库。
- 文档和配置模板只能使用 placeholder。
- 生产环境应由部署平台、环境变量或 future secret manager 注入敏感值。
- 日志、health、OpenAPI examples 和 docs harness 不得暴露 secret、raw prompt、raw provider response 或 raw
  dataset path。

## Env Var Naming

现有 PGvector 路径继续使用项目已有变量名：

```text
AFTERSALE_PGVECTOR_URL
AFTERSALE_PGVECTOR_USERNAME
AFTERSALE_PGVECTOR_PASSWORD
AFTERSALE_PGVECTOR_SCHEMA
```

MySQL profile 使用：

```text
AFTERSALE_MYSQL_URL
AFTERSALE_MYSQL_USERNAME
AFTERSALE_MYSQL_PASSWORD
```

provider API Key 仍只允许 placeholder 或部署环境注入，默认验证不需要任何真实 key。

## Docker Runtime Config

V5.B.1 的 image build 不包含 secret。Docker runtime config 必须通过环境变量、Spring profile、部署平台或
future secret manager 注入。V5.B.2.1 不修改 Dockerfile、compose 文件或 CI workflow。

## CI Secret Boundary

默认 CI gate 只运行离线 Maven 验证和 Docker image build validation，不注入 live secrets，不运行 live LLM、
live Spring AI、live PGvector、live MySQL、Redis、Docker Compose 或外部业务服务。

## Migration Follow-up

当前没有 Flyway / Liquibase migration framework。V5.B.2.2 需要选择 migration framework，并把当前 schema
baseline 拆成可版本化 migration。

## MySQL Migration Plan

- 当前参考基线：`schema-mysql.sql` 和 `data-mysql.sql`。
- 后续需要区分 schema migration、demo seed、production data migration。
- 默认验证不得因为 migration 连接 MySQL。

## PGvector Migration Plan

- 当前参考基线：`schema-rag-postgres.sql`，schema version `2026-06-01-001`。
- 后续 migration 需要覆盖 PGvector extension、policy documents / chunks / embeddings tables、indexes、vector
  dimension、rollback 和 smoke setup。
- `CREATE EXTENSION IF NOT EXISTS vector` 可能需要较高权限；现阶段仍以 docker-compose-rag fresh-volume init、
  手动预安装 extension 或 V5.A.3 smoke skip 作为边界。

## What Is Not Completed

- secret manager 未实现。
- Flyway / Liquibase 未实现。
- production deployment 未完成。
- production auth / RBAC 未完成。
- production monitoring 未完成。
- readiness / liveness runtime changes 未实现。
- profile matrix runtime validation 留到 V5.B.2.3。
- 真实退款、换货、补偿、支付、物流外部系统未接入。

## Default Offline Boundary

默认验证仍不需要：

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
- Docker Compose。

## Follow-up Task Split

- V5.B.2.2：Flyway / Liquibase migration framework。
- V5.B.2.3：profile matrix runtime validation。
- V5.B.3：observability runtime hardening。
- V5.B.4：auth、Kubernetes / Helm、release / rollback hardening。

## Completion Signal

TASK_COMPLETE
