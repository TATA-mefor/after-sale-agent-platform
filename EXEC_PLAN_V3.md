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
demo dataset enrichment
order-item-aware order tools
item-specific recommendation
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
- 增加可选 demo 数据集清洗和 seed 生成能力。
- 让已有 products / order_items demo 数据进入订单查询工具结果。
- 让 Return / Exchange specialist handler 基于 orderItems 生成商品明细级建议。

V3 不改变：

- LLM 只负责结构化规划；
- Java 后端负责校验和执行；
- ToolRegistry 是唯一工具执行入口；
- Agent / Handler 不直接访问 Repository；
- 高风险动作必须进入人工确认；
- 默认测试必须离线、确定性运行；
- 不执行真实退款、真实换货、真实优惠券补偿、真实支付或真实物流动作。

## 3. V3.1 MySQL Persistence

Status: completed for explicit MySQL profile and Spring JDBC persistence.

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

Status: completed for local app + mysql Docker Compose startup.

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

Status: completed for requestId propagation, MDC-backed structured log fields, and key-path observability.

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

### 5.7 完成记录

V3.3 已完成：

- 新增 `X-Request-Id` 请求头支持，请求未提供时自动生成，请求提供时原样透传；
- 响应头始终返回 `X-Request-Id`；
- 使用 MDC 保存请求级 `requestId`，并在请求结束后清理；
- 日志 pattern 增加 `requestId`、`ticketId`、`agentRunId`、`subtaskId`、`toolName` 和
  `approvalRequestId`；
- 在创建 Ticket、触发 AgentRun、执行 Specialist Handler、调用 ToolRegistry、创建 ApprovalRequest、
  approve / reject 和查询 execution tree 的关键路径增加结构化日志；
- 业务 ID 使用短生命周期 MDC scope，避免线程复用或后续步骤污染；
- 新增离线测试覆盖 requestId 生成、透传、MDC 清理和日志字段配置；
- 保持 ToolCallTrace、Approval、Execution Tree 和 persistence 作为业务审计与状态来源，日志只作为诊断入口。

V3.3 未做：

- 不接 Prometheus / Grafana；
- 不接 ELK、OpenTelemetry 或外部日志平台；
- 不记录 API Key、数据库密码、完整 LLM prompt、敏感凭证或过长原始文本；
- 不改变 Agent、ToolRegistry、Approval、Trace、Workspace 或 persistence 的业务语义。

## 6. V3.4 Final System Review

Status: completed for V3 infrastructure closure review.

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

### 6.7 完成记录

V3.4 已完成：

- README 收口为项目入口文档，覆盖项目简介、核心能力、技术栈、默认 in-memory 启动、MySQL profile、
  Docker Compose、Observability、核心 API、demo flow、execution tree、approval APIs、evaluation dataset、
  验证命令和已知限制；
- `docs/quality/QUALITY_SCORE.md` 增加 V3 final quality summary；
- `docs/exec-plans/active/EXEC_PLAN_V3_INFRASTRUCTURE_CLOSURE.md` 标记为 completed；
- 新增 `docs/exec-plans/completed/EXEC_PLAN_V3_FINAL_REVIEW.md`；
- 复跑默认质量门禁，确认默认测试仍不依赖 MySQL、Docker、Redis、真实 LLM、API Key 或外部网络。

V3.4 未做：

- 不新增 Java 业务功能；
- 不新增依赖；
- 不接新基础设施；
- 不改变 Agent、ToolRegistry、Approval、Trace、Workspace、Planner、Specialist Handler 或 persistence 语义；
- 不把 Docker Compose 写成生产部署方案；
- 不实现真实退款、真实换货、真实优惠券补偿、真实支付或真实物流。

## 7. V3.5 Demo Dataset Enrichment

Status: completed for optional public dataset cleaning, product/order-item schema enrichment, and generated demo seed.

### 7.1 目标

基于本地下载的公开订单、中文评论和女性服装反馈数据集，增强 MySQL demo 数据能力，同时保持默认启动和默认测试不依赖
外部 raw 数据。

### 7.2 范围

V3.5 覆盖：

- 新增 `products` 表；
- 新增 `order_items` 表；
- 在 `data-mysql.sql` 中保留现有 seed 并增加最小 product / order-item seed；
- 新增 `data/raw` 与 `data/generated` 目录说明；
- 更新 `.gitignore`，忽略 `data/raw` 下的原始大文件；
- 新增 `scripts/data/build_demo_seed.py`，使用 Python 标准库生成小规模 demo SQL 和可选 JSONL cases；
- 新增 `docs/data/DATASET_MAPPING.md`，记录三个数据集到项目字段的映射和清洗边界；
- 更新 README、质量目标和完成记录。

### 7.3 不做

V3.5 不做：

- 不提交原始大数据文件；
- 不接外部数据源 API 或爬虫；
- 不接生产数据库；
- 不把 `Age` 用于用户画像；
- 不让默认 `mvn test` 依赖 `data/raw`；
- 不改变 Agent、ToolRegistry、Approval、Trace、Workspace 或 order tools 的业务语义；V3.5 只准备数据基础，
  V3.6 再把 order item 明细接入工具输出；
- 不实现真实退款、真实换货、真实优惠券补偿、真实支付或真实物流。

### 7.4 验收标准

V3.5 完成时必须满足：

1. `schema-mysql.sql` 包含 `products` 和 `order_items`；
2. `data-mysql.sql` 或 `data/generated/demo_seed_extra.sql` 包含可演示的 product / order-item seed；
3. `scripts/data/build_demo_seed.py` 支持 bounded 参数和帮助说明；
4. `data/raw` 原始大文件被 Git 忽略；
5. `docs/data/DATASET_MAPPING.md` 说明三个数据集映射、暂不使用字段和清洗规则；
6. 默认 `mvn test` 不依赖 raw 文件；
7. 默认质量门禁继续通过。

### 7.5 测试要求

至少覆盖：

- schema 中 product / order-item 表结构；
- base seed 或 generated seed 中的 product / order-item 数据；
- build script 参数、UTF-8-SIG、XLSX 支持和 SQL 转义约束；
- raw 文件 ignore 规则；
- mapping 文档存在且覆盖三个数据集；
- ArchitectureTest 继续通过。

### 7.6 验证命令

```bash
python scripts/data/build_demo_seed.py --help
python scripts/data/build_demo_seed.py
mvn test
mvn checkstyle:check
mvn spotbugs:check
mvn test -Dtest=ArchitectureTest
```

### 7.7 完成记录

V3.5 已完成：

- `schema-mysql.sql` 新增 `products` 和 `order_items`；
- `data-mysql.sql` 新增最小 product / order-item seed，现有 orders 和 aftersale policies seed 保持不变；
- `.gitignore` 忽略 `data/raw` 下常见大文件格式，同时允许 README 和 `.gitkeep`；
- 新增 `data/raw/README.md`、`data/generated/README.md` 和 `docs/data/DATASET_MAPPING.md`；
- 新增 `scripts/data/build_demo_seed.py`，使用 Python 标准库读取 CSV / basic XLSX，生成
  `data/generated/demo_seed_extra.sql` 和 `data/generated/demo_evaluation_cases.jsonl`；
- 新增离线 harness 测试验证 schema、seed、脚本、ignore 和 mapping 文档；
- README 和 QUALITY_SCORE 记录 V3.5 数据质量边界。

V3.5 未做：

- 不新增 Java 业务 repository / service；
- V3.5 本身不改变 order tools 为多商品逻辑，后续由 V3.6 单独收口；
- 不把 optional generated evaluation cases 接入默认 Java evaluation dataset；
- 不提交 raw 数据集；
- 不让默认测试依赖 MySQL、Docker、真实 LLM、外部网络或 `data/raw`。

## 8. V3.6 Order Items Tool Enrichment

Status: completed for structured order-item output in order query tools.

### 8.1 目标

让 V3.5 引入的 `products` / `order_items` 数据进入现有订单查询工具结果，使 Agent 可以基于订单主表和商品明细生成
处理建议，同时保持默认 in-memory 测试路径离线运行。

### 8.2 范围

V3.6 覆盖：

- 新增纯 domain `OrderItem` 模型；
- 扩展 `Order` 返回 `orderItems`；
- MySQL `JdbcOrderRepository` 从 `order_items` join `products` 查询订单明细；
- in-memory order seed 为每个 demo order 提供最小 order item；
- `get_order_by_id` 工具输出结构化 `orderItems`；
- `AgentWorkspace` 的 `OrderFact` 记录商品明细摘要；
- ToolCallTrace / Execution Tree 通过现有 `outputJson` 可看到 `orderItems`；
- README、质量目标和完成记录同步更新。

### 8.3 不做

V3.6 不做：

- 不改 Agent 主流程；
- 不接真实订单中心、支付、物流、退款、库存或优惠券系统；
- 不让 Handler 或 Agent 直接访问 Repository；
- 不删除 in-memory repository；
- 不让默认测试依赖 MySQL、Docker、真实 LLM、API Key、外部网络或 `data/raw`；
- 不把 order tools 改成执行任何真实业务动作。

### 8.4 验收标准

V3.6 完成时必须满足：

1. `get_order_by_id` 返回 `orderItems`；
2. 每个 order item 至少包含 `orderItemId`、`productId`、`productName`、`category`、`quantity`、`unitPrice`、
   `itemStatus`、`supportReturn`、`supportExchange` 和 `isSpecialItem`；
3. MySQL schema harness 检查 `products` / `order_items` 表和 seed；
4. in-memory 默认测试能返回至少一个 order item；
5. ToolRegistry 执行 `get_order_by_id` 后 output data 包含 `orderItems`；
6. AgentRun trace 中 `get_order_by_id` 的 `outputJson` 包含 `orderItems`；
7. 默认 `mvn test` 不依赖 MySQL；
8. ArchitectureTest、Checkstyle、SpotBugs 继续通过。

### 8.5 测试要求

至少覆盖：

- order tool 直接调用的 `orderItems` 输出；
- special item 的 return / exchange support flag；
- AgentRun trace 中 tool output JSON 的 order item 字段；
- MySQL schema / seed harness 对 `products` 和 `order_items` 的检查；
- 默认 in-memory 流程回归；
- 架构边界回归。

### 8.6 验证命令

```bash
mvn test
mvn checkstyle:check
mvn spotbugs:check
mvn test -Dtest=ArchitectureTest
```

### 8.7 完成记录

V3.6 已完成：

- 新增 `OrderItem` 纯领域模型；
- `Order` 支持结构化 order item 列表，同时保留旧构造入口的 fallback item；
- `JdbcOrderRepository` 查询 `order_items` 和 `products`，并对旧数据提供主商品 fallback item；
- `InMemoryOrderRepository` seed 增加最小商品明细；
- `get_order_by_id` 输出包含结构化 `orderItems`；
- `get_user_orders` 复用共享 mapper，可保留结构化 item list；
- `OrderFact` 从工具结果提取 item summary；
- 测试覆盖 ToolRegistry output、AgentRun trace output 和 MySQL schema/seed harness。

V3.6 未做：

- 不新增真实外部订单中心；
- 不新增真实支付、物流、退款或优惠券动作；
- 不重构 Execution Tree 或 ToolCallTrace 数据结构；
- 不让默认测试依赖 MySQL、Docker、真实 LLM、API Key、外部网络或 raw datasets。

## 9. V3.7 Item-Specific Recommendation

Status: completed for deterministic item-level return and exchange recommendations.

### 9.1 目标

让 Return / Exchange Specialist Handler 使用 `get_order_by_id` 工具返回的 `orderItems`，在 final summary 和
Ticket note 中生成商品明细级售后建议，同时保持 Handler 不直接访问 Repository、不执行真实退款或换货。

### 9.2 范围

V3.7 覆盖：

- 扩展 AgentWorkspace 的订单事实，保存结构化 `OrderItemFact`；
- 从 `get_order_by_id` 工具输出解析 `orderItems`，作为 handler 内部建议依据；
- ReturnAgentHandler 生成 item-level return recommendation；
- ExchangeAgentHandler 生成 item-level exchange recommendation；
- 通过 productName、category 和简单服装关键词匹配相关商品；
- 匹配失败时 fallback 到订单第一个 item，并在 reason 中说明 fallback；
- 对 `supportReturn=false`、`supportExchange=false` 或 `isSpecialItem=true` 的 item，不建议直接退换，进入政策或人工边界；
- summary、subtask memory 和 Ticket note 能看到商品明细级建议；
- 测试覆盖正向、限制、特殊商品、fallback、Ticket note 和 trace 非退化。

### 9.3 不做

V3.7 不做：

- 不修改 MySQL `products` / `order_items` schema；
- 不要求数据库表包含 `support_return`、`support_exchange` 或 `is_special_item` 字段；
- 不接真实退款、真实换货、真实库存、真实物流、真实支付或真实订单中心；
- 不让 Handler 直接访问 OrderRepository 或任何业务 Repository；
- 不调用真实 LLM；
- 不重构 ToolCallTrace 或 Execution Tree 数据结构；
- 不让默认测试依赖 MySQL、Docker、真实 LLM、API Key、外部网络或 raw datasets。

### 9.4 验收标准

V3.7 完成时必须满足：

1. `get_order_by_id` 输出中的 `orderItems` 能进入 AgentWorkspace；
2. ReturnAgentHandler 能基于 `orderItems` 生成商品明细级退货建议；
3. ExchangeAgentHandler 能基于 `orderItems` 生成商品明细级换货建议；
4. `supportReturn=false` 时不建议直接退货；
5. `supportExchange=false` 时不建议直接换货；
6. `isSpecialItem=true` 时建议中体现特殊商品限制；
7. item 匹配失败时有清晰 fallback reason；
8. AgentRun final summary 和 Ticket note 包含商品明细级建议；
9. ToolCallTrace 继续记录 handler 内部工具调用；
10. ArchitectureTest、Checkstyle、SpotBugs 和默认测试继续通过。

### 9.5 测试要求

至少覆盖：

- Return handler item-level recommendation；
- Exchange handler item-level recommendation；
- unsupported return / exchange flags；
- special item restriction；
- fallback item selection；
- Ticket note 中的 item-level recommendation；
- AgentRun final summary 中的 item-level recommendation；
- ToolCallTrace 非退化；
- 默认 in-memory 流程回归；
- 架构边界回归。

### 9.6 验证命令

```bash
mvn test
mvn checkstyle:check
mvn spotbugs:check
mvn test -Dtest=ArchitectureTest
```

### 9.7 完成记录

V3.7 已完成：

- 新增 `OrderItemFact`，让 workspace 保存工具输出中的商品明细；
- `OrderFact` 从 `orderItems` 同时提取 item summary 和结构化 item facts；
- 新增确定性 `ItemRecommendationSupport`，按商品名、类目和简单服装关键词匹配 item；
- Return / Exchange handler 在成功 summary 与 Ticket note 中追加 item-level recommendation；
- 对 unsupported 或 special item 只给出政策 / 人工边界建议，不声称可直接退换；
- 保持 Handler 只通过 ToolRegistry 获取订单信息，不直接访问 Repository；
- 测试覆盖 item-level return / exchange、限制场景、fallback、Ticket note、final summary 和 trace 非退化。

V3.7 未做：

- 不新增数据库字段；`supportReturn`、`supportExchange` 和 `isSpecialItem` 仍由 Java demo 规则从现有商品字段派生；
- 不新增真实退款、真实换货、真实库存、真实支付、真实物流或外部订单中心集成；
- 不把 item recommendation 写成最终业务执行结果。

## 10. V3.8 Context Budget & Token Observability

Status: completed for deterministic prompt sectioning, budget enforcement, compact tool catalog, and token telemetry.

### 10.1 目标

在真实 LLM AgentRun 扩展前，先控制 LLM Planner 的输入上下文成本。V3.8 将 planner prompt 拆成明确 section，
对 critical section 做硬保护，对 optional section 做确定性裁剪，并在调用 LLM 前记录 token 估算 telemetry。

### 10.2 范围

V3.8 覆盖：

- `PromptSection` / `PromptSectionType` 表达 prompt 分层；
- `PromptBudget` 表达 system、history、rag、tool catalog、output 和 total input budget；
- `PromptBudgetPolicy` 表达 optional section 的处理顺序；
- `PromptBudgetApplier` 负责预算计算、optional section 裁剪和超限错误；
- `PromptUsageTelemetry` 记录 section token 估算、丢弃 token、总 input token、output budget 和 budget action；
- `CompactToolCatalogBuilder` 只输出工具名、风险等级、必要输入字段和简短用途；
- `AgentPlannerPromptFactory` 只负责组织 section 和拼装最终 prompt；
- `LlmAgentPlanner` 记录 token telemetry，但不记录完整 prompt；
- 默认配置增加 LLM budget 参数；
- 测试覆盖 token 估算、critical 保护、optional 裁剪、超限错误、compact catalog 和 sentinel phrase。

### 10.3 不做

V3.8 不做：

- 不调用真实 LLM；
- 不让默认测试依赖真实 LLM、MySQL、Docker、API Key、外部网络或 raw datasets；
- 不引入复杂 tokenizer 依赖；
- 不引入 Prometheus/Grafana、ELK、OpenTelemetry 或外部日志平台；
- 不改变 ToolRegistry、Approval、Trace、Workspace、Agent 执行语义；
- 不把完整 `TOOL_CONTRACTS.md`、完整 prompt、API Key、数据库密码或敏感凭证写入日志。

### 10.4 验收标准

V3.8 完成时必须满足：

1. Prompt section token 估算使用 `max(1, chars / 4)`；
2. Prompt budget 能计算 `totalInputTokens`；
3. Critical section 不会被静默丢弃或截断；
4. Optional section 按策略顺序裁剪或丢弃；
5. 超预算后仍无法满足时返回清晰错误；
6. Tool catalog 是压缩版本，只包含工具名、风险等级、必要输入字段和简短说明；
7. 长 optional document 的 sentinel phrase 不会完整进入最终 prompt；
8. `LlmAgentPlanner` 在请求前记录 token telemetry；
9. 默认 `mvn test` 不调用真实 LLM；
10. ArchitectureTest、Checkstyle、SpotBugs 和默认测试继续通过。

### 10.5 验证命令

```bash
mvn test
mvn checkstyle:check
mvn spotbugs:check
mvn test -Dtest=ArchitectureTest
```

### 10.6 完成记录

V3.8 已完成：

- 新增 prompt section、budget、policy、applier、telemetry、compact tool catalog 等独立协作者；
- `AgentPlannerPromptFactory` 输出 layered prompt，并保留旧 `systemPrompt()` / `userPrompt()` 兼容入口；
- `LlmAgentPlanner` 改为使用 budgeted prompt，并记录估算 token telemetry；
- `application.yml` 增加默认 LLM budget 配置；
- 测试覆盖预算计算、critical protection、optional reduction order、clear overflow error、compact catalog、
  sentinel phrase 和 prompt factory 职责拆分；
- 保持默认测试离线，不调用真实 LLM。

V3.8 未做：

- 不提供精确 provider tokenizer 计数；
- OpenAI provider usage 如果后续需要读取，应在 provider response 结构稳定后补充；
- 不实现真实 LLM + MySQL end-to-end AgentRun live validation。

## 11. V3.9 Real LLM + MySQL Seed Data Opt-In Validation

### 11.1 状态

completed

### 11.2 目标

新增一条显式 opt-in 的真实验证路径，用真实 LLM Planner 和 MySQL seed data 通过 HTTP API 跑完整 AgentRun
链路。默认 `mvn test` 不得调用真实 LLM、MySQL、Docker 或外部网络。

### 11.3 范围

V3.9 覆盖：

1. 新增 `RealAgentValidationLiveTest`；
2. 只有同时设置 `-Dlive.llm=true` 和 `-Dlive.mysql=true` 时才允许执行；
3. 缺少 `OPENAI_API_KEY`、`AFTERSALE_MYSQL_URL`、`AFTERSALE_MYSQL_USERNAME` 或
   `AFTERSALE_MYSQL_PASSWORD` 时跳过；
4. 使用 `mysql` profile 和 `agent.planner.mode=llm` 启动测试应用上下文；
5. 通过 HTTP `POST /api/tickets` 创建 Ticket；
6. 通过 HTTP `POST /api/tickets/{ticketId}/agent-runs` 触发 AgentRun；
7. 通过 HTTP `GET /api/agent-runs/{runId}/execution-tree` 查询执行树；
8. 通过 HTTP `GET /api/agent-runs/{runId}/traces` 验证 ToolCallTrace；
9. 验证 `get_order_by_id`、`search_aftersale_policy`、`add_ticket_note` 都通过 trace/tool call 出现；
10. 验证 `get_order_by_id` 输出包含 `orderItems`；
11. 验证 execution tree 或 final summary 包含商品明细级建议；
12. 增加 provider 403 / insufficient-balance 的清晰 live-run 错误提示；
13. 增加真实验证手册和完成记录。

### 11.4 不做什么

V3.9 不做：

- 不把真实 LLM / MySQL 验证加入默认测试路径；
- 不提交 API Key、数据库密码或个人路径；
- 不让 LLM 直接执行工具；
- 不绕过 ToolRegistry、AgentPlanValidator、Trace、Approval 或 Workspace 边界；
- 不实现真实退款、换货、支付、物流或库存动作；
- 不新增复杂外部评测框架；
- 不改变默认 rule-based / in-memory 离线测试路径。

### 11.5 验收标准

V3.9 完成时必须满足：

1. 默认 `mvn test` 中 `RealAgentValidationLiveTest` 被 skipped；
2. 缺少 `live.llm=true` 或 `live.mysql=true` 时 skipped；
3. 缺少必需环境变量时 skipped；
4. 手动命令文档完整；
5. HTTP 链路覆盖 Ticket、AgentRun、Execution Tree 和 Trace API；
6. 真实 LLM 只生成 AgentPlan，后端仍由 ToolRegistry 执行工具；
7. MySQL seed order 默认使用 `O202605130001`，并支持 `AFTERSALE_LIVE_ORDER_ID` 覆盖；
8. 默认测试不依赖 MySQL、Docker、真实 LLM、API Key 或外部网络；
9. ArchitectureTest、Checkstyle、SpotBugs 和默认测试继续通过。

### 11.6 验证命令

默认验证：

```bash
mvn test
mvn checkstyle:check
mvn spotbugs:check
mvn test -Dtest=ArchitectureTest
```

可选 live 验证：

```bash
mvn test -Dtest=RealAgentValidationLiveTest -Dlive.llm=true -Dlive.mysql=true
```

### 11.7 完成记录

V3.9 已完成：

- 新增 HTTP API 驱动的 opt-in live validation；
- 将 live test 限制在显式系统属性和必需环境变量之后；
- 验证真实 LLM AgentPlan、AgentPlanValidator、Specialist Handler、ToolRegistry、ToolCallTrace 和 Execution
  Tree 链路；
- 补齐 OpenAI strict JSON schema 的 `subtasks` 字段，使真实 LLM 输出契约与已有 parser / specialist handler
  能力一致；
- 增加 `docs/demo/REAL_AGENT_VALIDATION.md` 和 V3.9 完成记录。

## 12. V3.10 DashScope Qwen LLM Provider Adapter

### 12.1 状态

completed

### 12.2 目标

让 `LlmAgentPlanner` 在保留 OpenAI Responses provider 的同时，支持阿里云百炼 DashScope / Qwen 的
Responses-compatible 和 OpenAI-compatible Chat Completions 调用方式。默认测试仍然不调用真实 LLM、MySQL、
Docker 或外部网络。

### 12.3 范围

V3.10 覆盖：

1. 增加 provider 配置值 `openai-responses`、`dashscope-responses`、`dashscope-chat-compatible`；
2. 保留旧 `openai` 配置值到 `openai-responses` 的兼容映射；
3. 增加 DashScope API Key、base URL、responses endpoint、chat completions endpoint 配置；
4. 增加 provider-aware `LlmClientFactory`；
5. 复用 Responses client 处理 OpenAI Responses 和 DashScope Responses-compatible endpoint；
6. 新增 Chat Completions compatible client，将 system/user prompt 转换为 `messages`；
7. 将 `choices[0].message.content` 统一转换为 `LlmResponse` 文本；
8. provider error 摘要包含 provider、endpoint host、model、status code 和脱敏响应体；
9. 更新 live smoke / real validation 的 provider key 判断，使 DashScope 缺 key 时跳过；
10. 更新 README、real validation 手册、LLM contract、质量分和完成记录。

### 12.4 不做什么

V3.10 不做：

- 不把真实 DashScope / OpenAI 调用加入默认测试；
- 不提交任何 API Key、数据库密码或个人路径；
- 不绕过 `AgentPlanParser`、`AgentPlanValidator`、ToolRegistry、Approval、Trace 或 Workspace 边界；
- 不让 LLM 直接执行工具；
- 不实现真实退款、换货、支付、物流或库存动作；
- 不引入复杂 provider SDK 或外部监控平台。

### 12.5 验收标准

V3.10 完成时必须满足：

1. `openai-responses` 使用 Responses client；
2. `dashscope-responses` 使用 DashScope responses endpoint；
3. `dashscope-chat-compatible` 使用 Chat Completions compatible client；
4. Chat client 能生成 `messages` 请求并解析 `choices[0].message.content`；
5. provider error summary 不包含 API Key 或完整 prompt；
6. live validation 在缺少所选 provider API Key 时跳过或给出清晰提示；
7. 默认 `mvn test` 不调用真实 DashScope、OpenAI、MySQL 或 Docker；
8. ArchitectureTest、Checkstyle、SpotBugs 和默认测试继续通过。

### 12.6 验证命令

```bash
mvn test
mvn checkstyle:check
mvn spotbugs:check
mvn test -Dtest=ArchitectureTest
```

可选 DashScope live smoke：

```bash
mvn test -Dtest=LlmPlannerLiveSmokeTest -Dlive.llm=true
```

### 12.7 完成记录

V3.10 已完成：

- 新增 provider enum、provider settings、provider-aware client factory 和 provider error formatter；
- 新增 DashScope Chat Completions compatible client；
- 更新 `application.yml`、live smoke test 和 HTTP live validation 的 provider 配置路径；
- 增加 provider selection、chat request/response 和 error sanitization 单测；
- 文档补充 DashScope / Qwen PowerShell 示例和 endpoint/model mismatch 风险说明。

## 13. V3 当前状态

```text
V3.1 MySQL Persistence: completed
V3.2 Docker Compose: completed
V3.3 Structured Logging / Observability: completed
V3.4 Final System Review: completed
V3.5 Demo Dataset Enrichment: completed
V3.6 Order Items Tool Enrichment: completed
V3.7 Item-Specific Recommendation: completed
V3.8 Context Budget & Token Observability: completed
V3.9 Real LLM + MySQL Seed Data Opt-In Validation: completed
V3.10 DashScope Qwen LLM Provider Adapter: completed
```

V3.1 已完成显式 MySQL profile、Spring JDBC repository、schema/seed 初始化和默认 in-memory 回归保护。V3.2
已完成本地 app + mysql Docker Compose 启动路径。V3.3 已完成 requestId 追踪、MDC 日志字段和关键路径结构化日志。
V3.4 已完成最终系统复盘和文档收口。V3.5 已完成可选 demo dataset enrichment、products/order_items schema
与 seed 生成路径。V3.6 已完成 order-item-aware order tool output，使 demo 商品明细能进入 Agent 工具结果和
trace。V3.7 已完成 Return / Exchange handler 的商品明细级建议，使工具结果能进入最终建议和 Ticket note。
V3.8 已完成 LLM Planner context budget、compact tool catalog 和 token telemetry，为后续真实 LLM 演练提供
输入成本边界。V3.9 已完成真实 LLM + MySQL seed data 的显式 opt-in HTTP 验证路径，默认测试仍保持离线确定性。
V3.10 已完成 DashScope / Qwen provider adapter，使真实 LLM 验证可在 OpenAI Responses 和 DashScope
compatible endpoints 之间显式切换。V3 基础设施收口阶段完成。
