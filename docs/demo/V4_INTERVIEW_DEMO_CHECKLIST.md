# V4 Interview Demo Checklist

This checklist is the interview-facing path for explaining AfterSale-Agent V4. It is a local development and project
review guide, not a production deployment guide.

V4 status: completed. Use the final record and release summary as the closing references:

- `version-updates/EXEC_PLAN_V4_FINAL_COMPLETION_RECORD.md`
- `version-updates/V4_RELEASE_SUMMARY.md`

## Demo Goal

- 5 minutes: introduce the project purpose and safety model.
- 10 minutes: explain the architecture boundaries.
- 10 minutes: walk through RAG, ToolRegistry, ToolCallTrace, Workspace, and Execution Tree evidence.
- 5 minutes: explain quality gates, offline validation, and future work.

## Pre-demo Checks

- JDK 17+ is available.
- Maven 3.9+ is available.
- Use the default profile unless explicitly demonstrating an opt-in live path.
- No API Key is required for the default demo.
- Docker is not required for the default demo.
- PGvector is not required for the default demo.
- Run `mvn test` before the interview if time permits.
- Run `mvn test -Dtest=ArchitectureTest` to show architecture boundary checks.
- Swagger UI and `/actuator/health` are optional local viewing aids.

## Recommended Walkthrough Order

1. Start from the README project positioning.
2. Use `ARCHITECTURE.md` to explain the modular monolith and Agent boundaries.
3. Explain ToolRegistry, Approval, and ToolCallTrace as the execution safety model.
4. Open `docs/demo/V4_RAG_DEMO_SCRIPT.md` for the HYBRID policy evidence demo.
5. Show Execution Tree as a read-only explanation view.
6. Use `docs/evaluation/EVALUATION.md` to explain deterministic RAG evaluation metrics.
7. Show `/actuator/health` as offline readiness diagnostics.
8. Show Swagger UI or `docs/api/OPENAPI.md` for existing API documentation.
9. Close with `docs/quality/VALIDATION_COMMANDS.md` and the default offline verification boundary.
10. State future work without presenting it as completed runtime capability.

## Common Interview Questions

### How does RAG data enter the system?

- V4 has a policy ingestion foundation for document metadata, chunking, checksum, dedup, and fake-provider embedding
  pipeline tests.
- The ingestion path is an admin/offline pipeline, not an Agent runtime tool.
- Default validation uses fake / in-memory storage and does not require PGvector.
- Optional live provider and PGvector paths are documented separately.

### How is vector retrieval implemented?

- The application layer depends on an embedding abstraction and a vector repository contract.
- The default path uses deterministic fake / in-memory implementations for tests and demos.
- PGvector is an opt-in profile boundary; live connectivity validation is not part of the default test path.

### Why use fake / in-memory defaults?

- It keeps `mvn test` deterministic and suitable for local review.
- It prevents accidental dependence on API keys, Docker, databases, or external network.
- It lets architecture, trace, evidence, and evaluation behavior be verified mechanically.

### How does Spring AI fit?

- Spring AI is behind adapter boundaries for chat and embedding providers.
- The default profile does not create live model calls.
- Live provider checks require explicit opt-in and provider configuration.

### Why is LangChain not the main path?

- This project is a Java Spring Boot backend and keeps Agent execution inside the JVM boundary.
- ToolRegistry, Approval, ToolCallTrace, Workspace, and Execution Tree are first-class Java contracts.
- A sidecar is future work only if explicitly scoped.

### What is the difference between Tool and Skill?

- Tool is an atomic executable capability and must run through ToolRegistry.
- Skill is a composite capability concept for orchestrating safe steps.
- Skill does not replace ToolRegistry and does not bypass risk or approval boundaries.

### Can the LLM directly call tools?

- No. Planner output is a structured plan.
- Java application code validates the plan and invokes tools through ToolRegistry.
- High-risk actions remain approval-gated.

### How are high-risk operations protected?

- RiskPolicy classifies actions.
- Approval requests are created for high-risk proposed actions.
- The demo does not execute real refund, exchange, coupon compensation, payment, logistics, or dispute closure.

### How can you prove the default path is offline?

- Run `mvn test`.
- Run `mvn test -Dtest=ArchitectureTest`.
- Review `DefaultOfflineValidationTest` and `LiveTestSkipClosureTest`.
- Review `docs/quality/VALIDATION_COMMANDS.md`.

### How do you debug an AgentRun?

- Read ToolCallTrace for tool input and output audit.
- Read Workspace for single-run working memory summaries.
- Read Execution Tree for a read-only explanation view.
- Use final summary text only as a presentation layer, not as the audit source of truth.

### What is the relationship between RAG evidence and business decisions?

- RAG evidence is policy evidence only.
- Evidence score is a retrieval score, not business decision confidence.
- Business actions remain controlled by application services, risk rules, and approval boundaries.

## Demo Fallbacks

- If the app does not start, show README, architecture docs, docs harness tests, and validation commands.
- If Swagger UI is unavailable, show `docs/api/OPENAPI.md` and `/v3/api-docs` expectations.
- If RAG output is empty, show deterministic RAG evaluation cases and expected metrics.
- If a live provider key is missing, explain that the default demo is intentionally offline.
- If PGvector is unavailable, use the fake / in-memory RAG path and the optional PGvector setup doc.

## Boundary Reminder

The interview demo does not claim production deployment, production monitoring, real refund, real exchange, real coupon
compensation, real payment, real logistics, live PGvector connectivity, or live provider availability. Those are
future or opt-in paths unless a later execution plan implements and validates them.
