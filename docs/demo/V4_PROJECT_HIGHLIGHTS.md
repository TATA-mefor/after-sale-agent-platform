# V4 Project Highlights

This page summarizes AfterSale-Agent V4 for project review and interviews. It describes implemented repository
capabilities and explicit boundaries; it is not a production rollout statement.

V4 status: completed. The final completion record is
`docs/exec-plans/completed/EXEC_PLAN_V4_FINAL_COMPLETION_RECORD.md`; the concise release summary is
`docs/release/V4_RELEASE_SUMMARY.md`.

## Interview Summary

- Enterprise after-sale ticket Agent platform built on Spring Boot.
- Agent execution is auditable through ToolCallTrace, Workspace summaries, and a read-only Execution Tree.
- LLM planning is separated from tool execution; ToolRegistry remains the only Agent tool execution entry.
- RiskPolicy and Approval protect high-risk proposed actions.
- `search_aftersale_policy` supports KEYWORD, VECTOR, and HYBRID policy evidence retrieval.
- RAG evidence is evidence-only policy support, not a business action or decision confidence score.
- Spring AI, embedding, and vector repository integrations are abstracted behind contracts.
- Default validation is offline and does not require real LLMs, API keys, databases, Docker, or external network.

## Technology Stack

- Spring Boot backend.
- Spring AI adapter foundation.
- OpenAI / DashScope provider adapter boundary.
- RAG and PGvector foundation.
- ToolRegistry and SkillRegistry concepts.
- Approval, ToolCallTrace, AgentWorkspace, and Execution Tree.
- OpenAPI / Swagger UI documentation.
- Actuator health indicators for offline readiness.
- Maven, JUnit 5, ArchUnit, Checkstyle, SpotBugs, and docs harness tests.

## V4 Key Capabilities

- Tool / Skill layer: Tool is atomic execution; Skill is composite capability modeling and does not replace
  ToolRegistry.
- Provider abstraction: real providers are opt-in; fake providers support deterministic tests.
- Embedding abstraction: embedding clients are contracts, not direct Agent dependencies.
- Vector repository contract: retrieval uses an application contract and keeps PGvector infrastructure isolated.
- Fake vector store: default offline tests can verify search and merge behavior.
- Policy ingestion foundation: document metadata, chunking, checksum, dedup, and fake embedding pipeline.
- Hybrid RAG search: KEYWORD, VECTOR, and HYBRID retrieval modes for after-sale policy evidence.
- Evidence observability: policy evidence can appear in ToolCallTrace, Workspace summaries, and Execution Tree.
- RAG evaluation: deterministic retrieval metrics without LLM-as-judge.
- Spring Boot completeness: Actuator health and OpenAPI docs without widening default external dependencies.

## Engineering Quality

- ArchUnit checks enforce Agent, Handler, Skill, Tool, RAG, infrastructure, OpenAPI, and health indicator boundaries.
- Checkstyle and SpotBugs remain part of the default validation path.
- Default tests are offline and deterministic.
- Live provider, MySQL, and PGvector checks are opt-in paths.
- Docs harness tests check completion records, secret safety, evidence-only wording, and interview demo docs.

## Not Completed / Future Work

- Production authentication and authorization.
- Real payment, logistics, refund, exchange, coupon compensation, or dispute-closure integrations.
- `JdbcPolicyVectorRepository` implementation and live PGvector integration validation.
- Production ingestion admin UI.
- Production monitoring and alerting.
- LangChain sidecar integration.

These future items should only be described as planned or opt-in until a later execution plan implements and validates
them.
