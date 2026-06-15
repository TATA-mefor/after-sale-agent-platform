# Container + CI Hardening

Date: 2026-06-03

Status: V5.B.1 completed

## Goal

V5.B.1 adds the container build foundation and CI quality gate for AfterSale-Agent. The current package namespace is
`io.github.tatame.aftersale`.

This is production hardening foundation work. It is not a production deployment, not a CD pipeline, and not a registry
release process. V5.B Production Hardening current planned scope completed; see
`docs/deploy/PRODUCTION_HARDENING_COMPLETION_SUMMARY.md`.

## Dockerfile Boundary

- `Dockerfile` uses a Maven build stage and a separate Eclipse Temurin Java 17 JRE runtime stage.
- The Java version follows `pom.xml`, which currently declares Java 17.
- The build stage packages the Spring Boot jar with `mvn -DskipTests package`.
- Tests remain in the Maven quality gate instead of being forced inside image build.
- The image does not enable `rag-postgres`, live LLM, live Spring AI, or live PGvector profiles by default.
- Runtime configuration remains externalized through environment variables and Spring profiles.

## Multi-stage Build Boundary

The build stage contains Maven, source code, and packaging tools. The runtime stage copies only the packaged jar into
`/app/app.jar`.

The image build does not run live tests, does not start Docker Compose, and does not call external business services.

## Non-root Runtime Boundary

The runtime stage creates an `aftersale` system user and runs the application with `USER aftersale`.

The entrypoint is:

```text
java $JAVA_OPTS -jar /app/app.jar
```

`JAVA_OPTS` is intentionally empty by default and may be provided by the runtime environment.

## Dockerignore / Secret Safety

`.dockerignore` excludes Git metadata, `target`, IDE files, `.env` files, logs, temporary files, local data folders,
Node caches if present, Docker build output, and common key / secret file patterns.

The Docker image must not contain API keys, database passwords, tokens, private keys, local absolute paths, or raw
datasets. Do not bake secrets into the Dockerfile or image layers.

## CI Quality Gate

`.github/workflows/ci.yml` runs the default Maven quality gate:

```bash
mvn -B --no-transfer-progress test
mvn -B --no-transfer-progress checkstyle:check
mvn -B --no-transfer-progress spotbugs:check
mvn -B --no-transfer-progress test -Dtest=ArchitectureTest
```

The CI job uses Java 17 and Maven cache. It does not set live provider flags, database URLs, registry secrets, or
production credentials.

## Docker Build Validation

The workflow also has a Docker image build job:

```bash
docker build -t after-sale-agent-platform:ci .
```

This validates that the Dockerfile can build an image. It does not push the image, log in to a registry, publish a
release, or deploy the application.

Local validation:

```bash
docker build -t after-sale-agent-platform:local .
docker run --rm -p 8080:8080 after-sale-agent-platform:local
```

PowerShell:

```powershell
docker build -t after-sale-agent-platform:local .
docker run --rm -p 8080:8080 after-sale-agent-platform:local
```

Docker validation is optional for local development. The default Maven validation path does not require Docker.

## Default Offline Boundary

Default validation does not require:

- real LLM;
- API Key;
- PostgreSQL;
- PGvector;
- Docker;
- MySQL;
- Redis;
- real embedding provider;
- Spring AI live provider calls;
- external network;
- Docker Compose;
- registry login.

## Live Tests Boundary

Live tests remain explicit opt-in and are not part of the default CI gate. CI does not run live LLM, live Spring AI,
live PGvector, live MySQL, Redis, or external service checks. `JdbcPolicyVectorRepositorySmokeTest` still requires
`-Dlive.rag=true` and local PGvector configuration when a maintainer chooses to run it.

## What Is Not Completed

V5.B.1 does not complete:

- production deployment;
- CD / release automation;
- registry push;
- Kubernetes / Helm;
- secret manager integration;
- Flyway / Liquibase migration management;
- production auth / RBAC;
- production monitoring;
- Prometheus or OpenTelemetry runtime instrumentation;
- readiness / liveness runtime changes;
- live PGvector validation in the default gate;
- real refund, exchange, payment, logistics, or coupon compensation integrations.

## Future Work

- V5.B.2: config, secret, and migration hardening.
- V5.B.3: observability runtime hardening.
- V5.B.4: auth, Kubernetes / Helm, release, and rollback hardening.

## Completion Signal

TASK_COMPLETE
