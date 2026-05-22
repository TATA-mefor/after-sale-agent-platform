
# RAG Evaluation

## 1. 目标

本文件定义 V4 RAG / Skill 评测边界。V4 评测目标不是使用 LLM-as-judge，而是在默认离线路径下验证 RAG evidence、Skill selection、Tool boundary 和 no-fabrication 行为。

默认评测必须保持 deterministic，不依赖真实 LLM、API Key、PostgreSQL、PGvector、Docker、MySQL、Redis 或外部网络。

## 2. 新增评测维度

V4 在 V2/V3 evaluation 指标基础上新增：

```text
policyEvidenceRecallAccuracy
ragCitationCompleteness
unsupportedQueryNoFabricationRate
skillSelectionAccuracy
skillExecutionBoundaryPassRate
hybridRetrievalMergeCorrectness
workspaceEvidenceCompleteness
executionTreeEvidenceCompleteness
```

## 3. Case 字段建议

新增或扩展 JSONL case 字段：

```json
{
  "caseId": "RAG-001",
  "input": "我买的耳机左耳没声音，想退货退款。",
  "expectedIntent": "RETURN_AND_REFUND",
  "expectedSubtaskTypes": ["RETURN"],
  "expectedSkills": ["ReturnEligibilityAssessmentSkill"],
  "expectedTools": ["get_order_by_id", "search_aftersale_policy", "add_ticket_note"],
  "expectedPolicyCategories": ["RETURN", "QUALITY"],
  "expectedEvidenceKeywords": ["质量问题", "退货退款"],
  "expectedRetrievalModes": ["KEYWORD", "VECTOR", "HYBRID"],
  "expectedRequiresApproval": false,
  "notes": "quality return policy should be retrieved as evidence"
}
```

## 4. 指标定义

### policyEvidenceRecallAccuracy

检索结果是否包含 expectedPolicyCategories 和 expectedEvidenceKeywords。

### ragCitationCompleteness

每条被 Agent summary 引用的政策证据是否包含 chunkId、documentId、snippet、score、retrievalMode。

### unsupportedQueryNoFabricationRate

不支持的 query 是否返回空 evidence 并给出 clear message，而不是编造政策。

### skillSelectionAccuracy

Planner / dispatcher 选择的 plannedSkills 是否与 expectedSkills 一致。

### skillExecutionBoundaryPassRate

Skill 执行是否只通过 ToolRegistry 调用工具，是否产生 ToolCallTrace，是否不直接访问 Repository / VectorStore / Spring AI client。

### hybridRetrievalMergeCorrectness

HYBRID 模式是否正确合并 keyword / vector results，并按 chunkId 去重。

### workspaceEvidenceCompleteness

AgentWorkspace 是否保存来自 RAG result 的 PolicyEvidence。

### executionTreeEvidenceCompleteness

Execution Tree 是否展示 Skill node、ToolCall node 和 PolicyEvidence node。

## 5. 默认 Runner 边界

默认 runner 使用：

```text
RuleBasedAgentPlanner or FakeAgentPlanner
FakeSkillRegistry where needed
FakeEmbeddingClient
FakeVectorRepository
InMemoryPolicyRepository
No real provider
No external network
```

## 6. Live Evaluation 边界

真实 Spring AI / PGvector / provider evaluation 必须显式 opt-in：

```bash
mvn test -Dtest=V4RagLiveEvaluationTest -Dlive.rag=true -Dlive.embedding=true
```

Live evaluation 不得替代默认 deterministic evaluation。

## 7. Failure Reporting

每个 failure 必须包含：

```text
caseId
field
expected
actual
message
```

RAG failure 应额外包含：

```text
retrievalMode
query
returnedChunkIds
returnedCategories
fallbackUsed
```

## 8. Non-goals

- 不使用 LLM-as-judge 作为默认评测；
- 不把 live provider 输出作为默认 CI 判断；
- 不以长文本完全相等判断 Agent final summary；
- 不为了通过评测硬编码 caseId；
- 不删除失败 case 掩盖问题。
