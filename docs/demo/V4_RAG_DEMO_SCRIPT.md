# V4 RAG Demo Script

## Demo Purpose

This script is for local interview or project review demos. It shows the V4 path across Spring Boot, Agent execution,
ToolRegistry, HYBRID RAG policy evidence, ToolCallTrace, AgentWorkspace, Execution Tree, and deterministic RAG
evaluation.

The default demo path is offline and uses fake / in-memory dependencies. It does not require an API key, PostgreSQL,
PGvector, Docker, MySQL, Redis, a real LLM, a real embedding provider, or external network access.

## Demo Boundaries

- `search_aftersale_policy` is a LOW-risk read-only tool.
- ToolRegistry remains the tool execution entry point.
- RAG evidence is evidence-only and is not a business action.
- The demo does not execute real refund, exchange, compensation, payment, logistics, inventory, coupon issuance, or
  dispute closure.
- AgentWorkspace is single-AgentRun working memory, not long-term memory.
- ToolCallTrace remains the audit source for tool input/output.
- Execution Tree is a read-only explanation view.

## Prerequisites

- JDK and Maven.
- Default Spring profile.
- No PostgreSQL / PGvector setup.
- No Docker setup.
- No API key.

Optional live paths are documented separately:

- `docs/demo/V4_POLICY_INGESTION_PIPELINE.md`
- `docs/demo/V4_PGVECTOR_LOCAL_SETUP.md`
- `docs/decisions/DECISION_V4_SPRING_AI_ADAPTER.md`

## Start App

```bash
mvn spring-boot:run
```

Check the app:

```bash
curl http://localhost:8080/api/health
```

Expected:

```json
{
  "status": "UP",
  "service": "after-sale-agent-platform"
}
```

## Scenario A: HYBRID Policy Search Tool Demo

The public runtime path invokes tools through AgentRun. For a direct tool-level demo, use the same ToolRegistry input
shape in a local test or debug harness:

```json
{
  "toolName": "search_aftersale_policy",
  "arguments": {
    "query": "这件衣服不合适想退货，售后期限怎么算",
    "retrievalMode": "HYBRID",
    "topK": 5,
    "minScore": 0.0,
    "category": "RETURN",
    "productType": "clothing"
  }
}
```

Expected output shape:

```json
{
  "query": "这件衣服不合适想退货，售后期限怎么算",
  "retrievalMode": "HYBRID",
  "results": [
    {
      "policyId": "POL-RETURN-...",
      "matchedText": "退货政策片段...",
      "matchReason": "MERGED_HYBRID"
    }
  ],
  "evidences": [
    {
      "evidenceId": "rag-...",
      "policyId": "POL-RETURN-...",
      "documentId": "DOC-RETURN-...",
      "chunkId": "CHUNK-RETURN-...",
      "documentTitle": "售后退货政策",
      "category": "RETURN",
      "productType": "clothing",
      "snippet": "退货申请需要满足售后期限和商品状态要求...",
      "score": 0.82,
      "keywordScore": 0.78,
      "vectorScore": 0.85,
      "retrievalMode": "HYBRID",
      "source": "MERGED_HYBRID"
    }
  ],
  "fallbackUsed": false,
  "totalKeywordMatches": 1,
  "totalVectorMatches": 1,
  "message": "ok"
}
```

The score is a retrieval evidence score. It is not business decision confidence.

## Scenario B: AgentRun With RAG Evidence

Create a ticket:

```bash
curl -X POST http://localhost:8080/api/tickets \
  -H "Content-Type: application/json" \
  -d '{"userId":"U-1001","orderId":"O202605130001","message":"我买的耳机左耳没声音，怀疑质量问题，想退货退款。"}'
```

Trigger AgentRun:

```bash
curl -X POST http://localhost:8080/api/tickets/{ticketId}/agent-runs
```

Expected AgentRun response shape:

```json
{
  "code": "SUCCESS",
  "message": "ok",
  "data": {
    "runId": "RUN-...",
    "status": "SUCCEEDED",
    "toolCalls": [
      "get_order_by_id",
      "search_aftersale_policy",
      "add_ticket_note"
    ],
    "finalSuggestion": "包含简洁政策证据摘要..."
  }
}
```

Expected policy evidence in final summary:

```text
政策证据[KEYWORD|VECTOR|HYBRID]: category=RETURN / policyId=POL-... / chunkId=CHUNK-... / score=0.82 / 退货政策片段...
```

Query ToolCallTrace:

```bash
curl http://localhost:8080/api/agent-runs/{runId}/traces
```

Expected trace excerpt:

```json
{
  "toolName": "search_aftersale_policy",
  "status": "SUCCEEDED",
  "inputJson": "{\"query\":\"...\",\"retrievalMode\":\"HYBRID\"}",
  "outputJson": "{\"retrievalMode\":\"HYBRID\",\"evidences\":[{\"evidenceId\":\"rag-...\",\"source\":\"MERGED_HYBRID\",\"score\":0.82}],\"fallbackUsed\":false}"
}
```

Expected workspace visibility:

```text
AgentWorkspace.PolicyEvidence
- evidenceId: rag-...
- retrievalMode: HYBRID
- source: MERGED_HYBRID
- policyId or documentId/chunkId: present when available
- snippet: short policy evidence only
```

## Scenario C: Execution Tree Evidence View

Query Execution Tree:

```bash
curl http://localhost:8080/api/agent-runs/{runId}/execution-tree
```

Expected response excerpt:

```json
{
  "runId": "RUN-...",
  "toolCalls": [
    {
      "toolName": "search_aftersale_policy",
      "status": "SUCCEEDED"
    }
  ],
  "policyEvidence": [
    {
      "evidenceId": "rag-...",
      "retrievalMode": "HYBRID",
      "source": "MERGED_HYBRID",
      "documentId": "DOC-RETURN-...",
      "chunkId": "CHUNK-RETURN-...",
      "score": 0.82,
      "snippet": "退货政策片段..."
    }
  ]
}
```

Execution Tree is read-only. Querying it must not change Ticket, AgentRun, ToolCallTrace, ApprovalRequest, Workspace,
or retrieval state.

## Scenario D: RAG Evaluation

Run the deterministic RAG retrieval evaluation tests:

```bash
mvn test -Dtest=RagEvaluationApplicationServiceTest
```

The evaluation dataset is:

```text
docs/evaluation/rag_policy_cases.jsonl
```

Expected metrics:

```json
{
  "totalCases": 15,
  "passRate": 1.0,
  "evidenceRecallPassRate": 1.0,
  "citationCompletenessRate": 1.0,
  "safetyPassRate": 1.0,
  "fallbackAccuracy": 1.0
}
```

V4.6.1 RAG evaluation uses deterministic checks. It does not use LLM-as-judge, does not call real LLMs, does not call
real embedding providers, and does not connect PostgreSQL / PGvector.

## Optional Live Paths

The default demo is the offline path. Live provider and PGvector work remain opt-in:

- Spring AI adapter boundary: `docs/decisions/DECISION_V4_SPRING_AI_ADAPTER.md`
- PGvector local setup: `docs/demo/V4_PGVECTOR_LOCAL_SETUP.md`
- Policy ingestion foundation: `docs/demo/V4_POLICY_INGESTION_PIPELINE.md`

Use live paths only when explicitly configured. They are not required for the interview demo or default validation.

## Troubleshooting

- Port occupied: stop the process using port `8080` or configure a different Spring Boot port locally.
- No evidence returned: check the query, category, productType, topK, and minScore values.
- Invalid retrievalMode: use exactly `KEYWORD`, `VECTOR`, or `HYBRID`.
- Vector side unavailable: HYBRID may fall back to keyword evidence; VECTOR should return a clear unavailable message.
- Live provider missing key: use the default offline path, or configure the provider according to the Spring AI adapter
  docs.
- PGvector schema not loaded: use the PGvector local setup doc; it is not part of the default demo.
- Fake / in-memory path differs from live path: default demo data is deterministic and local, while live provider quality
  depends on configured documents, embeddings, and vector store state.

## Validation

```bash
mvn test
mvn checkstyle:check
mvn spotbugs:check
mvn test -Dtest=ArchitectureTest
```

V4.6.2 adds this demo script and docs harness coverage only. It does not add runtime behavior, change RAG retrieval,
change ToolRegistry semantics, change ToolCallTrace schema, change Workspace writing, or change Execution Tree runtime.
