# V4 Release Summary

Date: 2026-05-29

Status: Completed

## Project Summary

AfterSale-Agent V4 turns the Spring Boot after-sale Agent platform into an interview-ready Java AI backend foundation:
Agent planning stays controlled, tools execute through ToolRegistry, high-risk actions remain approval-gated, and RAG
policy retrieval produces auditable evidence without making live providers part of the default path.

中文事实口径：V4 completed 表示 foundation / demo / interview-grade 阶段完成，不表示 production deployment、
production auth、production monitoring、live PGvector validation 或真实外部业务系统集成已完成。

## What V4 Delivered

- Tool / Skill layer foundation.
- Spring AI chat and embedding adapter boundaries.
- PGvector profile and vector repository foundation.
- Policy ingestion, chunking, checksum deduplication, and fake-provider embedding pipeline.
- KEYWORD / VECTOR / HYBRID `search_aftersale_policy` retrieval.
- RAG evidence in ToolCallTrace, Workspace, and Execution Tree.
- RAG evaluation cases and metrics.
- Offline-safe Actuator health indicators.
- OpenAPI / Swagger UI documentation for existing APIs.
- Documentation consistency, architecture closure, interview demo polish, and final V4 completion record.

## Technical Highlights

- Modular monolith with API, application, domain, and infrastructure boundaries.
- Planner-only LLM contract: Java validates and executes.
- ToolRegistry remains the Agent tool execution entry.
- RiskPolicy and Approval keep high-risk actions out of automatic execution.
- ToolCallTrace is the audit source of truth.
- Workspace is single-run memory.
- Execution Tree is a read-only explanation view.

## RAG / Spring AI / PGvector Highlights

- RAG retrieval returns policy evidence, not business decisions.
- `search_aftersale_policy` supports KEYWORD, VECTOR, and HYBRID retrieval modes.
- Spring AI concrete clients stay inside infrastructure adapters.
- Fake embedding and fake vector store keep default tests deterministic.
- PGvector is an explicit opt-in profile; default validation does not connect to PostgreSQL or PGvector.
- PGvector delivery means profile, schema, compose docs, repository contract, and fake / in-memory default store. It
  does not mean `JdbcPolicyVectorRepository`, live PGvector write/search, or Spring AI VectorStore production path.
- Current addendum: V5.A.1 later adds an explicit opt-in `JdbcPolicyVectorRepository` infrastructure adapter. This
  does not change the V4 release boundary, does not add live PGvector validation to default tests, and does not enable
  Spring AI VectorStore production use.
- Spring AI delivery means adapter foundation. It does not mean ChatMemory, Advisors, Tool Calling API, or bulk
  embedding are used in the default runtime.
- RAG retrieval does not yet include reranking, query rewriting, RRF, or chunk window expansion.

## Tool / Skill / Approval / Trace Highlights

- Tool is the atomic execution unit.
- Skill is a composite capability and does not replace ToolRegistry.
- High-risk proposed actions remain approval-gated.
- Tool calls continue to produce ToolCallTrace records.
- RAG evidence is visible through trace, workspace, and execution tree structures.

## Spring Boot Completeness Highlights

- Typed configuration properties for provider and RAG boundaries.
- Offline Actuator health indicators for RAG components.
- OpenAPI `/v3/api-docs` and Swagger UI for existing API documentation.
- Default actuator exposure remains limited to health.

## Evaluation / Quality Highlights

- Default offline JUnit tests.
- ArchUnit architecture boundaries.
- Checkstyle and SpotBugs quality gates.
- RAG evaluation metrics.
- Docs harness tests for final status, secret / path safety, and documentation boundaries.
- Live provider and live infrastructure checks are explicit opt-in paths.

## How To Validate

```bash
mvn test
mvn checkstyle:check
mvn spotbugs:check
mvn test -Dtest=ArchitectureTest
```

These commands are the V4 final default validation gate and do not require API keys, PostgreSQL, PGvector, Docker,
MySQL, Redis, real LLMs, real embedding providers, Spring AI VectorStore, or external network.

## How To Demo

- Start with `README.md`.
- Use `docs/demo/V4_INTERVIEW_DEMO_CHECKLIST.md` for the interview sequence.
- Use `docs/demo/V4_PROJECT_HIGHLIGHTS.md` for the short capability summary.
- Use `docs/demo/V4_RAG_DEMO_SCRIPT.md` for RAG evidence.
- Use `docs/api/OPENAPI.md` for Swagger UI and API grouping.
- Use `docs/quality/VALIDATION_COMMANDS.md` for validation and live opt-in boundaries.

## What Is Intentionally Not Production / Live

V4 does not complete production auth, production deployment, production monitoring, real refund, real exchange, real
payment, real logistics, real inventory, real coupon compensation, live PGvector validation, or production ingestion
admin UI. Those remain future or explicit opt-in work. V5.A.1 later adds an opt-in JDBC vector repository adapter, but
does not turn live PGvector validation into a default gate.

The current HTTP surface is a demo/backend API surface, not a complete production CRUD platform. It covers ticket
create/get, AgentRun create, ToolCallTrace and Execution Tree read-only views, Approval pending/get/approve/reject,
Actuator health, and OpenAPI docs.

The project review correction record is
`docs/exec-plans/completed/EXEC_PLAN_PROJECT_REVIEW_CORRECTION_STAGE0.md`.

系统性补丁阶段 0-6 current correction scope completed：阶段 0 修正文档事实口径，阶段 1 补生产配置模板，
阶段 2 补可观测性加固决策，阶段 3 补 API 完整性路线与最小只读 API 改进，阶段 4 补 Spring AI 深化评估，
阶段 5 补 RAG 检索质量改进评估，阶段 6 补部署加固路线。阶段 6 只完成 deployment hardening decision /
roadmap，不表示 Dockerfile、CI/CD、Kubernetes / Helm、secret manager、production auth/RBAC、production
monitoring、live PGvector validation 或 production deployment 已完成。

Deployment hardening docs:

- `docs/decisions/DECISION_PROJECT_REVIEW_DEPLOYMENT_HARDENING.md`
- `docs/deploy/DEPLOYMENT_HARDENING_ROADMAP.md`
- `docs/exec-plans/completed/EXEC_PLAN_PROJECT_REVIEW_CORRECTION_STAGE6_DEPLOYMENT_HARDENING_ROADMAP.md`

## Future Roadmap

- V5 production hardening.
- Production auth and security.
- Live PGvector validation.
- Production ingestion API or admin UI.
- Real payment, logistics, refund, exchange, and coupon compensation integrations.
- Observability and metrics hardening.
- Deployment hardening.
