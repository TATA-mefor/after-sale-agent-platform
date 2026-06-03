# Flyway 迁移基础

Date: 2026-06-03

Status: V5.B.2.2 completed for Flyway migration foundation.

## Goal

本文件记录 AfterSale-Agent 的数据库迁移基础。V5.B.2.2 只引入 Flyway 依赖、默认关闭配置、profile-specific
migration locations 和 baseline migration 文件，不改变业务 runtime，也不让默认验证连接数据库。

## Why Flyway

- 项目当前 schema 以 SQL 文件为事实基线，Flyway 的 SQL-first 模型与现有 `schema-mysql.sql` 和
  `schema-rag-postgres.sql` 对齐。
- Spring Boot 已提供 Flyway auto-configuration，依赖版本由 Spring Boot dependency management 管理。
- V5.B.2.2 不需要 rollback DSL、复杂变更集元数据或 XML/YAML migration，因此选择 Flyway 更轻量。
- Flyway migration 默认关闭，只在显式 profile 和显式环境变量启用时运行。

## Why Not Liquibase

本阶段不引入 Liquibase。原因是当前目标是把既有 SQL schema baseline 纳入版本化目录，而不是引入新的
changeset DSL、rollback DSL 或跨数据库抽象层。Liquibase 可作为 future evaluation，但 V5.B.2.2 不添加
Liquibase dependency、配置或 migration 文件。

## Migration Locations

```text
src/main/resources/db/migration/mysql
src/main/resources/db/migration/pgvector
```

MySQL baseline migration:

```text
src/main/resources/db/migration/mysql/V20260603001__mysql_baseline.sql
```

PGvector baseline migration:

```text
src/main/resources/db/migration/pgvector/V20260601001__pgvector_policy_rag_baseline.sql
```

## Default Flyway Disabled

`application.yml` keeps Flyway disabled by default:

```yaml
spring:
  flyway:
    enabled: false
```

Default `mvn test` does not run migrations, does not create a `DataSource`, and does not connect to MySQL,
PostgreSQL, PGvector, Docker, Redis, real LLMs, real embedding providers, Spring AI live providers, or external
network.

## MySQL Opt-in

`application-mysql.yml` declares:

```yaml
spring:
  flyway:
    enabled: ${AFTERSALE_FLYWAY_ENABLED:false}
    locations: classpath:db/migration/mysql
```

This keeps MySQL migration explicit opt-in. The baseline migration is schema-only and intentionally does not include
`data-mysql.sql` demo seed records.

## PGvector Opt-in

`application-rag-postgres.yml` declares:

```yaml
spring:
  flyway:
    enabled: ${AFTERSALE_RAG_FLYWAY_ENABLED:false}
    locations: classpath:db/migration/pgvector
```

The PGvector path keeps the existing environment variable convention:

```text
AFTERSALE_PGVECTOR_URL
AFTERSALE_PGVECTOR_USERNAME
AFTERSALE_PGVECTOR_PASSWORD
AFTERSALE_PGVECTOR_SCHEMA
```

V5.B.2.2 does not rename these variables.

## PGvector Baseline

The PGvector baseline migration copies the schema semantics from `src/main/resources/schema-rag-postgres.sql` version
`2026-06-01-001`:

- `CREATE EXTENSION IF NOT EXISTS vector`;
- `policy_documents`;
- `policy_chunks`;
- `policy_embeddings`;
- supporting indexes.

The migration is intended for the explicit opt-in `JdbcPolicyVectorRepository` path. It does not include policy
content, raw datasets, raw prompts, secrets, or sample data.

## Relationship to schema-rag-postgres.sql

`schema-rag-postgres.sql` remains as a manual / Docker init baseline reference for existing local setup docs and
V5.A.3 smoke setup. The Flyway baseline migration is a versioned copy for future managed migration usage.

V5.B.2.2 does not remove the manual SQL initialization path and does not alter `docker-compose-rag.yml`.

## Manual / Docker Init Retained

Existing local paths remain valid:

- docker-compose-rag fresh-volume init using `schema-rag-postgres.sql`;
- manual SQL import for local PGvector setup;
- opt-in live smoke setup guarded by `-Dlive.rag=true`.

If `CREATE EXTENSION IF NOT EXISTS vector` requires elevated database privileges, manual PGvector setup should use a
database where the extension is preinstalled or initialized by a privileged init process. V5.B.2.2 does not change
that operational boundary.

## Secret Boundary

Migration files and docs do not contain real API keys, database passwords, access tokens, private endpoints, local
absolute paths, raw prompts, raw provider responses, or raw datasets. Database credentials remain environment-driven
or future secret-manager-driven.

## CI Boundary

The default Maven gate remains:

```bash
mvn test
mvn checkstyle:check
mvn spotbugs:check
mvn test -Dtest=ArchitectureTest
```

Flyway is disabled by default, so the default gate remains offline and deterministic. CI does not run MySQL or
PGvector migrations unless a future explicit profile validation task adds opt-in jobs.

## Live Validation Boundary

V5.B.2.3 Profile Matrix Validation later completes the file-based profile matrix validation harness for default,
`mysql`, `rag-postgres`, production template, Flyway, CI, and live smoke boundaries. Runtime profile behavior was not
changed. Live migration validation must remain explicit opt-in and must skip or fail clearly when database
configuration is absent.

## Rollback / Repeatable Future Plan

Future migration work should define:

- repeatable seed strategy for demo data;
- migration repair policy for local development;
- rollback strategy for production releases;
- optional live profile validation for MySQL and PGvector;
- separation between schema migration, demo seed data, and production data migration.

## What Is Not Completed

- Liquibase is not introduced.
- Flyway is not enabled by default.
- Profile matrix runtime behavior changes are not implemented; V5.B.2.3 only adds file-based harness coverage.
- Production deployment is not completed.
- Production auth / RBAC is not completed.
- Production monitoring is not completed.
- Secret manager integration is not implemented.
- Real refund, exchange, coupon compensation, payment, logistics, or dispute-closing integrations are not connected.

## Completion Signal

TASK_COMPLETE
