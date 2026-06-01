# 项目审查修正阶段 2：可观测性加固方案

Date: 2026-06-01

Status: Completed

## Goal

为项目审查中指出的可观测性缺口形成中文决策记录、整改口径和 docs harness coverage，明确当前基线、未来
metrics / tracing 策略、Actuator 暴露边界、secret safety 和默认离线验证边界。

## Scope Completed

- 新增可观测性加固决策记录；
- 更新中文整改方案，标记阶段 2 completed；
- 更新 README、生产配置模板说明、验证命令和质量文档；
- 更新项目审查修正 active plan；
- 新增 docs harness test，验证阶段 2 文档、边界和 secret safety；
- 保持 runtime 行为不变。

## What Changed

- `docs/decisions/DECISION_PROJECT_REVIEW_OBSERVABILITY_HARDENING.md` 记录当前 observability baseline、问题、
  决策、metrics strategy、tracing strategy、logging strategy、Actuator exposure strategy、RAG / Agent
  observability strategy、secret safety 和 default offline boundary。
- `docs/quality/PROJECT_REMEDIATION_PLAN.md` 将阶段 2 从 planned 更新为 completed。
- `docs/quality/VALIDATION_COMMANDS.md` 增加阶段 2 docs harness 验证说明。
- `docs/quality/QUALITY_SCORE.md` 增加阶段 2 质量状态。
- `docs/deploy/PRODUCTION_CONFIG_TEMPLATE.md` 补充可观测性与 actuator 模板边界。
- `README.md` 链接可观测性决策文档。
- `src/test/java/com/example/aftersale/docs/ObservabilityHardeningDecisionDocsTest.java` 增加只读文档测试。

## Observability Decision Boundary

阶段 2 只做决策和文档闭环，不接入新的 runtime 监控组件。当前可观测性基线是 MDC / structured logs、
ToolCallTrace、ApprovalRequest、Execution Tree、Actuator health、RAG readiness diagnostics、OpenAPI docs 和离线
RAG evaluation metrics。

## Metrics Strategy Boundary

本阶段只定义 future metrics 候选方向，例如 AgentRun、ToolCall、Approval、RAG search、LLM provider 和 embedding
provider 的聚合指标。不实现 Micrometer instrumentation，不引入 Prometheus registry，不暴露 Prometheus endpoint，
不创建 dashboard。

## Tracing Strategy Boundary

当前 tracing 继续保持 MDC-only。OpenTelemetry 是 future / opt-in path，只有在异步 AgentRun、多服务调用或部署拓扑
明确后才单独评估。本阶段不接 collector、不导出 spans、不改变 requestId 行为。

## Actuator Exposure Boundary

默认 actuator exposure 继续只包含 `health`。本阶段不默认暴露 env、beans、configprops、heapdump、threaddump、
prometheus 或其他敏感 endpoint。RAG health 仍是 offline readiness diagnostics，不执行 live provider、PGvector、
ToolRegistry 或 AgentRun 检查。

## Secret Safety Boundary

文档明确 logs、metrics labels、trace attributes、health details、OpenAPI examples 和 docs 都不得包含 API keys、
database passwords、tokens、full prompts、raw provider responses、raw dataset paths、本地绝对路径或客户隐私数据。

## Runtime Non-change Boundary

本阶段未修改 `src/main/java` runtime/business code，未修改 `pom.xml`，未修改 `application.yml`，未修改
ToolRegistry、`search_aftersale_policy`、RAG runtime、ingestion pipeline、Actuator health indicators、OpenAPI config、
ToolCallTrace、Workspace、ExecutionTreeApplicationService 或 AgentApplicationService。

## Default Offline Boundary

默认验证仍不需要 real LLM、API Key、PostgreSQL、PGvector、Docker、MySQL、Redis、external network、Prometheus、
Grafana、OpenTelemetry collector、external logging platform、real embedding provider 或 Spring AI live provider calls。

## Validation Commands

```bash
mvn test -Dtest=ObservabilityHardeningDecisionDocsTest,ProductionConfigTemplateDocsTest,ProjectRemediationPlanDocsTest
mvn test
mvn checkstyle:check
mvn spotbugs:check
mvn test -Dtest=ArchitectureTest
```

## Known Limitations

- 阶段 2 不实现 production metrics；
- 阶段 2 不实现 Prometheus endpoint；
- 阶段 2 不实现 Grafana dashboard；
- 阶段 2 不实现 OpenTelemetry；
- 阶段 2 不实现生产日志采集；
- 阶段 2 不证明 live provider 或 live PGvector 连通性；
- 阶段 2 不修复 API 分页、异步 AgentRun、SSE / WebSocket 或批量 API。

## Follow-ups

- 阶段 3：API 完整性改进；
- 后续可单独实现 Micrometer metrics instrumentation；
- 后续可单独评估 Prometheus endpoint opt-in；
- 后续可在部署拓扑明确后评估 OpenTelemetry；
- 后续可增加 provider latency / cost metrics；
- 后续所有 observability 输出都必须继续遵守 secret safety 和 low-cardinality label 边界。

## Completion Signal

TASK_COMPLETE
