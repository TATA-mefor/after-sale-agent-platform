# V5.B.1 Container + CI

Date: 2026-06-03

Status: Completed

## Goal

Add a safe container build foundation and CI quality gate after the V5.A RAG production path foundation.

V5.B.1 is container and CI hardening foundation work. It is not production deployment completion and does not change
runtime business behavior.

## Scope Completed

- Hardened the existing Dockerfile into a multi-stage Java 17 Maven build plus Java 17 JRE runtime image.
- Added non-root runtime user execution.
- Expanded `.dockerignore` to keep secrets, local files, build output, IDE files, and temporary data out of build
  context.
- Expanded GitHub Actions CI to run the Maven quality gate.
- Added a CI Docker build validation job that does not push images or require registry credentials.
- Added container / CI docs and docs harness coverage.

## What Changed

- `Dockerfile` now uses a dedicated build stage and a non-root runtime stage.
- `.dockerignore` excludes `.env*`, keys, certificates, `target`, Git metadata, IDE files, logs, temp folders, and
  common local build output.
- `.github/workflows/ci.yml` runs tests, Checkstyle, SpotBugs, ArchitectureTest, and a local Docker image build.
- Documentation now records V5.B.1 as completed foundation work and keeps V5.B.2 through V5.B.4 planned.

## Dockerfile Boundary

The Dockerfile packages the existing Spring Boot jar and starts it with `java -jar`. It follows the current Java 17
project setting and does not change Spring configuration semantics.

It does not enable live LLM, live Spring AI, `rag-postgres`, PGvector, MySQL, Redis, or production profiles by default.

## Multi-stage Build Boundary

The build stage uses Maven to create the jar with tests skipped because CI owns the quality gate. The runtime stage
contains the JRE and packaged jar only.

## Non-root Runtime Boundary

The runtime image creates and uses an `aftersale` system user. The application process does not run as root.

## Dockerignore / Secret Safety Boundary

The Docker build context excludes local `.env` files, credentials, key material, `target`, Git metadata, IDE folders,
logs, temporary folders, and local data folders.

No API keys, database passwords, tokens, private keys, raw prompts, raw datasets, or local absolute paths are baked
into the image.

## CI Quality Gate Boundary

The GitHub Actions workflow runs:

```bash
mvn -B --no-transfer-progress test
mvn -B --no-transfer-progress checkstyle:check
mvn -B --no-transfer-progress spotbugs:check
mvn -B --no-transfer-progress test -Dtest=ArchitectureTest
```

The explicit ArchitectureTest step remains even though the full test suite already includes it.

## Docker Build Boundary

CI validates:

```bash
docker build -t after-sale-agent-platform:ci .
```

The workflow does not push an image, log in to a registry, require registry secrets, run Docker Compose, or deploy.

## Live Test Boundary

CI does not set `live.rag=true`, `live.llm=true`, live Spring AI flags, live embedding flags, database URLs, or
provider keys. Live validation remains explicit opt-in only.

## Production Deployment Boundary

V5.B.1 adds build and CI foundation only. Kubernetes / Helm, CD, release automation, secret manager integration,
production auth / RBAC, production monitoring, readiness / liveness runtime changes, Flyway / Liquibase, and external
business integrations remain future work.

## Runtime Non-change Boundary

No `src/main/java` runtime or business code was changed. ToolRegistry, AgentRun, `search_aftersale_policy`, RAG
retrieval, RAG evidence merge, ingestion, health indicators, OpenAPI, ToolCallTrace, Workspace, and Execution Tree
runtime behavior remain unchanged.

## Default Offline Boundary

Default Maven validation remains offline and deterministic. It does not require real LLMs, API keys, PostgreSQL,
PGvector, Docker, MySQL, Redis, real embedding providers, Spring AI live calls, Docker Compose, registry secrets, or
external business services.

## Validation Commands

```bash
mvn test -Dtest=ContainerCiHardeningDocsTest
mvn test
mvn checkstyle:check
mvn spotbugs:check
mvn test -Dtest=ArchitectureTest
```

Optional local Docker validation:

```bash
docker build -t after-sale-agent-platform:local .
```

## Known Limitations

- V5.B.1 does not add production deployment manifests.
- V5.B.1 does not add Kubernetes / Helm.
- V5.B.1 does not add a secret manager.
- V5.B.1 does not add production auth / RBAC.
- V5.B.1 does not add production monitoring.
- V5.B.1 does not add Flyway / Liquibase migration management.
- V5.B.1 does not make live PGvector validation part of the default gate.

## Follow-ups

- V5.B.2: config, secret, and migration hardening.
- V5.B.3: observability runtime hardening.
- V5.B.4: auth, Kubernetes / Helm, release, and rollback hardening.

## Completion Signal

TASK_COMPLETE
