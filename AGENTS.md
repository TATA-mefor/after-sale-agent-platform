# AGENTS.md v0.1

# AfterSale-Agent 智能体入口导航

本文件是智能体进入仓库后的第一入口。

它不是百科全书，只负责指路。任何智能体在修改本仓库前，必须先阅读本文件，并按任务类型继续阅读对应文档。

## 1. 项目一句话

AfterSale-Agent 是一个基于 Java Spring Boot 的企业级电商售后工单 Agent 平台。

项目目标：

> 用电商售后作为业务外壳，用企业工单作为系统骨架，用 Harness Engineering 作为工程底座，构建一个可追踪、可审计、可验证的 Agent 执行系统。

## 2. 工作原则

本仓库遵循以下原则：

1. 先约束，后代码；
2. 仓库是唯一可信上下文；
3. 文档、测试、lint、架构检查和代码同等重要；
4. Agent 不直接执行高风险业务动作；
5. 每个非平凡任务必须先写执行计划；
6. 任务完成必须通过机械化验证；
7. 文档变更和代码变更必须保持一致。

## 3. 必读文档顺序

### 3.1 所有任务都必须先读

```text
SPEC.md
WORKFLOW.md
AGENTS.md
ARCHITECTURE.md
```

### 3.2 涉及业务需求时继续读

```text
docs/product/
docs/decisions/
docs/exec-plans/active/
```

### 3.3 涉及 Agent 行为时继续读

```text
docs/agent/TOOL_CONTRACTS.md
docs/agent/RISK_POLICY.md
docs/agent/EVALUATION.md
docs/agent/PROMPT_GUIDE.md
```

### 3.4 涉及质量、测试、架构约束时继续读

```text
docs/quality/QUALITY_SCORE.md
docs/quality/TESTING.md
docs/quality/ARCHITECTURE_RULES.md
```

## 4. 任务执行流程

任何非平凡任务必须遵循：

```text
Read Context
→ Write / Update Execution Plan
→ Implement Small Change
→ Run Back-Pressure Checks
→ Update Docs and Decision Logs
→ Produce Review Packet
→ TASK_COMPLETE
```

不得跳过执行计划直接写复杂代码。

## 5. 允许智能体做什么

智能体可以：

* 新增或修改文档；
* 新增 Java 领域模型；
* 新增应用服务；
* 新增 Controller；
* 新增 Agent 工具；
* 新增测试；
* 新增 ArchUnit 规则；
* 修复测试、lint、架构检查失败；
* 更新执行计划和决策日志。

## 6. 智能体禁止做什么

智能体不得：

* 绕过 SPEC 修改项目目标；
* 绕过 WORKFLOW 直接完成复杂任务；
* 删除测试来让构建通过；
* 降低架构约束来适配坏代码；
* 在 Controller 中写业务逻辑；
* 让 API 层直接访问 Repository；
* 让 Agent 直接修改订单核心数据；
* 让 Agent 直接执行真实退款、补偿、删除订单等高风险动作；
* 隐藏工具调用失败；
* 生成没有 trace 的 Agent 执行结果；
* 引入大型依赖但不写决策日志。

## 7. 推荐开发顺序

V1 开发按以下顺序推进：

```text
1. Harness 文档骨架
2. Spring Boot 项目初始化
3. 基础包结构
4. 架构约束测试
5. 核心领域模型
6. 工单创建 API
7. 模拟订单查询工具
8. 售后政策检索工具
9. Agent 编排服务
10. AgentRun / ToolCallTrace 记录
11. V1 演示接口
12. 测试、质量评分、Review Packet
```

## 8. 最低验证命令

当项目骨架建立后，所有代码任务至少运行：

```bash
mvn test
mvn checkstyle:check
mvn spotbugs:check
mvn test -Dtest=ArchitectureTest
```

如果某个验证命令尚未接入，必须在执行计划中说明原因，并创建后续任务。

## 9. Java 编码边界

默认采用模块化单体。

推荐包结构：

```text
io.github.tatame.aftersale
├── common
├── order
├── ticket
├── policy
├── agent
├── tool
├── trace
└── approval
```

每个业务域内部优先遵循：

```text
api → application → domain → infrastructure
```

依赖只能从外层指向内层，不能反向依赖。

## 10. Agent 行为边界

Agent 可以执行：

* 售后意图识别；
* 订单查询；
* 售后政策检索；
* 工单创建；
* 添加工单备注；
* 低风险状态更新；
* 处理建议生成。

Agent 不得直接执行：

* 真实退款；
* 真实补偿；
* 删除订单；
* 修改支付状态；
* 绕过人工审核关闭高风险工单。

高风险动作必须进入人工确认流程。

## 11. Review Packet 模板

任务完成后必须输出：

```text
## Review Packet

### What Changed

### Files Changed

### Why This Design

### Validation Run

### Risks

### Follow-ups

### Completion Signal
TASK_COMPLETE
```

没有验证结果，不得输出 TASK_COMPLETE。

## 12. 文档维护规则

如果修改了以下内容，必须同步更新文档：

* 项目范围；
* API 行为；
* 数据模型；
* Agent 工具契约；
* 风险策略；
* 架构依赖；
* 验证命令；
* 任务执行流程。

## 13. 项目成功标准

本项目成功不是代码量大，而是：

* 业务场景明确；
* Java 后端结构清晰；
* Agent 能规划、检索、调工具、留 trace；
* 高风险动作有边界；
* 架构规则可机械化验证；
* 文档能指导后续智能体继续开发；
* 项目可本地启动、测试、演示。

## 14. V1 当前收口说明

当前 V1 已收口为 API-only 后端 Demo：创建工单、触发规则式 AgentRun、检索售后政策、写入工单备注、
记录 ToolCallTrace 并查询执行轨迹。V1 不接入真实 LLM、真实数据库、真实退款、真实物流、复杂前端或多
Agent。订单查询工具未进入最终 V1，作为 V2 候选方向保留。

## 15. V2 LLM / Planner 任务规则

涉及 LLM、Planner、模型调用、结构化输出的任务，必须额外阅读：

```text
docs/agent/LLM_PLANNER_CONTRACT.md
docs/decisions/DECISION_LLM_PLANNER_ADAPTER.md
EXEC_PLAN_V2.md
```

V2 任务必须遵守：

1. 禁止把真实 API Key 写入代码、测试、README 或提交历史；
2. 禁止让测试依赖真实 LLM、API Key 或外部网络；
3. 禁止让 LLM 绕过 ToolRegistry；
4. 禁止让 LLM 直接修改 Ticket、Order、AgentRun 或 ToolCallTrace；
5. 禁止让 LLM 直接执行退款、补偿、关闭争议工单等高风险动作；
6. 默认测试必须可离线运行；
7. LLM 只能生成结构化 AgentPlan；
8. Java 后端必须校验 AgentPlan，并通过 ToolRegistry 执行工具；
9. RuleBasedAgentPlanner 必须保留；
10. FakeAgentPlanner 必须用于确定性测试。

如果修改了 Planner 行为，必须同步更新：

```text
docs/agent/LLM_PLANNER_CONTRACT.md
docs/agent/TOOL_CONTRACTS.md
docs/agent/RISK_POLICY.md
```

## 16. V2.4 Specialist Handler 任务规则

涉及 Specialist Handler、子任务专业处理器、handler registry、SubtaskExecutionContext 或 SubtaskExecutionResult 的任务，
必须额外阅读：

```text
docs/decisions/DECISION_SPECIALIST_AGENT_HANDLER.md
EXEC_PLAN_V2.md
ARCHITECTURE.md
docs/agent/TOOL_CONTRACTS.md
docs/agent/RISK_POLICY.md
```

V2.4 任务必须遵守：

1. Handler 不得直接访问 Repository；
2. Handler 不得绕过 ToolRegistry；
3. Handler 不得绕过 RiskPolicy；
4. Handler 不得直接调用 LLM；
5. Handler 不得直接修改 Ticket、Order、AgentRun 或 ToolCallTrace；
6. Handler 不得执行真实退款、真实换货、真实优惠券补偿、支付变更、物流变更或争议关闭；
7. AgentApplicationService 负责调度 Handler；
8. LLM / Planner 只负责生成结构化 subtasks，不直接执行 Handler；
9. ToolCallTrace 必须继续记录 Handler 内部工具调用；
10. 默认测试必须可离线运行。

## 17. V2.6 Agent Workspace / Structured Memory 任务规则

涉及 AgentWorkspace、Structured Memory、OrderFact、PolicyEvidence、SubtaskMemory、ToolResultSummary 或 RiskFlag 的任务，
必须额外阅读：

```text
docs/decisions/DECISION_AGENT_WORKSPACE_MEMORY.md
EXEC_PLAN_V2.md
ARCHITECTURE.md
docs/agent/TOOL_CONTRACTS.md
docs/agent/RISK_POLICY.md
```

V2.6 任务必须遵守：

1. Workspace 只表示单次 AgentRun 内部的结构化工作记忆；
2. Workspace 不得替代 ToolCallTrace；
3. Workspace 不得绕过 ToolRegistry；
4. Workspace 不得直接访问 Repository；
5. Workspace 不得保存 API Key、敏感凭证、完整长 prompt 或 LLM 原始长文本；
6. Workspace 不得实现长期用户画像；
7. Workspace 不得实现跨会话记忆；
8. Workspace 不得实现向量记忆；
9. 默认测试不得依赖真实 LLM、API Key、Redis、MySQL、向量库或外部网络；
10. final summary 可以基于 workspace 汇总，但工具调用审计仍必须来自 ToolCallTrace。

## 18. V3 Infrastructure Closure 任务规则

涉及 persistence、MySQL、schema、seed data、Docker Compose、structured logging、actuator health 或基础设施
profile 的任务，必须额外阅读：

```text
EXEC_PLAN_V3.md
docs/decisions/DECISION_V3_INFRASTRUCTURE_CLOSURE.md
ARCHITECTURE.md
WORKFLOW.md
```

V3 任务必须遵守：

1. 不得删除 in-memory / test profile；
2. 不得让默认 `mvn test` 强依赖本地 MySQL、Docker、Redis、真实 LLM 或外部网络；
3. 不得提交真实数据库密码、API Key、访问令牌或其他敏感凭证；
4. 不得把 Docker Compose 当作生产部署方案；
5. 不得绕过现有 ToolRegistry / Agent / Approval / Trace / Workspace 边界；
6. 不得让 Controller 直接访问 Repository；
7. 不得让 Agent 或 Specialist Handler 直接访问 Repository；
8. 不得让 persistence 绕过 ApplicationService 执行业务状态流转；
9. 数据库变更必须有 schema 初始化或 migration 策略；
10. Docker Compose 变更必须同步更新 README。

## V4 RAG / Spring AI / Tool / Skill 任务规则

V4 任务聚焦 RAG、Spring AI、PGvector / VectorStore、Tool / Skill 能力层和 Spring Boot 完整性。任何涉及以下关键词的任务，都必须先阅读本节列出的文档：

```text
Spring AI
RAG
VectorStore
PGvector
Embedding
Policy Ingestion
ToolRegistry
SkillRegistry
AgentSkill
plannedSkills
search_aftersale_policy
Hybrid Retrieval
```

### V4 必读文档

```text
EXEC_PLAN_V4.md
docs/exec-plans/active/EXEC_PLAN_V4_RAG_SPRING_AI.md
docs/decisions/DECISION_V4_TOOL_SKILL_LAYER.md
docs/decisions/DECISION_V4_SPRING_AI_ADAPTER.md
docs/decisions/DECISION_V4_RAG_VECTOR_STORE.md
docs/decisions/DECISION_V4_SPRING_BOOT_COMPLETENESS.md
docs/agent/TOOL_CONTRACTS.md
docs/agent/SKILL_CONTRACTS.md
docs/agent/RAG_POLICY_RETRIEVAL_CONTRACT.md
docs/agent/RISK_POLICY.md
docs/agent/LLM_PLANNER_CONTRACT.md
docs/quality/RAG_EVALUATION.md
```

### V4 不得违反的边界

1. Tool 是原子执行能力，必须通过 ToolRegistry 调用；
2. Skill 是复合任务能力，必须通过 SkillRegistry 调度；
3. Planner 可以规划 Tool / Skill，但不得执行 Tool / Skill；
4. Skill 可以组合多个 Tool，但不得绕过 ToolRegistry；
5. Skill 不得直接访问 Repository、VectorStore、JdbcTemplate、Spring AI ChatClient 或 EmbeddingModel；
6. RAG 检索只能作为政策证据，不能作为最终业务动作；
7. `search_aftersale_policy` 仍然是 LOW-risk read-only tool；
8. 每个实际 Tool 调用必须产生 ToolCallTrace；
9. Skill 执行结果必须写入 AgentWorkspace、SubtaskExecutionResult 或 Execution Tree 可读结构；
10. 默认测试不得依赖真实 LLM、API Key、PostgreSQL、PGvector、Docker、MySQL、Redis 或外部网络；
11. live RAG / embedding / provider tests 必须显式 opt-in；
12. 不得执行真实退款、换货、补偿、支付、物流、库存或争议关闭。

### V4 Codex 执行顺序

```text
Read V4 Harness Context
→ Update / confirm execution plan
→ Implement smallest boundary-safe change
→ Add deterministic tests
→ Run default verification commands
→ Update docs and Review Packet
→ TASK_COMPLETE
```
