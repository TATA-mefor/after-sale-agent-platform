# EXEC_PLAN_V4_OPENAPI_API_DOCS

Date: 2026-05-28

Status: Completed

## Goal

Add OpenAPI and Swagger UI documentation for the existing Spring Boot APIs so reviewers can inspect Ticket, AgentRun,
Approval, ToolCallTrace, Execution Tree, RAG evidence boundaries, and health entry points without adding business
runtime behavior.

## Scope Completed

- Added springdoc OpenAPI WebMVC UI dependency and configuration.
- Added OpenAPI project metadata and tags.
- Added documentation annotations to existing controllers and DTOs.
- Added `docs/api/OPENAPI.md`.
- Added OpenAPI endpoint, exposure, docs harness, and architecture tests.
- Updated README, V4 roadmap, active V4 plan, quality score, Spring Boot completeness decision, and RAG retrieval
  contract docs.

## What Changed

- `/v3/api-docs` exposes OpenAPI JSON for existing HTTP APIs.
- `/swagger-ui.html` and `/swagger-ui/index.html` expose Swagger UI.
- `/actuator/health` remains the only default actuator exposure.
- API examples use synthetic demo data and do not include secrets, local paths, raw prompts, or raw datasets.

## OpenAPI Boundary

OpenAPI describes existing HTTP APIs only. It does not add endpoints, change controller paths, execute tools, create
Ticket or AgentRun state, modify ToolCallTrace, write Workspace evidence, change Execution Tree runtime, or alter
`search_aftersale_policy` retrieval.

## Swagger UI Boundary

Swagger UI is a local development and review aid. It does not represent production deployment, production monitoring,
authentication completeness, or live provider connectivity.

## API Documentation Boundary

The documented API groups are Ticket, AgentRun, Approval, ToolCallTrace, Execution Tree, and platform health.
`search_aftersale_policy` remains a ToolRegistry tool rather than a new public HTTP endpoint.

## Evidence-only Documentation Boundary

RAG policy evidence is documented as evidence-only. Evidence score is a retrieval score, not business decision
confidence. Documentation does not claim real refund, exchange, coupon compensation, payment, logistics, or dispute
closure execution.

## Default Offline Test Boundary

Default tests and docs do not require real LLMs, API keys, PostgreSQL, PGvector, Docker, MySQL, Redis, real embedding
providers, or external network access.

## Security / Secret Safety Boundary

OpenAPI metadata, examples, docs, and tests avoid API keys, passwords, tokens, local absolute paths, raw prompts, raw
dataset paths, full provider configuration, and credential-bearing URLs. Actuator exposure remains limited to health.

## Validation Commands

```bash
mvn test
mvn checkstyle:check
mvn spotbugs:check
mvn test -Dtest=ArchitectureTest
```

## Known Limitations

- V4.6.4 does not add authentication or authorization.
- OpenAPI does not include a public policy-search HTTP endpoint because none exists in the runtime.
- OpenAPI does not prove live PGvector or live embedding provider availability.

## Follow-ups

- V4.7 can continue Skill layer integration.
- Future security work can add explicit auth boundaries for approval or admin APIs.
- Future live integration tasks can document opt-in provider connectivity checks separately from default tests.

## Completion Signal

TASK_COMPLETE
