# Project Review Correction Stage 1: Production Config Template

Date: 2026-06-01

Status: Completed

## Goal

Close the project review finding that the repository had no production configuration template, while preserving the
current default offline validation path and avoiding any runtime behavior change.

## Scope Completed

- Added a safe `application-prod.example.yml` template.
- Added Chinese documentation for the template and its environment variable groups.
- Linked the template documentation from README and validation docs.
- Updated the Chinese remediation plan and quality notes to mark Stage 1 completed.
- Added docs harness coverage for template existence, links, secret safety, default offline wording, and no production
  overclaims.

## What Changed

- `src/main/resources/application-prod.example.yml` documents production-oriented placeholders for server, logging,
  datasource / Hikari, LLM provider, Spring AI, RAG / PGvector, Actuator, and OpenAPI settings.
- `docs/deploy/PRODUCTION_CONFIG_TEMPLATE.md` explains how to treat the template as an example, not as a deployment
  manifest.
- `ProductionConfigTemplateDocsTest` locks the documentation and template boundaries with read-only tests.

## Production Config Template Boundary

The template is named `application-prod.example.yml`, so it is not a Spring Boot profile file loaded by default. It can
be copied or rendered by a deployment system later, but the repository does not ship real production configuration.

## Secret Placeholder Boundary

Sensitive fields use environment variable placeholders with empty defaults. The template and docs do not contain real
API keys, database passwords, tokens, private endpoints, local absolute paths, raw prompts, or raw dataset paths.

## Default Offline Boundary

Default validation remains offline and deterministic. The default test path does not require real LLMs, API keys,
PostgreSQL, PGvector, Docker, MySQL, Redis, real embedding providers, Spring AI live provider calls, or external
network.

## Runtime Non-change Boundary

Stage 1 does not modify `src/main/java`, runtime service logic, controllers, tool executors, RAG search, ingestion
pipeline, health indicators, OpenAPI config, ToolRegistry, ToolCallTrace, Workspace, Execution Tree, or
AgentApplicationService.

## Production Hardening Boundary

This stage does not implement production auth, secret manager integration, production deployment, production monitoring,
metrics, distributed tracing, CI/CD, Kubernetes, Helm, Dockerfile hardening, `JdbcPolicyVectorRepository`, live
PGvector validation, production ingestion admin UI, or real payment / logistics / refund integrations.

## Validation Commands

```bash
mvn test -Dtest=ProductionConfigTemplateDocsTest,ProjectRemediationPlanDocsTest
mvn test
mvn checkstyle:check
mvn spotbugs:check
mvn test -Dtest=ArchitectureTest
```

## Known Limitations

- The template is a documentation and configuration skeleton only.
- It does not prove live database, live provider, live PGvector, production auth, or production monitoring readiness.
- Later stages must decide how to handle secret manager integration, metrics, tracing, deployment hardening, and
  production operational workflows.

## Follow-ups

- Stage 2: 可观测性决策。
- Stage 3: 领域模型强化。
- Stage 4: API 完整性。
- Stage 5: RAG 检索质量。
- Stage 6: Spring AI 深化。
- Stage 7: 部署工程化。

## Completion Signal

TASK_COMPLETE
