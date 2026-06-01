# 项目审查修正阶段 3.1：API Surface Audit / API Completeness Decision

Date: 2026-06-01

Status: Completed

## Goal

完成当前 HTTP API surface 的事实审计和 API completeness 决策，明确当前 API 是 demo/backend surface，不是完整生产
CRUD，并把分页、AgentRun status、异步执行、SSE / WebSocket、batch API 和 production auth 放入后续阶段。

## Scope Completed

- 新增 API 完整性决策记录；
- 更新 README 的 API 口径和文档链接；
- 更新 OpenAPI 文档，补充 API completeness roadmap；
- 更新中文整改方案、active correction plan、质量文档和验证命令；
- 新增只读 docs harness test；
- 保持 runtime 行为不变。

## What Changed

- `docs/decisions/DECISION_PROJECT_REVIEW_API_COMPLETENESS.md` 记录当前 API surface、限制、决策、分页策略、
  AgentRun read/status 策略、异步与流式策略、ToolRegistry 边界、安全边界和默认离线边界。
- `docs/api/OPENAPI.md` 补充 API completeness roadmap，明确 OpenAPI docs 展示 existing APIs。
- `README.md` 链接 API completeness decision，并明确当前 API 不是完整生产 CRUD。
- `docs/quality/PROJECT_REMEDIATION_PLAN.md` 标记阶段 3.1 completed。
- `docs/exec-plans/active/EXEC_PLAN_PROJECT_REVIEW_CORRECTION_PLAN.md` 标记阶段 3.1 completed。
- `docs/quality/QUALITY_SCORE.md` 和 `docs/quality/VALIDATION_COMMANDS.md` 补充阶段 3.1 质量与验证说明。
- `src/test/java/com/example/aftersale/docs/ApiCompletenessDecisionDocsTest.java` 增加只读文档校验。

## API Surface Audit Boundary

当前 API surface 为：

- Ticket create/get；
- AgentRun create/start；
- ToolCallTrace read-only view；
- Execution Tree read-only view；
- Approval pending/get/approve/reject；
- `/api/health`、`/actuator/health`、`/v3/api-docs` 和 Swagger UI。

本阶段不新增 endpoint，不修改现有路径，不改变请求/响应 runtime 行为。

## Pagination Strategy Boundary

分页是阶段 3.2 候选任务，不属于阶段 3.1 已实现能力。当前阶段只记录 page/size 或 cursor、默认 page size、
最大 page size 和 fake/example data 的文档策略。

## AgentRun Read / Status Boundary

AgentRun get/status polling 是阶段 3.3 候选任务，不属于阶段 3.1 已实现能力。Trace 和 Execution Tree 继续作为
只读审计/解释视图，不替代 AgentRun status model。

## Async / Streaming Boundary

异步 AgentRun、SSE / WebSocket streaming 和 batch API 是阶段 3.4 或后续任务，不属于阶段 3.1 已实现能力。后续
实现必须先定义事件模型、安全字段边界、幂等边界和默认离线测试策略。

## ToolRegistry Boundary

`search_aftersale_policy` 仍是 LOW-risk read-only ToolRegistry tool，不是 public RAG HTTP endpoint。LLM 不得直接
执行工具，也不得通过 HTTP API 绕过 ToolRegistry、RiskPolicy、Approval 或 ToolCallTrace。

## Security / Auth Boundary

Production auth / RBAC、idempotency、rate limit 和 API audit hardening 是 future work。本阶段不实现安全 runtime，
也不声明 production API hardening 已完成。

## Runtime Non-change Boundary

本阶段未修改 `src/main/java` runtime/business code，未修改 `pom.xml`、`src/main/resources`、Controller、Service、
ToolRegistry、`search_aftersale_policy`、RAG runtime、ingestion pipeline、health indicators、OpenAPI config、
ToolCallTrace、Workspace、ExecutionTreeApplicationService、AgentApplicationService 或 ArchitectureTest。

## Default Offline Boundary

阶段 3.1 docs harness 只读文档文件，不启动 Spring context，不调用 HTTP，不连接数据库，不调用 LLM、embedding
provider、Docker、Redis、MySQL、PostgreSQL、PGvector、Spring AI VectorStore 或外部网络。默认验证仍保持离线和确定性。

## Validation Commands

```bash
mvn test -Dtest=ApiCompletenessDecisionDocsTest,ObservabilityHardeningDecisionDocsTest,ProductionConfigTemplateDocsTest,ProjectRemediationPlanDocsTest
mvn test
mvn checkstyle:check
mvn spotbugs:check
mvn test -Dtest=ArchitectureTest
```

## Known Limitations

- 阶段 3.1 不实现分页；
- 阶段 3.1 不实现 AgentRun get/status polling；
- 阶段 3.1 不实现异步 AgentRun；
- 阶段 3.1 不实现 SSE / WebSocket；
- 阶段 3.1 不实现 batch API；
- 阶段 3.1 不实现 production auth / RBAC；
- 阶段 3.1 不新增 public RAG HTTP endpoint。

## Follow-ups

- 阶段 3.2：list / pagination foundation；
- 阶段 3.3：AgentRun get/status polling endpoint；
- 阶段 3.4：async AgentRun、SSE / WebSocket 和 batch API 评估；
- 后续安全任务：production auth / RBAC、idempotency、rate limit、audit hardening。

## Completion Signal

TASK_COMPLETE
