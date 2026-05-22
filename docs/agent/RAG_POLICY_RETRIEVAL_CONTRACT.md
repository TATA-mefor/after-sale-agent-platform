
# RAG Policy Retrieval Contract

## 1. 目标

本文件定义 V4 `search_aftersale_policy` RAG 政策检索契约。V4 将现有 deterministic keyword policy retrieval 升级为支持 KEYWORD / VECTOR / HYBRID 的结构化政策证据检索能力。

核心原则：

```text
RAG retrieves evidence.
RAG does not execute business actions.
```

RAG 只能提供政策证据，不能替代 AgentPlanValidator、RiskPolicy、Approval、ToolRegistry、AgentWorkspace 或业务服务。

## 2. 检索边界

允许链路：

```text
AgentSkill / Specialist Handler
→ ToolRegistry
→ search_aftersale_policy
→ PolicyApplicationService
→ PolicyHybridSearchService
→ KeywordRepository + VectorRepository
→ PolicySearchResult
→ ToolCallTrace
→ AgentWorkspace.PolicyEvidence
→ Execution Tree
```

禁止链路：

```text
AgentSkill → VectorStore
SpecialistAgentHandler → VectorStore
AgentApplicationService → PGvector repository
LLM Planner → VectorStore
ToolExecutor → external LLM direct business action
```

## 3. Tool Definition

```text
toolName: search_aftersale_policy
riskLevel: LOW
requiresApproval: false
type: read-only evidence retrieval
```

该工具不得修改 Ticket、Order、Payment、Inventory、Logistics、Coupon 或 ApprovalRequest 状态。

## 4. Input Schema

```json
{
  "query": "质量问题 退货 退款",
  "categories": ["RETURN", "REFUND"],
  "productType": "electronics",
  "retrievalMode": "HYBRID",
  "topK": 5,
  "minScore": 0.65,
  "subtaskId": "S1"
}
```

字段说明：

```text
query: required, non-blank
categories: optional, narrows policy category
productType: optional, narrows product type
retrievalMode: KEYWORD | VECTOR | HYBRID, default HYBRID when vector store is enabled
topK: bounded integer, default 5, maximum configured by policy
minScore: optional threshold for vector/hybrid results
subtaskId: optional trace attribution field
```

## 5. Output Schema

```json
{
  "results": [
    {
      "chunkId": "chunk-001",
      "documentId": "policy-001",
      "documentTitle": "售后退货退款政策",
      "category": "RETURN",
      "productType": "electronics",
      "snippet": "质量问题在签收后七天内可申请退货退款...",
      "score": 0.82,
      "retrievalMode": "HYBRID",
      "effectiveFrom": "2026-01-01",
      "effectiveTo": null,
      "metadata": {
        "chunkIndex": 3,
        "sourceType": "markdown"
      }
    }
  ],
  "message": "Found 1 policy evidence chunk.",
  "fallbackUsed": false
}
```

## 6. Retrieval Modes

### KEYWORD

- 使用 deterministic keyword matching；
- 默认测试必须覆盖；
- 不依赖 embedding provider 或 vector store；
- 可作为 VECTOR 不可用时的 fallback。

### VECTOR

- 使用 embedding 相似度搜索；
- 仅在 vector store profile / fake vector repository 可用时启用；
- 默认测试使用 fake embedding / fake vector repository；
- live vector test 必须显式 opt-in。

### HYBRID

- 合并 keyword 和 vector results；
- 去重规则以 chunkId / documentId + chunkIndex 为准；
- score 必须可解释；
- fallbackUsed 必须标明是否退回 keyword-only。

## 7. Evidence Rules

每条 evidence 必须包含：

```text
chunkId
documentId
documentTitle
category
snippet
score or deterministic ranking reason
retrievalMode
effectiveFrom/effectiveTo when available
```

不得返回：

- API Key；
- database password；
- full raw private document；
- full prompt；
- sensitive credentials；
- unbounded long raw text。

## 8. Unsupported Query Behavior

Unsupported query 必须返回结构化空结果：

```json
{
  "results": [],
  "message": "No supported after-sale policy evidence found for query.",
  "fallbackUsed": false
}
```

不得编造政策依据。

## 9. AgentWorkspace 写入规则

RAG result 进入 workspace 时，应写入：

```text
PolicyEvidence
- chunkId
- documentId
- documentTitle
- category
- snippet
- score
- retrievalMode
- subtaskId
- toolCallTraceId optional
```

Workspace 不替代 ToolCallTrace。ToolCallTrace 仍是实际 tool audit record。

## 10. Execution Tree 展示规则

Execution Tree 应展示：

```text
PolicyEvidenceNode
- chunkId
- documentId
- documentTitle
- score
- retrievalMode
- snippet
- attachedSubtaskId
- attachedSkillName optional
```

## 11. Ingestion Contract

Policy ingestion 必须可追踪：

```text
PolicyIngestionRun
- runId
- sourceType
- status
- documentCount
- chunkCount
- embeddedCount
- failedCount
- errorMessage
- startedAt
- finishedAt
```

同一 document checksum 重复导入时不得重复生成 chunk 和 embedding，除非显式 version 更新。

## 12. Testing Contract

默认测试必须覆盖：

- keyword retrieval；
- fake vector retrieval；
- hybrid merge；
- unsupported query empty result；
- duplicate chunk merge；
- ToolRegistry execution；
- ToolCallTrace output shape；
- Workspace PolicyEvidence write；
- Execution Tree evidence node；
- no real provider dependency。

Live tests 必须显式 opt-in。

## 13. Risk Boundary

`search_aftersale_policy` remains LOW risk.

RAG evidence 不得声称：

- 已退款；
- 已换货；
- 已补偿；
- 已修改支付；
- 已修改物流；
- 已关闭争议；
- 已执行任何真实高风险业务动作。
