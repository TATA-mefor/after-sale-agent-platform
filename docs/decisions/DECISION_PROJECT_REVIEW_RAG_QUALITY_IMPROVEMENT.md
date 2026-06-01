# Decision: Project Review RAG Quality Improvement

Date: 2026-06-01

Status: Completed

## Context

项目审查指出当前 RAG 质量能力仍停留在策略检索层面：已有 KEYWORD / VECTOR / HYBRID policy evidence
retrieval，但没有 reranking、query rewriting、RRF、chunk window expansion 或 live PGvector ranking comparison。

该结论基本准确，但需要保持当前 V4 边界：RAG evidence 是政策证据，不是业务决策，不执行业务动作；
`search_aftersale_policy` remains LOW-risk read-only ToolRegistry tool；默认测试必须继续 fake / in-memory /
offline。

## Current RAG Baseline

当前已完成的 RAG baseline：

- `search_aftersale_policy` supports KEYWORD / VECTOR / HYBRID。
- KEYWORD path preserves existing policy search behavior。
- VECTOR path uses EmbeddingClient abstraction + PolicyVectorRepository contract。
- HYBRID path uses `RagPolicyEvidenceMergeService` 合并 keyword / vector evidence。
- keyword / vector evidence mappers exist。
- default vector path uses `FakeEmbeddingClient` and `InMemoryPolicyVectorRepository`。
- RAG evidence can surface through ToolCallTrace / Workspace / Execution Tree。
- deterministic RAG evaluation exists。
- RAG evidence is evidence-only。

## Current Evaluation Baseline

当前 RAG evaluation baseline：

- versioned deterministic JSONL cases live under `docs/evaluation/rag_policy_cases.jsonl`。
- no LLM-as-judge by default。
- metrics include evidence recall、source matching、fallback accuracy、empty-result accuracy、citation completeness
  and safety pass rate。
- evaluation is offline and does not call real provider。
- current dataset is intentionally small / synthetic and can be expanded。

## Problem Statement

当前缺口：

- reranking is not implemented。
- query rewriting is not implemented。
- RRF is not implemented。
- chunk window expansion is not implemented。
- JdbcPolicyVectorRepository is not implemented。
- live PGvector validation is not completed。
- Spring AI VectorStore production path is not enabled。
- 没有 production-scale relevance benchmark。
- HYBRID score 仍是 deterministic heuristic。
- chunk snippet 可能过窄。
- query 表达和政策措辞不一致时可能影响召回。
- vector side failure 当前只能 fallback，不代表 retrieval quality 已充分优化。

## Decision

阶段 5 采用 evaluation-driven 分阶段策略：

- 不实现新的 RAG runtime。
- 不修改 retrieval algorithm。
- 新增 decision record、docs、docs harness tests。
- 保持现有 KEYWORD / VECTOR / HYBRID runtime。
- 保持 default fake / in-memory offline path。
- 以现有 deterministic RAG evaluation 作为后续改动 gate。
- 任何 RAG 质量增强必须先有 deterministic offline tests。

后续推荐顺序：

1. 扩展 RAG evaluation dataset。
2. 基于失败 case 判断是 query mismatch、ranking issue、snippet context issue 还是 source coverage issue。
3. 先优化 hybrid scoring / RRF 这类可离线确定性测试的策略。
4. 再评估 chunk window expansion。
5. query rewriting 只有在可控、可审计、可离线测试时推进。
6. reranking 先定义 abstraction，再决定是否接真实 provider。
7. live PGvector / JdbcPolicyVectorRepository 仍作为 opt-in production hardening path。

## Reranking Evaluation

Reranking 可能提升 relevance ordering，尤其是 keyword 和 vector 都返回相近候选时。但 reranker provider 可能
引入外部依赖、成本、延迟和隐私风险。

未来如果实现：

- 先新增 reranker abstraction。
- 默认测试使用 fake deterministic reranker。
- 不允许默认测试调用真实 reranker。
- real reranker/embedding provider must be opt-in。
- reranking score 仍是 retrieval score，不是业务决策置信度。

当前阶段 reranking is not implemented。

## Query Rewriting Evaluation

Query rewriting 可以改善用户自然语言和政策条款之间的表达差距。但如果使用 LLM rewriting，会引入不稳定性、
成本和 prompt safety 问题。

未来如果实现：

- rewritten query 必须可审计。
- 不能把 rewritten query 当作用户原话。
- 不能泄露 raw prompt。
- 默认测试必须 deterministic。
- 优先支持 rule-based / offline deterministic rewrite 或 fake LLM rewrite。

当前阶段 query rewriting is not implemented。

## RRF / Hybrid Scoring Evaluation

当前 HYBRID merge 使用 deterministic weighted scoring / dedup / fallback。RRF 可以作为 keyword/vector rank
fusion 的候选，因为它对不同 score scale 更稳。

未来如果评估：

- 必须用 evaluation cases 对比现有 weighted merge。
- 必须保持 deterministic tests。
- scoring change 不得改变 RAG evidence-only 边界。

当前阶段 RRF is not implemented，也不更改 hybrid scoring。

## Chunk Window Expansion Evaluation

当前 evidence snippet 可能只覆盖命中 chunk。Chunk window expansion 可以把前后 chunk 作为上下文，但风险是返回
过长 evidence、引入无关信息、增加敏感文本暴露。

未来如果实现：

- 需要 max window、max chars、source citation、dedup 规则。
- 不能返回完整原文。
- evidence 仍必须只作为政策证据。

当前阶段 chunk window expansion is not implemented。

## Evaluation Dataset Expansion Strategy

后续 dataset expansion 应优先增加：

- hard negative cases。
- policy conflict cases。
- synonym / paraphrase cases。
- low-score no-answer cases。
- fallback cases。
- safety cases，防止 evidence 声称业务动作完成。

Metrics 仍默认 deterministic。LLM-as-judge 只能作为 optional analysis，不作为 default CI gate。

## Provider / PGvector Boundary

Provider / PGvector 边界：

- JdbcPolicyVectorRepository is not implemented。
- live PGvector validation is not completed。
- Spring AI VectorStore production path is not enabled。
- real embedding/reranking provider must be opt-in。
- default `mvn test` 不连接 PostgreSQL / PGvector。
- 默认 fake / in-memory path 必须保留。

## ToolRegistry / Evidence-only Boundary

`search_aftersale_policy` remains LOW-risk read-only ToolRegistry tool。
search_aftersale_policy remains LOW-risk read-only ToolRegistry tool.

RAG evidence is evidence-only：

- 不执行退款、换货、补偿、支付、物流或争议关闭。
- RAG score is not business decision confidence。
- high-risk actions require Approval。
- LLM must not directly execute tools。
- future RAG improvements must not bypass ToolRegistry / RiskPolicy / Approval / Trace / Workspace / Execution Tree。

## Default Offline Boundary

阶段 5 docs harness tests 只读文件。默认验证不需要：

- real LLM；
- API Key；
- PostgreSQL；
- PGvector；
- Docker；
- MySQL；
- Redis；
- real embedding provider；
- real reranker provider；
- Spring AI VectorStore；
- external network。

## Security / Secret Safety

RAG 质量文档、测试和后续 evaluation cases 不得包含：

- API keys；
- database passwords；
- tokens；
- raw prompts；
- raw provider responses；
- raw dataset paths；
- local absolute paths；
- customer private data。

## Non-goals

阶段 5 不做：

- 不实现 reranking。
- 不实现 query rewriting。
- 不实现 RRF。
- 不实现 chunk window expansion。
- 不扩展 runtime evaluation runner。
- 不实现 JdbcPolicyVectorRepository。
- 不连接 live PGvector。
- 不接 Spring AI VectorStore。
- 不新增 public RAG HTTP endpoint。
- 不修改 `search_aftersale_policy` runtime。
- 不让 policy evidence 执行业务动作。

## Alternatives Considered

### 立即实现 reranking

拒绝。没有先扩展 deterministic evaluation dataset，无法证明收益，也容易引入真实 provider 依赖。

### 立即使用 LLM query rewriting

拒绝。默认路径需要离线、确定性；LLM rewriting 需要额外 prompt safety、审计和 fallback 设计。

### 立即替换 HYBRID scoring 为 RRF

拒绝。RRF 是合理候选，但应先通过同一批 evaluation cases 和扩展数据集对比现有 weighted merge。

### 立即启用 Spring AI VectorStore production path

拒绝。当前 V4 只完成 adapter、profile、schema、contract、fake / in-memory 默认路径和 opt-in boundary。

## Consequences

Positive:

- 将 RAG 质量问题转化为可验证路线。
- 保留当前 KEYWORD / VECTOR / HYBRID runtime 稳定性。
- 保留默认 fake / in-memory offline path。
- 避免把 future RAG 能力误写成已完成 runtime。

Costs:

- 阶段 5 不直接提升 runtime relevance。
- 需要后续独立阶段扩展 evaluation dataset 和实现候选策略。

## Follow-ups

- 扩展 `docs/evaluation/rag_policy_cases.jsonl`。
- 单独评估 RRF / hybrid scoring。
- 单独评估 chunk window expansion。
- 单独定义 reranker abstraction。
- 单独评估 deterministic query rewriting。
- 将 JdbcPolicyVectorRepository、live PGvector validation 和 Spring AI VectorStore production path 放入
  production hardening。

## Completion Signal

TASK_COMPLETE
