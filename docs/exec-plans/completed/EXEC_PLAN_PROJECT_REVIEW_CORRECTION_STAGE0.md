# 项目审查问题修正阶段 0：文档事实口径修正

Date: 2026-06-01

Status: Completed

## Goal

根据整体项目审查结论，对 V4 文档中的事实口径做中文校准，明确“已完成 foundation / demo /
interview-grade 能力”和“后续 production hardening / live integration 能力”的边界。

## Scope Completed

- 修正 README、V4 总执行计划、历史 active V4 plan、release summary、quality docs、validation docs、PGvector
  local setup docs 和 Policy Ingestion demo docs 中容易造成误读的表述。
- 明确 V4 completed 不等于 production deployment 已完成。
- 明确 PGvector 当前是 profile、schema、compose docs、repository contract、fake / in-memory default store 和
  opt-in boundary，不是默认 live vector persistence。
- 明确当前 HTTP API 是 demo/backend API surface，不是完整生产 CRUD 平台。
- 明确当前 Spring AI 是 adapter foundation，不代表 ChatMemory、Advisors、Tool Calling API 或 bulk embedding
  已成为默认 runtime。
- 明确当前 RAG search 支持 KEYWORD / VECTOR / HYBRID policy evidence retrieval，但 reranking、query rewriting、
  RRF 和 chunk window expansion 仍是 future work。
- 新增 docs harness test，锁定项目审查后的文档事实边界、secret/path safety 和 production overclaim 防线。

## What Changed

- README 增加中文“V4 事实口径”小节。
- V4 总执行计划和历史 active V4 plan 将偏强的 PGvector / VectorStore profile 和 persistence wording 修正为
  “PGvector / vector repository foundation and opt-in profile / boundary”。
- PGvector local setup docs 更新为当前 V4 全局状态：V4.4/V4.5 已完成 offline ingestion foundation 和 HYBRID
  ToolRegistry runtime，但 live PGvector persistence/search 仍未完成。
- Policy Ingestion demo docs 区分 V4.4 阶段边界与 V4.5 已完成的 HYBRID policy search runtime。
- Release summary、quality score、validation commands 增加项目审查后的事实口径。
- 新增 `ProjectReviewCorrectionDocsTest`，只读文档，不启动 Spring context，不连接外部依赖。

## Documentation Fact Correction Boundary

本阶段只修正文档事实口径。它不改变 Controller、Service、ToolRegistry、RAG search、Policy Ingestion、
Actuator health、OpenAPI config、ToolCallTrace、Workspace 或 Execution Tree runtime 行为。

## PGvector / Vector Store Wording Boundary

已完成能力：

- PGvector profile boundary；
- `schema-rag-postgres.sql`；
- `PolicyVectorRepository` contract；
- `InMemoryPolicyVectorRepository` / fake vector store；
- `docker-compose-rag.yml`；
- PGvector local setup docs；
- 架构边界和默认离线测试。

未完成能力：

- `JdbcPolicyVectorRepository`；
- 默认 live PGvector write/search；
- Spring AI `VectorStore` production path；
- live PGvector integration validation。

## API Surface Wording Boundary

当前 HTTP API 口径是 demo/backend API surface：

- Ticket create/get；
- AgentRun create；
- ToolCallTrace / Execution Tree read-only views；
- Approval pending/get/approve/reject；
- Actuator health；
- OpenAPI docs。

本阶段不声明完整生产 CRUD、分页、异步 AgentRun、SSE/WebSocket 流式输出或批量 API 已完成。

## Production Hardening Boundary

V4 completed 不代表以下能力已完成：

- production auth；
- production monitoring；
- production deployment；
- application-prod profile/template；
- Prometheus registry；
- metrics dashboard；
- distributed tracing；
- cross-service trace-id propagation；
- real refund / exchange / payment / logistics / coupon compensation integrations。

这些内容进入 V5 / future work。

## Runtime Non-change Boundary

本阶段没有新增 runtime business feature，没有修改 `search_aftersale_policy` runtime、retrieval algorithm、
RAG evaluation runner、Actuator health behavior、OpenAPI runtime behavior、ToolRegistry 语义、ToolCallTrace schema、
Workspace evidence logic 或 Execution Tree runtime。

## Default Offline Validation Boundary

默认验证仍必须离线、确定性，并且不需要：

- real LLM；
- API Key；
- PostgreSQL；
- PGvector；
- Docker；
- MySQL；
- Redis；
- external network；
- real embedding provider；
- Spring AI live provider calls。

## Validation Commands

计划验证命令：

```bash
mvn test -Dtest=ProjectReviewCorrectionDocsTest,V4DocumentationConsistencyTest,V4FinalCompletionDocsTest,V4InterviewDemoDocsTest
mvn test
mvn checkstyle:check
mvn spotbugs:check
mvn test -Dtest=ArchitectureTest
```

## Known Limitations

- 本阶段不实现 V5 production hardening。
- 本阶段不新增 `application-prod.yml` 或 production profile template。
- 本阶段不新增 Prometheus、metrics dashboard、distributed tracing 或 cross-service trace-id propagation。
- 本阶段不新增 API pagination、async AgentRun、SSE/WebSocket streaming 或 batch API。
- 本阶段不新增 Spring AI ChatMemory、Advisors、Tool Calling API 或 bulk embedding。
- 本阶段不新增 RAG reranking、query rewriting、RRF 或 chunk window expansion。
- 本阶段不实现 `JdbcPolicyVectorRepository` 或 live PGvector validation。

## Follow-ups

- Stage 1：生产配置模板。
- Stage 2：可观测性指标 / tracing 决策。
- Stage 3：API 完整性改进。
- Stage 4：Spring AI 深化使用评估。
- Stage 5：RAG 检索质量改进。
- Stage 6：部署加固路线。

## Completion Signal

TASK_COMPLETE
