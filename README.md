# AfterSale-Agent Platform

AfterSale-Agent is a Java Spring Boot platform for auditable e-commerce after-sale ticket handling with Agent execution traces.

## Requirements

- Java 17+
- Maven 3.9+

## Run Locally

```bash
mvn spring-boot:run
```

Health checks:

```bash
curl http://localhost:8080/api/health
curl http://localhost:8080/actuator/health
```

Ticket APIs:

```bash
curl -X POST http://localhost:8080/api/tickets \
  -H "Content-Type: application/json" \
  -d '{"userId":"U-1001","orderId":"O-1001","message":"Item arrived damaged."}'

curl http://localhost:8080/api/tickets/{ticketId}
```

Agent run APIs:

```bash
curl -X POST http://localhost:8080/api/tickets/{ticketId}/agent-runs
curl http://localhost:8080/api/agent-runs/{runId}/traces
```

API responses use a shared envelope:

```json
{
  "code": "SUCCESS",
  "message": "ok",
  "data": {}
}
```

## Validate

```bash
mvn test
mvn checkstyle:check
mvn spotbugs:check
mvn test -Dtest=ArchitectureTest
```

Start with these project documents:

- `SPEC.md`
- `WORKFLOW.md`
- `AGENTS.md`
- `ARCHITECTURE.md`
- `EXEC_PLAN_V1.md`
