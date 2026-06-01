# 项目审查修正决策：可观测性加固方案

Date: 2026-06-01

Status: Completed

## Context

项目审查指出当前可观测性仍偏基础：已有 MDC 结构化日志和 Actuator health，但没有 Micrometer production
metrics、Prometheus、Grafana、OpenTelemetry、跨服务 trace-id 传播或生产监控看板。这个判断基本准确，但需要
区分“当前已经存在的诊断能力”和“后续生产加固能力”。

本阶段只形成可观测性加固决策、文档路线和 docs harness test，不接入新的 runtime 监控组件。

## Current Observability Baseline

当前已经具备：

- `X-Request-Id` 请求关联，缺失时由应用生成，存在时原样透传到响应头；
- MDC / structured logging 字段：`requestId`、`ticketId`、`agentRunId`、`subtaskId`、`toolName`、
  `approvalRequestId`；
- Ticket、AgentRun、Specialist Handler、ToolRegistry、Approval 和 Execution Tree 路径的诊断日志；
- ToolCallTrace 作为工具调用审计记录；
- ApprovalRequest 作为高风险动作人工审核记录；
- Execution Tree 作为只读解释视图；
- `/actuator/health` 默认暴露；
- RAG search、vector-store、embedding、ingestion 的 offline readiness health diagnostics；
- OpenAPI / Swagger UI 作为本地 API review 入口；
- RAG evaluation metrics / offline RAG evaluation metrics 作为离线评测结果，不是生产 telemetry。

当前缺口：

- 没有 Prometheus registry；
- 没有 Grafana dashboard；
- 没有 OpenTelemetry tracing；
- 没有 collector；
- 没有跨服务 trace-id 传播；
- 没有 provider latency / cost metrics；
- 没有 AgentRun / ToolCall / RAG search 的 production metrics；
- 没有生产日志采集方案；
- health 不是 live provider、live PGvector 或生产依赖连通性的证明。

## Problem Statement

如果直接接入 Prometheus、Grafana 或 OpenTelemetry，容易把生产监控能力误写成已完成，也可能把外部 collector
或 monitoring platform 引入默认验证路径。当前更合适的做法是先明确指标、trace、Actuator 暴露和 secret
safety 边界，再在后续阶段按 opt-in 路径实现。

## Decision

阶段 2 决策如下：

- 保持当前 runtime 行为不变；
- 不新增 Prometheus、Grafana、OpenTelemetry、collector 或外部日志平台依赖；
- 不修改 `pom.xml`、`application.yml`、Actuator health indicator 或 OpenAPI runtime；
- 默认 actuator exposure 继续只包含 `health`；
- 当前 tracing 继续是 MDC-only；
- ToolCallTrace 继续是工具审计 source of truth；
- Execution Tree 继续是只读解释视图；
- RAG health 继续是 offline readiness diagnostics，不执行 live provider 或 live vector checks；
- 后续 metrics / tracing 实现必须显式 opt-in，不能进入默认 `mvn test` 外部依赖路径。

## Metrics Strategy

后续 metrics 设计应基于 Micrometer abstraction，但本阶段不实现。候选指标包括：

- `agent_run_total`
- `agent_run_failed_total`
- `agent_run_duration_seconds`
- `agent_run_waiting_approval_total`
- `tool_call_total`
- `tool_call_failed_total`
- `tool_call_duration_seconds`
- `tool_call_by_tool_name`
- `approval_request_total`
- `approval_request_approved_total`
- `approval_request_rejected_total`
- `rag_search_total`
- `rag_search_empty_result_total`
- `rag_search_fallback_total`
- `rag_search_mode_total`
- `rag_vector_side_failure_total`
- `rag_evidence_count`
- `llm_provider_call_total`
- `llm_provider_call_failed_total`
- `llm_provider_latency_seconds`
- `embedding_provider_call_total`
- `embedding_provider_call_failed_total`
- `embedding_provider_latency_seconds`

指标标签必须低基数、可聚合、无敏感信息。禁止把 API keys、passwords、tokens、完整 prompt、长 user text、
raw dataset path、local absolute path 或 customer PII 放进 metrics label。

## Tracing Strategy

当前 tracing 策略保持 MDC-only：

- HTTP request 使用 `requestId` 关联日志；
- AgentRun、subtask、tool、approval 使用业务 ID 进入 MDC；
- ToolCallTrace 记录工具输入、输出、状态、错误和耗时；
- Execution Tree 读取 AgentRun、ToolCallTrace、ApprovalRequest 和 Workspace 摘要，不执行新的业务动作。

OpenTelemetry 是 future / opt-in path，只在以下条件明确后推进：

- 存在跨服务调用或异步 AgentRun event model；
- 需要跨 JVM / 服务边界传播 trace context；
- 有明确 collector 和 exporter 部署方案；
- 能保证默认测试不依赖 collector、外部网络或监控平台；
- 能保证 trace span 不包含 secrets、full prompts、raw text 或本地路径。

## Logging Strategy

日志继续作为诊断面，而不是审计事实来源：

- 日志用于按 `requestId`、`ticketId`、`agentRunId`、`toolName` 排查；
- ToolCallTrace、ApprovalRequest 和 Execution Tree 仍是审计与解释面；
- 日志不得记录 API keys、database passwords、tokens、完整 prompt、provider raw response、raw dataset path、
  local absolute path 或长原始用户文本；
- 后续如接日志平台，应作为部署层 opt-in，不进入默认 Maven 验证。

## Actuator Exposure Strategy

默认暴露边界保持不变：

- 默认只暴露 `/actuator/health`；
- 不默认暴露 `/actuator/env`；
- 不默认暴露 `/actuator/beans`；
- 不默认暴露 `/actuator/configprops`；
- 不默认暴露 `/actuator/heapdump`；
- 不默认暴露 `/actuator/threaddump`；
- 不默认暴露 `/actuator/prometheus`。

Health endpoint 仍然是 readiness signal：

- RAG health 不调用真实 LLM；
- RAG health 不调用真实 embedding provider；
- RAG health 不连接 PostgreSQL / PGvector；
- RAG health 不调用 Spring AI `VectorStore`；
- RAG health 不调用 ToolRegistry；
- RAG health 不创建 AgentRun；
- RAG health 不写 ToolCallTrace；
- health details 必须 sanitize。

## RAG / Agent Observability Strategy

RAG 与 Agent 的后续可观测性应保持 evidence-only 和审计边界：

- `search_aftersale_policy` 仍是 LOW-risk read-only ToolRegistry tool；
- RAG evidence 是 policy evidence，不是业务动作，也不是业务决策置信度；
- evidence score 是 retrieval score；
- RAG evaluation metrics 是离线质量评测，不是生产 telemetry；
- AgentRun metrics 可以记录数量、耗时、失败、等待审批等聚合信号；
- Tool metrics 可以记录 toolName 级别的调用量、失败量和耗时；
- 不允许 metrics 或 tracing 绕过 ToolRegistry、RiskPolicy、Approval、ToolCallTrace、Workspace 或 Execution Tree。

## Secret Safety

可观测性输出不得泄露：

- API keys；
- database passwords；
- tokens；
- private endpoints with credentials；
- full prompts；
- full provider config；
- raw provider responses；
- long raw user text；
- raw dataset paths；
- local absolute paths；
- customer private data。

OpenAPI examples、health details、logs、metrics labels、trace attributes 和 docs 均遵守同一 secret safety boundary。

## Default Offline Boundary

默认验证仍不需要：

- real LLM；
- API Key；
- PostgreSQL；
- PGvector；
- Docker；
- MySQL；
- Redis；
- external network；
- Prometheus；
- Grafana；
- OpenTelemetry collector；
- external logging platform；
- real embedding provider；
- Spring AI live provider calls。

## Non-goals

本阶段不做：

- Prometheus registry 接入；
- Grafana dashboard；
- OpenTelemetry tracing；
- collector 配置；
- 外部日志平台；
- metrics endpoint 暴露；
- Actuator exposure 修改；
- runtime business metrics instrumentation；
- provider latency / cost metrics 实现；
- production monitoring implementation；
- production deployment；
- production authentication / authorization；
- 真实退款、换货、优惠券补偿、支付或物流系统接入。

## Alternatives Considered

### 直接接 Prometheus

暂不采用。原因是当前阶段目标是事实口径和方案闭环，直接引入 registry 会扩大依赖面，并需要明确 endpoint
exposure、安全和部署边界。

### 直接接 OpenTelemetry

暂不采用。当前是模块化单体和同步 demo path，跨服务 tracing 价值有限。等异步 AgentRun、多服务调用或部署
拓扑明确后，再作为 opt-in path 评估。

### 继续只保留 README 说明

不采用。项目审查已经明确指出可观测性缺口，需要决策记录把当前基线、缺口、边界和未来策略固定下来。

## Consequences

收益：

- 文档明确当前可观测性能力和未完成项；
- 后续 metrics / tracing 有指标命名和安全边界起点；
- 默认离线验证不被外部监控依赖污染；
- Actuator 敏感 endpoint 暴露边界保持清晰。

代价：

- 本阶段不会增加生产 metrics；
- 本阶段不会提供 dashboard；
- 本阶段不会提供跨服务 distributed tracing；
- 后续实现仍需要单独任务、测试和安全评审。

## Follow-ups

- 阶段 3：API 完整性改进；
- 后续可单独实现 Micrometer metrics instrumentation；
- 后续可单独评估 Prometheus endpoint opt-in；
- 后续可在部署拓扑明确后评估 OpenTelemetry；
- 后续可增加 provider latency / cost metrics，但必须避免 secret 和高基数标签。

## Completion Signal

TASK_COMPLETE
