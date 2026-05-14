# WORKFLOW.md v0.1

# AfterSale-Agent 工作流规范

## 1. 工作流目标

本文件定义 AfterSale-Agent 项目的协作方式。

项目遵循 Harness Engineering 原则：

> 先约束，后代码；先定义目标、边界、反馈和验收，再进入实现。

本项目中的任何代码变更，都不应该是孤立的“直接开写”。每个任务都必须经过：

```text
意图定义 → 上下文读取 → 执行计划 → 小步实现 → 机械化验证 → 记录结果 → 人类审查
```

## 2. 角色分工

### 2.1 Human Steering

人类负责：

* 定义业务目标；
* 判断项目方向；
* 审查高风险设计；
* 决定技术取舍；
* 合并或拒绝变更；
* 将经验沉淀为规则。

人类不应该直接跳过规范去写大量业务代码。

### 2.2 Agent Execution

智能体负责：

* 阅读仓库上下文；
* 生成执行计划；
* 按任务修改文档或代码；
* 补充测试；
* 运行本地验证命令；
* 解释失败原因；
* 根据反馈修复问题；
* 输出变更摘要。

智能体不得绕过约束直接完成高风险动作。

## 3. 标准任务生命周期

### Step 1：Create Task

每个任务必须有明确目标。

任务描述至少包含：

* 背景；
* 目标；
* 不做什么；
* 影响范围；
* 验收标准。

推荐位置：

```text
docs/exec-plans/active/{task-name}.md
```

### Step 2：Read Context

开始任务前，必须读取相关上下文。

最低要求：

```text
SPEC.md
WORKFLOW.md
AGENTS.md
ARCHITECTURE.md
相关 docs/decisions/*
相关模块 README 或 package 文档
```

如果任务涉及 Agent 行为，还必须读取：

```text
docs/agent/TOOL_CONTRACTS.md
docs/agent/RISK_POLICY.md
docs/agent/EVALUATION.md
```

### Step 3：Write Execution Plan

任何非平凡任务都必须先写执行计划。

执行计划必须包含：

* 任务目标；
* 影响模块；
* 预期变更文件；
* 风险点；
* 验证命令；
* 回滚方案；
* 完成信号。

禁止在没有执行计划的情况下直接实现复杂功能。

### Step 4：Implement in Small Steps

实现必须小步推进。

优先顺序：

```text
测试或契约 → 领域模型 → 应用服务 → 基础设施 → API → Agent 工具 → Trace → 文档更新
```

禁止一次性生成大量无法验证的代码。

### Step 5：Run Back-Pressure Checks

每次变更后必须运行机械化验证。

V1 最低验证命令：

```bash
mvn test
mvn checkstyle:check
mvn spotbugs:check
mvn test -Dtest=ArchitectureTest
```

如果当前阶段尚未接入某个工具，需要在执行计划中说明。

### Step 6：Update Records

任务完成后必须更新记录。

包括：

* 执行计划状态；
* 决策日志；
* 已知问题；
* 后续任务；
* 质量评分变化。

推荐位置：

```text
docs/exec-plans/completed/
docs/decisions/
docs/quality/QUALITY_SCORE.md
```

### Step 7：Review Packet

每个任务完成后必须生成 Review Packet。

Review Packet 包含：

```text
1. 本次任务做了什么
2. 改动了哪些文件
3. 为什么这样设计
4. 运行了哪些验证
5. 哪些风险仍然存在
6. 后续建议
```

## 4. 完成信号

任务完成不能靠主观判断。

必须满足：

* 执行计划中的验收标准已完成；
* 核心测试通过；
* 架构约束通过；
* 文档已更新；
* 没有未解释的失败；
* Review Packet 已生成。

完成信号：

```text
TASK_COMPLETE
```

禁止在验证失败时输出完成信号。

## 5. 失败处理流程

如果测试、lint、架构检查失败，必须按以下顺序处理：

```text
读取失败信息
定位失败类型
判断是代码问题、测试问题、配置问题还是规范问题
优先修复违反约束的代码
必要时更新文档或执行计划
重新运行验证
记录失败原因和修复方式
```

禁止：

* 删除测试来绕过失败；
* 降低架构约束来让代码通过；
* 忽略失败继续推进；
* 用“模型可能会错”作为不修复理由。

## 6. 分支与任务粒度

每个任务应该足够小。

推荐任务粒度：

* 一个领域对象；
* 一个 API；
* 一个 Agent 工具；
* 一个状态流转；
* 一组测试；
* 一个架构约束。

不推荐：

* 一次实现完整售后系统；
* 一次实现完整 Agent；
* 一次接入所有中间件；
* 一次生成大批无测试代码。

## 7. V1 开发顺序

V1 按以下顺序推进：

```text
1. 完成 Harness 文档骨架
2. 初始化 Spring Boot 项目
3. 建立基础包结构
4. 加入 ArchUnit 分层约束
5. 定义核心领域模型：Order / Ticket / AgentRun / ToolCallTrace
6. 实现工单创建 API
7. 实现模拟订单查询工具
8. 实现售后政策检索工具
9. 实现 Agent 执行编排
10. 实现 Trace 记录
11. 实现 V1 演示接口
12. 补充测试和质量评分
```

## 8. Agent 行为边界

Agent 可以：

* 读取用户问题；
* 查询订单；
* 检索政策；
* 生成处理计划；
* 创建售后工单；
* 添加工单备注；
* 更新低风险工单状态；
* 生成建议。

Agent 不可以：

* 真实退款；
* 发放补偿；
* 删除订单；
* 修改支付记录；
* 绕过审批完成高风险工单；
* 隐藏工具调用失败；
* 生成没有依据的最终结论。

## 9. 人工确认规则

以下动作必须人工确认：

* 退款金额大于 0；
* 换货涉及库存变更；
* 补偿优惠券；
* 关闭争议工单；
* 覆盖系统自动判断；
* 用户历史售后异常但仍建议通过。

V1 可以只实现人工确认的数据结构和状态，不需要完整后台 UI。

## 10. 文档更新规则

代码变更如果改变以下内容，必须同步更新文档：

* API 行为；
* 模块边界；
* Agent 工具契约；
* 风险策略；
* 状态机；
* 数据模型；
* 验证命令；
* 项目范围。

文档不是附属品，是 Agent 的工作上下文。

## 11. 决策日志规则

以下情况必须写入 docs/decisions：

* 技术栈选择；
* 架构边界变化；
* Agent 行为边界变化；
* 是否引入中间件；
* 是否采用 Spring AI / LangChain4j；
* 是否真实接入外部系统；
* 安全策略变化。

决策日志格式：

```text
# Decision: {title}

Date: YYYY-MM-DD
Status: Proposed / Accepted / Rejected / Superseded

## Context

## Decision

## Consequences

## Alternatives Considered
```

## 12. 质量门禁

V1 最低质量门禁：

* 所有测试通过；
* 核心业务有单元测试；
* 至少 3 条 ArchUnit 规则；
* Agent 工具调用必须有 trace；
* Controller 不写业务逻辑；
* Service 不直接拼接 Prompt；
* 高风险动作必须进入人工确认；
* 所有执行计划必须有验收标准。

## 13. Review 标准

审查一个任务时，重点看：

```text
是否符合 SPEC
是否遵守 ARCHITECTURE
是否有测试
是否有 trace
是否有风险边界
是否更新文档
是否能被本地启动和演示
是否让 Agent 更容易继续工作
```

## 14. 项目哲学

本项目的核心不是证明“AI 能写代码”。

本项目要证明：

> 在明确上下文、机械化约束和反馈回路下，Agent 可以参与企业级 Java 后端系统的构建，并在业务边界内完成可追踪、可审计、可回滚的智能执行。

## 15. V2 外部模型接入规则

任何接入外部 LLM、Embedding、向量库、外部 Agent SDK 的任务，必须先满足：

1. 已有决策日志；
2. 已有契约文档；
3. 已有本地降级路径；
4. 已有 fake 或 rule 测试实现；
5. 默认测试不依赖外部网络；
6. API Key 不进入仓库；
7. 失败行为可解释；
8. 不破坏 V1 demo。

涉及 LLM Planner 的任务必须阅读：

```text
docs/decisions/DECISION_LLM_PLANNER_ADAPTER.md
docs/agent/LLM_PLANNER_CONTRACT.md
EXEC_PLAN_V2.md
```

### 15.1 LLM 任务完成标准

LLM 相关任务只有在以下条件满足时才可完成：

- `mvn test` 通过；
- `mvn checkstyle:check` 通过；
- `mvn spotbugs:check` 通过；
- `mvn test -Dtest=ArchitectureTest` 通过；
- 无真实 API Key 被提交；
- 默认测试可离线运行；
- RuleBased 或 Fake Planner 可用；
- Review Packet 说明 LLM 边界、失败处理和测试策略。

### 15.2 禁止行为

- 为了接入 LLM 删除 V1 测试；
- 让真实 LLM 成为默认测试依赖；
- 在文档中声称未完成能力已经完成；
- 让 LLM 绕过 ToolRegistry；
- 让 LLM 直接执行高风险业务动作。
