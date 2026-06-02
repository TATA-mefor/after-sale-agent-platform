## V4 事实口径

V4 completed 表示 foundation / demo / interview-grade 阶段完成，不表示生产部署完成。

- 当前项目不是空的 Spring Boot skeleton：Ticket、AgentRun、Approval、ToolCallTrace、Workspace、Execution Tree、
  RAG evaluation、Actuator health 和 OpenAPI docs 已存在。
- Ticket 不是纯贫血模型：它有状态流转和 terminal-state guard；Order 当前更薄，更接近只读模型。
- PGvector 当前是 profile、schema、compose、docs、repository contract、fake / in-memory vector store、默认离线
  测试边界，以及 V5.A.1 新增的显式 opt-in `JdbcPolicyVectorRepository`；默认 live PGvector write/search、
  Spring AI VectorStore production path 和 live PGvector integration validation 仍是 future / opt-in。
- V5.A.2 为 `schema-rag-postgres.sql` 增加 schema version baseline `2026-06-01-001`，用于
  `JdbcPolicyVectorRepository` / PGvector policy evidence search 的初始化口径。它不是 Flyway / Liquibase
  migration framework。
- V5.A.3 adds an explicit opt-in PGvector connectivity smoke test for `JdbcPolicyVectorRepository`. It only runs with
  `mvn test -Dtest=JdbcPolicyVectorRepositorySmokeTest -Dlive.rag=true` and the existing
  `AFTERSALE_PGVECTOR_URL`, `AFTERSALE_PGVECTOR_USERNAME`, `AFTERSALE_PGVECTOR_PASSWORD`, and optional
  `AFTERSALE_PGVECTOR_SCHEMA` variables. Default `mvn test` does not run live PGvector smoke.
- V5.A completed the RAG production path foundation: V5.A.1 opt-in JDBC adapter, V5.A.2 schema baseline,
  V5.A.3 opt-in connectivity smoke, and V5.A.4 docs / completion record closure. This is not production deployment,
  RAG quality enhancement, real embedding quality validation, Flyway / Liquibase migration management, or Spring AI
  `VectorStore` production enablement.
- `docker-compose-rag.yml` 提供本地 PGvector infrastructure，不是完整 app + PGvector 生产部署方案。
- 当前 HTTP API 是 demo/backend API surface：Ticket create/get/list pagination、AgentRun create/status read、
  trace / execution-tree 只读视图、Approval pending/get/approve/reject、Actuator health 和 OpenAPI docs；
  它不是完整生产 CRUD 平台。
- Spring AI 当前是 adapter foundation；阶段 4 已完成
  [Spring AI 深化评估](docs/decisions/DECISION_PROJECT_REVIEW_SPRING_AI_DEEPENING.md)，不代表已经实现
  ChatMemory、Advisors、Tool Calling API 或 bulk embedding runtime。
- RAG 当前支持 KEYWORD / VECTOR / HYBRID policy evidence retrieval；阶段 5 已完成
  [RAG 检索质量改进评估](docs/decisions/DECISION_PROJECT_REVIEW_RAG_QUALITY_IMPROVEMENT.md)，但 reranking、
  query rewriting、RRF 和 chunk window expansion 仍是 future / opt-in，不是当前 runtime 能力。
- 当前 observability 覆盖 MDC / structured logs、ToolCallTrace、Execution Tree、Actuator health 和 RAG readiness
  diagnostics；Prometheus registry、metrics dashboard、distributed tracing 和 cross-service trace-id propagation
  仍是 V5 / future work。
- RAG evidence 是政策证据，不是业务决策，也不执行退款、换货、补偿、支付、物流或争议关闭。
- `search_aftersale_policy` 仍是 LOW-risk read-only ToolRegistry tool；ToolRegistry 仍是 Agent tool execution
  entry；Skill 不替代 ToolRegistry；Policy ingestion 仍是 admin/offline pipeline，不是 Agent runtime tool。
