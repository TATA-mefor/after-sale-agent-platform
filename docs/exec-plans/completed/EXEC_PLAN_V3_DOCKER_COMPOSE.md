# EXEC_PLAN_V3_DOCKER_COMPOSE

Date: 2026-05-17
Status: Completed

## Goal

Implement V3.2 Docker Compose local startup for the existing V3.1 MySQL persistence profile without changing business
logic, default tests, or Agent execution boundaries.

## Scope Completed

- Added `docker-compose.yml`.
- Added `Dockerfile`.
- Added `.dockerignore`.
- Added local `mysql` service with placeholder development credentials.
- Added local `app` service that builds the Spring Boot jar and runs with `SPRING_PROFILES_ACTIVE=mysql`.
- Mounted `schema-mysql.sql` and `data-mysql.sql` into MySQL initialization.
- Added a named MySQL data volume.
- Added a MySQL healthcheck and app dependency on healthy MySQL.
- Updated README with start, health check, stop, cleanup, and fallback notes.
- Updated V3 execution plan, quality status, and active V3 plan.
- Added offline harness tests for Compose and Dockerfile constraints.

## Design

Docker Compose is scoped to local development only:

```text
docker compose up --build
```

The app container uses the explicit `mysql` profile and connects to the `mysql` service over the compose network. MySQL
initialization reuses the V3.1 schema and seed scripts, keeping persistence behavior aligned across manual MySQL startup
and compose startup.

Credentials are local placeholders:

```text
AFTERSALE_MYSQL_USERNAME=aftersale
AFTERSALE_MYSQL_PASSWORD=aftersale
AFTERSALE_MYSQL_ROOT_PASSWORD=aftersale_root
```

They can be overridden by shell environment variables or an uncommitted local `.env` file. No real password, API key,
token, or production configuration is committed.

## Boundaries Preserved

- Default `mvn test` does not require Docker, MySQL, Redis, real LLMs, API keys, or external network.
- In-memory repositories remain the default non-`mysql` path.
- Compose does not introduce Redis, Kubernetes, production deployment scripts, or microservices.
- Agent, ToolRegistry, Approval, Trace, Workspace, Planner, and Specialist Handler logic is unchanged.
- Compose does not execute real refund, exchange, payment, logistics, or coupon compensation actions.

## Validation Requirements

The completion gate for this phase remains:

```bash
mvn test
mvn checkstyle:check
mvn spotbugs:check
mvn test -Dtest=ArchitectureTest
```

Optional manual local smoke command:

```bash
docker compose up --build
```

This Docker command is intentionally outside the default test gate.

## Risks

- Docker image builds may need network access to pull base images and Maven dependencies on a fresh machine.
- The compose setup is local-only and does not provide production-grade secret management, backups, monitoring, or high
  availability.
- MySQL host port `3306` can conflict with an existing local MySQL instance.

## Follow-ups

- V3.3 should add structured logging fields and keep actuator health visible.
- A future opt-in integration test may run against compose or Testcontainers without joining the default test path.
- V3.4 should include a final system review and demo flow verification.

## Completion Signal

TASK_COMPLETE
