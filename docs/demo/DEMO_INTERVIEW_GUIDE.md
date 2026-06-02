## Interview Quick Guide

AfterSale-Agent is an enterprise after-sale ticket Agent platform built with Spring Boot. The review path is designed
to show Agent planning, ToolRegistry-controlled tool execution, risk / approval boundaries, ToolCallTrace audit,
single-run Workspace memory, read-only Execution Tree explanation, RAG policy evidence retrieval, Spring AI adapter
foundation, PGvector / vector repository foundation, OpenAPI docs, Actuator health, and default offline validation.

Core interview points:

- LLMs plan only; Java application code validates plans and tools execute through ToolRegistry.
- Agent does not directly execute high-risk business actions.
- RiskPolicy and Approval protect high-risk proposed actions.
- ToolCallTrace is the audit source of truth for tool calls.
- Workspace is single AgentRun working memory, not long-term memory.
- Execution Tree is a read-only explanation view.
- `search_aftersale_policy` supports KEYWORD, VECTOR, and HYBRID policy evidence retrieval.
- RAG evidence is policy evidence only, not a business action or decision confidence score.
- Spring AI, DashScope / OpenAI, PGvector, JDBC vector persistence, and live embedding providers are opt-in paths.
- Default validation does not require real LLMs, API keys, PostgreSQL, PGvector, Docker, MySQL, Redis, or external
  network.

Interview docs:

- [V4 Interview Demo Checklist](docs/demo/V4_INTERVIEW_DEMO_CHECKLIST.md)
- [V4 Project Highlights](docs/demo/V4_PROJECT_HIGHLIGHTS.md)
- [V4 RAG Demo Script](docs/demo/V4_RAG_DEMO_SCRIPT.md)
- [V4 Policy Ingestion Pipeline](docs/demo/V4_POLICY_INGESTION_PIPELINE.md)
- [V4 PGvector Local Setup](docs/demo/V4_PGVECTOR_LOCAL_SETUP.md)
- [Evaluation Docs](docs/evaluation/EVALUATION.md)
- [OpenAPI Docs](docs/api/OPENAPI.md)
- [Validation Commands](docs/quality/VALIDATION_COMMANDS.md)
- [中文项目整改方案](docs/quality/PROJECT_REMEDIATION_PLAN.md)
- [Production Config Template](docs/deploy/PRODUCTION_CONFIG_TEMPLATE.md)
- [V4 Final Completion Record](version-updates/EXEC_PLAN_V4_FINAL_COMPLETION_RECORD.md)
- [V4 Release Summary](version-updates/V4_RELEASE_SUMMARY.md)
- [Project Review Correction Stage 0](version-updates/EXEC_PLAN_PROJECT_REVIEW_CORRECTION_STAGE0.md)
- [Project Review Correction Stage 1](version-updates/EXEC_PLAN_PROJECT_REVIEW_CORRECTION_STAGE1_PROD_CONFIG_TEMPLATE.md)
- [API Completeness Decision](docs/decisions/DECISION_PROJECT_REVIEW_API_COMPLETENESS.md)
- [Async / Streaming / Batch API Decision](docs/decisions/DECISION_PROJECT_REVIEW_ASYNC_STREAMING_BATCH_API.md)
- [Spring AI Deepening Decision](docs/decisions/DECISION_PROJECT_REVIEW_SPRING_AI_DEEPENING.md)
- [RAG Quality Improvement Decision](docs/decisions/DECISION_PROJECT_REVIEW_RAG_QUALITY_IMPROVEMENT.md)
- [Deployment Hardening Decision](docs/decisions/DECISION_PROJECT_REVIEW_DEPLOYMENT_HARDENING.md)
- [Deployment Hardening Roadmap](docs/deploy/DEPLOYMENT_HARDENING_ROADMAP.md)
- [V5.A.1 JdbcPolicyVectorRepository](version-updates/EXEC_PLAN_V5_A1_JDBC_POLICY_VECTOR_REPOSITORY.md)
- [V5.A.3 PGvector Connectivity Smoke Test](version-updates/EXEC_PLAN_V5_A3_PGVECTOR_CONNECTIVITY_SMOKE_TEST.md)
- [V5.A RAG Production Path Completion](version-updates/EXEC_PLAN_V5_A_RAG_PRODUCTION_PATH_COMPLETION.md)
- [V5.A RAG Production Path Summary](version-updates/V5_A_RAG_PRODUCTION_PATH_SUMMARY.md)

Fast validation:

```bash
mvn test
mvn checkstyle:check
mvn spotbugs:check
mvn test -Dtest=ArchitectureTest
```

