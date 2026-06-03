# EXEC PLAN V5.B.2.1 Config + Secret Boundary / Profile Matrix Plan

Date: 2026-06-03

Status: Completed

## Goal

记录 AfterSale-Agent 当前配置、密钥、profile matrix 和迁移治理边界，为 V5.B.2 后续 migration framework 和
profile runtime validation 提供文档基线。

## Scope Completed

- 新增 V5.B.2 config / secret / migration decision record。
- 新增中文维护者方案 `docs/deploy/CONFIG_SECRET_MIGRATION_PLAN.md`。
- 更新 deployment roadmap、production config docs、quality docs、validation docs、README 和 V5 summary 文档。
- 新增 docs harness test，确保本阶段只读文档、默认离线、无 secret / local path / production overclaim。

## What Changed

- 明确 default、mysql、rag-postgres、prod-template 的 profile matrix。
- 明确现有 PGvector env var 使用 `AFTERSALE_PGVECTOR_URL`、`AFTERSALE_PGVECTOR_USERNAME`、
  `AFTERSALE_PGVECTOR_PASSWORD` 和 optional `AFTERSALE_PGVECTOR_SCHEMA`。
- 明确 `schema-rag-postgres.sql` 是 schema version `2026-06-01-001` baseline reference。
- 明确 Flyway / Liquibase、secret manager 和 profile matrix runtime validation 仍为后续阶段。

## Configuration Baseline Boundary

本阶段不修改 `application.yml`、`application-mysql.yml`、`application-rag-postgres.yml` 或
`application-prod.example.yml` runtime 语义。`application-prod.example.yml` 仍只是模板，不默认加载。

## Profile Matrix Boundary

profile matrix 只在文档中记录。default profile 继续使用 in-memory / fake；MySQL 和 PGvector 仍为显式 opt-in。

## Secret Boundary

Docker image 不包含 secret，CI default gate 不注入 live secrets。真实 API Key、数据库密码、访问令牌、private
endpoint 和证书必须由部署环境或 future secret manager 注入。

## Migration Framework Boundary

V5.B.2.1 不实现 Flyway / Liquibase，不新增 migration directory，不修改 schema SQL。V5.B.2.2 才评估和实现
migration framework。

## Docker / CI Boundary

本阶段不修改 Dockerfile、`.dockerignore`、compose 文件或 GitHub Actions workflow。V5.B.1 的 Docker build 和 CI
quality gate 保持不变。

## Runtime Non-change Boundary

本阶段不修改 `src/main/java`、ToolRegistry、`search_aftersale_policy`、RAG runtime、ingestion pipeline、health
indicators、OpenAPI config、ToolCallTrace、Workspace、Execution Tree 或 AgentApplicationService。

## Default Offline Test Boundary

新增测试只读文档。默认验证仍不需要真实 LLM、API Key、PostgreSQL、PGvector、Docker、MySQL、Redis、real
embedding provider、Spring AI live calls 或外部网络。

## Validation Commands

```bash
mvn test -Dtest=ConfigSecretMigrationPlanDocsTest
mvn test
mvn checkstyle:check
mvn spotbugs:check
mvn test -Dtest=ArchitectureTest
```

## Known Limitations

- 本阶段只是方案与文档边界，不实现 secret manager。
- 本阶段不实现 Flyway / Liquibase。
- 本阶段不实现 profile matrix runtime validation。
- 本阶段不实现 readiness / liveness runtime changes、production monitoring、production auth / RBAC 或 production
  deployment。

## Follow-ups

- V5.B.2.2：Flyway / Liquibase migration framework。
- V5.B.2.3：profile matrix runtime validation。
- V5.B.3：observability runtime hardening。
- V5.B.4：auth、Kubernetes / Helm、release / rollback hardening。

## Completion Signal

TASK_COMPLETE
