# EXEC PLAN V5.B.2.2 Flyway Migration Foundation

Date: 2026-06-03

Status: Completed

## Goal

Introduce a Flyway migration foundation for the existing MySQL and PGvector schema baselines while keeping default
validation offline and keeping all runtime business behavior unchanged.

## Scope Completed

- Added Flyway dependencies managed by Spring Boot dependency management.
- Kept Flyway disabled in the default profile.
- Added explicit opt-in Flyway locations for `mysql` and `rag-postgres` profiles.
- Added a schema-only MySQL baseline migration copied from `schema-mysql.sql`.
- Added a schema-only PGvector baseline migration copied from `schema-rag-postgres.sql` version `2026-06-01-001`.
- Added deployment documentation for the migration foundation and boundaries.
- Added docs/config harness coverage for Flyway selection, Liquibase exclusion, default disabled behavior, migration
  locations, baseline files, secret safety, and offline validation.

## What Changed

- `pom.xml` now includes `flyway-core`, `flyway-database-postgresql`, and `flyway-mysql` without hardcoded dependency
  versions.
- `application.yml` sets `spring.flyway.enabled=false`.
- `application-mysql.yml` sets `spring.flyway.locations=classpath:db/migration/mysql` and keeps
  `AFTERSALE_FLYWAY_ENABLED` default false.
- `application-rag-postgres.yml` sets `spring.flyway.locations=classpath:db/migration/pgvector` and keeps
  `AFTERSALE_RAG_FLYWAY_ENABLED` default false.
- `docs/deploy/MIGRATION_FOUNDATION.md` records the Flyway migration boundary.

## Flyway Selection Boundary

Flyway was selected because the project already has SQL schema baselines and Spring Boot can manage the dependency
versions. This is a foundation only; it does not run migrations by default.

## Liquibase Boundary

Liquibase is not introduced in V5.B.2.2. No Liquibase dependency, configuration, or migration directory was added.

## Default Disabled Boundary

Flyway is disabled by default. Default Spring context and default Maven validation do not create database connections
or execute migrations.

## Profile-specific Migration Boundary

Migration locations are profile-specific:

- `classpath:db/migration/mysql`
- `classpath:db/migration/pgvector`

Both profile flags default to false.

## PGvector Baseline Migration Boundary

The PGvector migration is a versioned baseline copy of `schema-rag-postgres.sql` version `2026-06-01-001`. It is meant
for the opt-in `JdbcPolicyVectorRepository` path and contains no sample data or policy content.

## MySQL Migration Boundary

The MySQL migration is a schema-only baseline copied from `schema-mysql.sql`. Demo seed data from `data-mysql.sql` is
not included.

## Secret Boundary

No migration, docs, or tests include real API keys, database passwords, access tokens, private endpoints, local
absolute paths, raw prompts, raw provider responses, or raw datasets.

## CI / Default Offline Boundary

The default gate remains offline:

```bash
mvn test
mvn checkstyle:check
mvn spotbugs:check
mvn test -Dtest=ArchitectureTest
```

It does not require real LLMs, API keys, PostgreSQL, PGvector, Docker, MySQL, Redis, real embedding providers, Spring
AI live provider calls, or external network.

## Runtime Non-change Boundary

V5.B.2.2 does not modify `src/main/java`, ToolRegistry, `search_aftersale_policy`, RAG runtime, ingestion pipeline,
health indicators, OpenAPI config, ToolCallTrace, Workspace, Execution Tree, or AgentApplicationService.

## Validation Commands

```bash
mvn test -Dtest=FlywayMigrationFoundationDocsTest
mvn test
mvn checkstyle:check
mvn spotbugs:check
mvn test -Dtest=ArchitectureTest
```

## Known Limitations

- Profile matrix runtime validation remains V5.B.2.3.
- Live migration execution against MySQL or PGvector is not part of default validation.
- `CREATE EXTENSION IF NOT EXISTS vector` may require elevated database privileges in manually managed PGvector
  databases; existing docker init or preinstalled extension setup remains the recommended live path.
- Rollback policy, repeatable demo seed strategy, production data migration strategy, and migration repair policy are
  future work.

## Follow-ups

- V5.B.2.3: profile matrix runtime validation.
- Future: migration rollback/release hardening.
- Future: secret manager integration.
- Future: production deployment and production monitoring hardening.

## Completion Signal

TASK_COMPLETE
