# EXEC_PLAN_V3.md

# AfterSale-Agent V3 执行计划

## 1. V3 总目标

V2 已完成 Agent 规划、工具执行、审批、执行树、评测集和 deterministic fallback robustness。V3 不继续堆叠
Agent 行为能力，而是把当前 in-memory 版本收口为更接近可运行系统的基础设施形态：

```text
core data persistence
local MySQL profile
repeatable Docker Compose development environment
structured logging and basic observability
final system review
```

V3 的目标是让项目从“可演示的内存闭环”升级为“可本地复现、可持久化、可诊断的后端系统雏形”，同时保持
V2 已建立的 Agent、ToolRegistry、Approval、Trace、Workspace 和 Evaluation 边界。

## 2. V3 与 V2 的边界

V2 已完成：

- LLM Planner Adapter；
- Structured LLM Planner Client；
- LLM Live Smoke Test；
- Order Query Tools；
- Multi-Intent Planning；
- Specialist Agent Handler；
- Controlled Policy Retrieval；
- Agent Workspace；
- Approval APIs；
- Execution Tree；
- Evaluation Dataset；
- Robustness Improvements。

V3 只推进基础设施收口：

- 持久化核心业务数据；
- 增加本地 MySQL profile；
- 增加本地 Docker Compose 启动路径；
- 增加结构化日志和基础健康检查；
- 做最终系统能力和限制复盘。

V3 不改变：

- LLM 只负责结构化规划；
- Java 后端负责校验和执行；
- ToolRegistry 是唯一工具执行入口；
- Agent / Handler 不直接访问 Repository；
- 高风险动作必须进入人工确认；
- 默认测试必须离线、确定性运行；
- 不执行真实退款、真实换货、真实优惠券补偿、真实支付或真实物流动作。

## 3. V3.1 MySQL Persistence

Status: completed for explicit MySQL profile and Spring JDBC persistence. Docker Compose and observability remain
planned in later V3 stages.

### 3.1.1 目标

为核心业务数据增加可替换的 MySQL 持久化实现，同时保留当前 in-memory/test profile，保证默认测试不依赖本地
MySQL、Docker 或外部网络。

### 3.1.2 范围

V3.1 覆盖：

- Ticket 持久化；
- AgentRun 持久化；
- ToolCallTrace 持久化；
- ApprovalRequest 持久化；
- Order demo data 持久化或 seed 初始化；
- Policy data 持久化或 seed 初始化；
- repository 抽象与 MySQL/JPA/Jdbc infrastructure 实现；
- profile 区分，例如 `test` / `dev-simple` 使用 in-memory，`mysql` 使用 MySQL；
- schema 初始化或 migration 策略；
- 文档化本地配置、环境变量和降级路径；
- 可选 Testcontainers 测试，但不得让默认 `mvn test` 强制依赖 Docker。

### 3.1.3 不做

V3.1 不做：

- 不接真实订单中心；
- 不接真实支付、退款、物流、库存或优惠券系统；
- 不引入 Redis；
- 不做微服务拆分；
- 不让 Controller、Agent 或 Handler 直接访问 Repository；
- 不让 persistence 绕过 ApplicationService；
- 不删除 in-memory/test profile；
- 不让默认测试依赖本地 MySQL、Docker、真实 LLM 或外部网络。

### 3.1.4 验收标准

V3.1 完成时必须满足：

1. Ticket、AgentRun、ToolCallTrace、ApprovalRequest 有 MySQL 持久化路径；
2. Order demo data 和 Policy data 有明确 seed 或持久化初始化策略；
3. 默认 profile 或 test profile 仍可使用 in-memory repository；
4. MySQL profile 可通过环境变量配置连接信息；
5. 不提交真实数据库密码、API Key 或敏感凭证；
6. domain 层不依赖 JPA/Jdbc/Spring Data 等数据库框架；
7. Controller、Agent、Handler 仍不得直接访问 Repository；
8. V2 demo flow 在 in-memory profile 下不退化；
9. 默认 `mvn test` 离线通过；
10. ArchitectureTest、Checkstyle、SpotBugs 继续通过。

### 3.1.5 测试要求

至少覆盖：

- repository contract 测试；
- in-memory profile 回归测试；
- MySQL profile 的显式 opt-in 测试或可手动验证路径；
- schema / seed 初始化验证；
- AgentRun 与 ToolCallTrace 持久化一致性；
- high-risk ApprovalRequest 持久化状态流转；
- 默认测试不依赖 MySQL 或 Docker 的证明。

### 3.1.6 验证命令

默认验证：

```bash
mvn test
mvn checkstyle:check
mvn spotbugs:check
mvn test -Dtest=ArchitectureTest
```

如引入可选 Testcontainers 或 MySQL integration test，必须使用显式 opt-in 命令，并在 README 中说明。

### 3.1.7 完成记录

V3.1 已完成：

- 新增 `mysql` profile；
- 新增 `schema-mysql.sql` 和 `data-mysql.sql`；
- 新增 Ticket、AgentRun、ToolCallTrace、ApprovalRequest、Order、AfterSalePolicy 的 Spring JDBC repository；
- 保留默认 in-memory repository，并通过 profile 测试证明默认路径不创建 `DataSource`；
- MySQL 连接信息只来自环境变量或本地未提交配置；
- domain 层只增加 restore factory，不引入 JPA / Jdbc / Spring Data 依赖；
- README 记录 MySQL profile 启动说明和默认离线路径；
- 默认测试仍不依赖 MySQL、Docker、Redis、真实 LLM、API Key 或外部网络。

V3.1 未做：

- 不新增 Docker Compose；
- 不新增 Redis；
- 不新增 Testcontainers 默认测试；
- 不实现真实退款、真实换货、真实优惠券补偿、真实支付或真实物流。

## 4. V3.2 Docker Compose

Status: completed for local app + mysql Docker Compose startup. Structured logging and final review remain planned in
later V3 stages.

### 4.1 目标

为本地开发和演示提供一键启动路径，使 app 和 MySQL 能在本机以可复现方式启动。Docker Compose 只代表本地
development environment，不代表生产部署。

### 4.2 范围

V3.2 覆盖：

- `app` 服务；
- `mysql` 服务；
- 可选 `redis` 服务，但不得强制引入；
- 本地一键启动命令；
- 环境变量管理；
- `.env.example` 或等价示例配置；
- 不提交任何真实密钥；
- README 中记录启动、停止、清理和常见问题；
- 与 V3.1 MySQL profile 对齐。

### 4.3 不做

V3.2 不做：

- 不把 Docker Compose 作为生产部署方案；
- 不引入 Kubernetes；
- 不引入复杂服务发现；
- 不强制默认测试依赖 Docker；
- 不提交真实数据库密码、API Key 或生产配置；
- 不引入真实支付、物流、退款、优惠券或订单外部系统。

### 4.4 验收标准

V3.2 完成时必须满足：

1. 本地可以通过文档化命令启动 app + mysql；
2. MySQL 数据库、账号、库名通过环境变量或示例 env 管理；
3. README 包含 Docker Compose 启动和降级说明；
4. 默认 Maven 测试仍不依赖 Docker；
5. compose 配置不包含真实密钥；
6. app 健康检查可访问；
7. 停止和清理步骤清晰。

### 4.5 测试要求

至少覆盖：

- 本地 compose 启动 smoke check；
- app health check；
- MySQL profile 连接 smoke check；
- README 命令可复现；
- 默认测试离线稳定。

### 4.6 验证命令

默认验证：

```bash
mvn test
mvn checkstyle:check
mvn spotbugs:check
mvn test -Dtest=ArchitectureTest
```

Docker Compose smoke 命令必须在 README 中作为显式本地验证，不进入默认测试门禁。

### 4.7 完成记录

V3.2 已完成：

- 新增 `docker-compose.yml`；
- 新增 `Dockerfile`；
- 新增 `.dockerignore`；
- Compose 包含 `mysql` 和 `app` 服务；
- MySQL 使用本地占位数据库、账号和密码，并允许通过环境变量覆盖；
- MySQL 初始化挂载 `schema-mysql.sql` 和 `data-mysql.sql`；
- app 服务通过 `SPRING_PROFILES_ACTIVE=mysql` 启动，并连接 compose 内部的 `mysql` 服务；
- README 记录启动、健康检查、停止和清理命令；
- 默认 Maven 测试仍不依赖 Docker、MySQL、Redis、真实 LLM、API Key 或外部网络；
- 新增离线 harness 测试检查 compose profile、schema/seed 挂载、Redis 边界和 secret safety。

V3.2 未做：

- 不新增 Redis；
- 不新增 Kubernetes；
- 不新增生产部署脚本；
- 不改变 Agent、ToolRegistry、Approval、Trace 或 Workspace 业务边界；
- 不实现真实退款、真实换货、真实优惠券补偿、真实支付或真实物流。

## 5. V3.3 Structured Logging / Observability

### 5.1 目标

让关键请求、AgentRun、子任务、工具调用和审批请求具备可检索的结构化日志字段，并保留基础 health check。

### 5.2 范围

V3.3 覆盖：

- `requestId`；
- `ticketId`；
- `agentRunId`；
- `subtaskId`；
- `toolName`；
- `approvalRequestId`；
- structured log 输出；
- actuator health；
- 关键 application service 和 tool execution 路径的日志一致性；
- 不记录 API Key、数据库密码、完整长 prompt、LLM 原始长文本或敏感凭证。

### 5.3 不做

V3.3 不做：

- 不做复杂 Prometheus / Grafana 平台，除非后续明确需要；
- 不接外部日志 SaaS；
- 不引入分布式 tracing 平台；
- 不把日志当作业务状态存储；
- 不用日志替代 ToolCallTrace、Execution Tree 或 persistence。

### 5.4 验收标准

V3.3 完成时必须满足：

1. 关键请求日志带 `requestId`；
2. Ticket / AgentRun / Subtask / Tool / Approval 相关日志带对应业务 ID；
3. 工具调用失败和审批等待有结构化日志；
4. actuator health 可用；
5. 日志不输出真实密钥或敏感凭证；
6. ToolCallTrace 仍然是工具审计记录，不被日志替代；
7. 默认测试不依赖外部 observability 平台。

### 5.5 测试要求

至少覆盖：

- requestId 生成或传播；
- AgentRun 关键日志字段；
- toolName / subtaskId 日志字段；
- approvalRequestId 日志字段；
- actuator health smoke test；
- secret redaction 或敏感字段不输出检查。

### 5.6 验证命令

```bash
mvn test
mvn checkstyle:check
mvn spotbugs:check
mvn test -Dtest=ArchitectureTest
```

## 6. V3.4 Final System Review

### 6.1 目标

对 V1、V2、V3 的系统能力、边界、验证命令、demo flow 和后续方向做最终复盘，形成可交付的项目说明。

### 6.2 范围

V3.4 覆盖：

- 系统能力清单；
- 已知限制；
- 验证命令；
- demo flow；
- infrastructure profile 说明；
- V2 Agent 能力边界回顾；
- V3 persistence / compose / observability 状态；
- 后续方向建议。

### 6.3 不做

V3.4 不做：

- 不新增业务能力；
- 不引入新外部依赖；
- 不扩大 Agent 执行动作边界；
- 不把实验性能力写成已完成；
- 不做生产部署承诺。

### 6.4 验收标准

V3.4 完成时必须满足：

1. README 能完整指导本地 demo；
2. SPEC / ARCHITECTURE / WORKFLOW / QUALITY_SCORE 与实际能力一致；
3. 已知限制清晰列出；
4. 默认验证命令全部通过；
5. V2/V3 边界无冲突；
6. Review Packet 包含最终系统能力和风险说明。

### 6.5 测试要求

V3.4 不新增功能测试，但必须复跑全部默认质量门禁，并手动核对 README demo flow。

### 6.6 验证命令

```bash
mvn test
mvn checkstyle:check
mvn spotbugs:check
mvn test -Dtest=ArchitectureTest
```

## 7. V3 当前状态

```text
V3.1 MySQL Persistence: completed
V3.2 Docker Compose: completed
V3.3 Structured Logging / Observability: planned
V3.4 Final System Review: planned
```

V3.1 已完成显式 MySQL profile、Spring JDBC repository、schema/seed 初始化和默认 in-memory 回归保护。V3.2
已完成本地 app + mysql Docker Compose 启动路径。V3.3 observability 和 V3.4 final review 尚未完成。
