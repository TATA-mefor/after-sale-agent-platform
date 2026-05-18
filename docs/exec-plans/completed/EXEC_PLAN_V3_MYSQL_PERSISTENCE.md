# EXEC_PLAN_V3_MYSQL_PERSISTENCE

Date: 2026-05-17
Status: Completed

## Goal

Implement V3.1 MySQL Persistence without changing the default offline test path or the existing Agent business
boundaries.

## Scope Completed

- Added an explicit `mysql` Spring profile.
- Added Spring JDBC based repository implementations for:
  - Ticket
  - AgentRun
  - ToolCallTrace
  - ApprovalRequest
  - Order demo data
  - AfterSalePolicy data
- Kept existing in-memory repositories active for the default non-`mysql` profile.
- Added MySQL schema initialization in `schema-mysql.sql`.
- Added deterministic demo seed data in `data-mysql.sql`.
- Added profile/configuration tests proving default tests do not create a `DataSource`.
- Added schema/seed harness tests covering required tables and secret-safety checks.
- Updated README, V3 execution plan, quality status, and active V3 plan.

## Design

The implementation uses Spring JDBC instead of JPA to keep the dependency surface small and to avoid introducing
database annotations into the domain layer. Domain models expose restore factories for repository hydration, while the
database-specific mapping remains in infrastructure classes.

Profile behavior:

```text
default profile -> in-memory repositories, no DataSource
mysql profile   -> JDBC repositories, MySQL DataSource, schema + seed initializer
```

Configuration comes from environment variables:

```text
AFTERSALE_MYSQL_URL
AFTERSALE_MYSQL_USERNAME
AFTERSALE_MYSQL_PASSWORD
```

No real database password, API key, or production secret is committed.

## Boundaries Preserved

- Default `mvn test` does not require MySQL, Docker, Redis, real LLMs, API keys, or external network.
- Domain layer does not depend on JPA, JdbcTemplate, DataSource, or Spring Data.
- Controller classes continue to call application services instead of repositories.
- Agent, Planner, Workspace, and Specialist Handler classes still do not access repositories directly.
- Tool execution still goes through `ToolRegistry`.
- High-risk actions still require approval and do not execute real refund, exchange, payment, logistics, or coupon
  compensation.
- Docker Compose, Redis, microservices, and production deployment concerns are left for later V3 stages.

## Validation Requirements

The completion gate for this phase remains:

```bash
mvn test
mvn checkstyle:check
mvn spotbugs:check
mvn test -Dtest=ArchitectureTest
```

## Manual MySQL Profile Verification

Date: 2026-05-18

Manual local verification was completed against MySQL 8.0.44 with the explicit `mysql` profile.

Verified results:

- `schema-mysql.sql` imported successfully.
- `data-mysql.sql` imported successfully.
- `orders` seed count: 6.
- `aftersale_policies` seed count: 6.
- Application startup with `SPRING_PROFILES_ACTIVE=mysql` succeeded.
- Creating a Ticket through the HTTP API succeeded.
- Triggering an AgentRun for the created Ticket succeeded.
- Querying the read-only Execution Tree for the AgentRun succeeded.

The verification used local environment variables for database connection values. No database password, personal
filesystem path, API key, or production secret is recorded in this repository.

## Risks

- The JDBC repositories are covered by configuration/schema harness tests, but not by a real MySQL integration test in
  the default suite. This is intentional to preserve offline determinism.
- Schema initialization currently uses SQL scripts loaded at application startup under the `mysql` profile. A future
  production-like setup may need a migration tool.
- MySQL seed data must stay aligned with the in-memory demo order and policy data as future scenarios are added.

## Follow-ups

- V3.2 Docker Compose should provide a local app + mysql startup path using example environment variables only.
- A future opt-in integration test can validate JDBC repositories against a real MySQL instance or Testcontainers
  without joining the default `mvn test` path.
- V3.3 should add structured logging fields that correlate with persisted IDs.

## Completion Signal

TASK_COMPLETE
