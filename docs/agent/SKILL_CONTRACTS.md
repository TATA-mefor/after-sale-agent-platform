
# Agent Skill Contracts

## 1. 目标

本文件定义 AfterSale-Agent V4 的 Skill 契约。V4 引入 Skill 是为了在 Tool 原子能力之上表达可复用的任务能力，并让 RAG、订单事实、政策证据、风险判断和工具调用形成可审计的执行链路。

核心原则：

```text
Tool = 原子可执行能力
Skill = 可复用复合任务能力
```

Skill 不是 LLM prompt，Skill 不是微服务，Skill 不是外部工具直接调用。Skill 是 Java 后端中的可测试、可审计、可约束的任务策略。

V4.1 实现状态：

- 已新增 `AgentSkill`、`SkillDefinition`、`SkillRegistry`、`SkillExecutionContext` 和
  `SkillExecutionResult`；
- 已通过 `SpecialistHandlerSkillAdapter` 将现有 Specialist Handler 暴露为 Skill-compatible 能力；
- 已注册 Return / Exchange / Coupon / Logistics / General / Human Escalation Skill definitions；
- 当前 AgentRun 主执行路径仍使用 `SpecialistAgentHandlerRegistry`，尚未迁移为 SkillRegistry 调度；
- `plannedSkills` 当前是后续兼容扩展，V4.1 不要求 Planner 默认生成，也不在运行时执行。

## 2. Tool 与 Skill 的区别

### 2.1 Tool

Tool 是最小可执行单元。

示例：

```text
get_order_by_id
get_user_orders
search_aftersale_policy
add_ticket_note
update_ticket_status
create_aftersale_ticket
```

Tool 必须：

- 由 ToolRegistry 注册；
- 声明 inputSchema / outputSchema；
- 声明 riskLevel；
- 声明 requiresApproval；
- 返回 SUCCEEDED / FAILED / REQUIRES_APPROVAL；
- 每次执行产生 ToolCallTrace。

### 2.2 Skill

Skill 是面向任务的复合能力。

示例：

```text
ReturnEligibilityAssessmentSkill
ExchangeRecommendationSkill
CouponConsultationSkill
LogisticsIssueAnalysisSkill
GeneralAfterSaleConsultationSkill
HumanApprovalRoutingSkill
RagPolicyEvidenceSkill
```

Skill 可以：

- 组合多个 Tool；
- 读取 AgentWorkspace；
- 写入 AgentWorkspace；
- 聚合订单事实和政策证据；
- 生成 SubtaskExecutionResult；
- 返回 approval requirement；
- 将结果暴露给 Execution Tree。

Skill 不得绕过 ToolRegistry 调用业务能力。

## 3. Skill 必须声明的契约

每个 Skill 必须声明：

```text
skillName
description
supportedSubtaskTypes
inputSchema
outputSchema
requiredTools
optionalTools
riskLevel
requiresApprovalWhen
evidenceRequirements
workspaceReads
workspaceWrites
failureModes
```

推荐 Java 接口形态：

```java
public interface AgentSkill {
    SkillDefinition definition();
    SkillExecutionResult execute(SkillExecutionContext context);
}
```

## 4. SkillDefinition

`SkillDefinition` 至少包含：

```text
skillName
shortDescription
supportedSubtaskTypes
requiredToolNames
optionalToolNames
riskLevel
evidenceRequirements
requiresApprovalWhen
```

SkillRegistry 使用 `skillName` 和 `supportedSubtaskTypes` 查找 Skill。

## 5. SkillExecutionContext

`SkillExecutionContext` 至少包含：

```text
runId
ticket
agentPlan
subtask
availableTools
workspace
riskPolicySummary
skillInput
previousSkillResults
```

SkillExecutionContext 不得包含：

- API Key；
- database password；
- provider secret；
- full raw prompt；
- long raw LLM output；
- sensitive credentials。

## 6. SkillExecutionResult

`SkillExecutionResult` 至少包含：

```text
skillName
subtaskId
status
summary
evidence
toolCalls
riskFlags
requiresApproval
approvalReason
errorCode
errorMessage
workspaceWrites
```

Status 建议：

```text
SUCCEEDED
FAILED
WAITING_APPROVAL
SKIPPED
```

## 7. Skill 执行规则

Skill 必须遵守：

1. 必须通过 ToolRegistry 调用 Tool；
2. 不得直接访问 Repository；
3. 不得直接访问 VectorStore；
4. 不得直接访问 JdbcTemplate / DataSource；
5. 不得直接调用 Spring AI ChatClient；
6. 不得直接调用 Spring AI EmbeddingModel；
7. 不得绕过 RiskPolicy；
8. 不得绕过 Approval；
9. 不得隐藏 ToolCallTrace；
10. 不得声称真实退款、真实换货、优惠券补偿、支付变更、物流变更或争议关闭已经完成；
11. 不得把 RAG evidence 当成最终业务结论；
12. 不得在默认测试中依赖真实 LLM、API Key、PostgreSQL、PGvector、Docker、MySQL、Redis 或外部网络。

## 8. Skill 与 RAG

RAG 检索在 Skill 中只能作为 evidence acquisition。

允许：

```text
Skill
→ ToolRegistry
→ search_aftersale_policy
→ PolicyHybridSearchService
→ PolicySearchResult
→ Workspace.PolicyEvidence
```

禁止：

```text
Skill → VectorStore
Skill → EmbeddingModel
Skill → PGvector repository
Skill → raw SQL vector search
```

Skill 可以基于 RAG evidence 生成建议，但必须保留证据来源，例如：

```text
chunkId
documentId
documentTitle
score
retrievalMode
snippet
```

## 9. Skill 风险聚合

Skill 风险等级由以下因素决定：

```text
max(subtask riskLevel, requiredTools riskLevel, intended business outcome riskLevel)
```

示例：

- 只读订单查询 + 政策检索：LOW；
- 添加内部工单备注：LOW；
- 受控工单状态变更：MEDIUM；
- 涉及退款、补偿、支付、物流、库存、争议关闭：HIGH，必须进入 Approval。

## 10. SkillRegistry 规则

SkillRegistry 必须：

- 按 skillName 查找唯一 Skill；
- 按 SubtaskType 查找可用 Skill；
- 拒绝重复 skillName；
- 拒绝重复且冲突的默认 SubtaskType handler；
- 暴露只读 SkillDefinition 列表给 Planner；
- 不暴露 Spring bean internals 给 LLM。

V4.1 的 `SkillRegistry` 允许同一 `SubtaskType` 返回多个候选 Skill 列表；当前内置 Specialist Skill 每个
SubtaskType 只有一个默认候选。后续如果需要 primary skill 选择策略，必须在 SkillRegistry 或规划校验层显式表达，不得隐式随机选择。

V4.2 adds Spring AI adapters only behind infrastructure boundaries. Skill implementations still must not inject
`ChatClient`, `ChatModel`, `EmbeddingModel`, `LlmClient`, or concrete Spring AI adapters directly. A future RAG evidence
Skill must acquire evidence through ToolRegistry and policy tools, not by calling Spring AI or VectorStore directly.

## 11. Planner 与 Skill

Planner 可以输出：

```text
plannedSkills
plannedTools
```

Planner 不得执行 Skill 或 Tool。

Java 后端必须校验：

- plannedSkills 均来自 SkillRegistry；
- plannedTools 均来自 ToolRegistry；
- plannedSkills 与 SubtaskType 兼容；
- HIGH-risk skill 进入 Approval 边界；
- Skill 和 Tool 不产生非法依赖或循环执行。

V4.1 边界：`plannedSkills` 尚未接入 `AgentPlanParser` / `AgentPlanValidator` / `AgentApplicationService`。当前
LLM 和 RuleBased planner 仍以 `plannedTools` 和 `subtasks` 为运行时契约。启用 `plannedSkills` 需要后续阶段以
backward-compatible 方式补充解析、校验和运行时策略。

## 12. Execution Tree 展示

V4 Execution Tree 应支持：

```text
AgentRun
├── Subtask
│   ├── Skill
│   │   ├── ToolCall
│   │   └── ToolCall
│   ├── PolicyEvidence
│   └── ApprovalRequest optional
```

ToolCallTrace 仍记录实际 Tool 调用。SkillExecutionResult 记录 Skill 层结果。

## 13. Default Test Boundary

默认验证命令：

```bash
mvn test
mvn checkstyle:check
mvn spotbugs:check
mvn test -Dtest=ArchitectureTest
```

默认验证不得依赖：

- real LLM；
- API Key；
- PostgreSQL；
- PGvector；
- Docker；
- MySQL；
- Redis；
- external network。

Live Skill / RAG tests 必须显式 opt-in。
