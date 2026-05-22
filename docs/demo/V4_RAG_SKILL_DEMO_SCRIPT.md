
# V4 RAG + Skill Demo Script

## 1. Demo Goal

This demo shows that AfterSale-Agent V4 is not a plain chatbot and not an isolated RAG Q&A demo. It is a Spring Boot after-sale Agent platform where:

```text
policy documents are ingested
→ chunks are embedded
→ policy evidence is retrieved through RAG
→ AgentSkill orchestrates tools
→ ToolCallTrace records execution
→ AgentWorkspace stores evidence
→ Execution Tree explains the run
```

The demo must preserve the safety boundary: no real refund, exchange, payment, logistics, inventory, coupon compensation, or dispute closure is executed.

## 2. Required Run Modes

Default offline mode:

```bash
mvn spring-boot:run
```

RAG local profile, once implemented:

```bash
SPRING_PROFILES_ACTIVE=rag-postgres AFTERSALE_RAG_POSTGRES_URL='jdbc:postgresql://localhost:5432/after_sale_agent_rag' AFTERSALE_RAG_POSTGRES_USERNAME=aftersale AFTERSALE_RAG_POSTGRES_PASSWORD='<local-password>' mvn spring-boot:run
```

Optional Docker Compose RAG profile, once implemented:

```bash
docker compose -f docker-compose.yml -f docker-compose-rag.yml up --build
```

## 3. Policy Ingestion

Example policy document:

```markdown
# 售后退货退款政策

质量问题商品在签收后七天内可申请退货退款。用户需要提供问题描述和必要凭证。特殊定制商品不适用无理由退货，但质量问题仍需进入人工审核。
```

Ingestion API, once implemented:

```bash
curl -X POST http://localhost:8080/api/admin/policies/ingest   -H "Content-Type: application/json"   -H "X-Request-Id: demo-v4-ingest-001"   -d '{
    "title": "售后退货退款政策",
    "category": "RETURN",
    "productType": "electronics",
    "sourceType": "MARKDOWN",
    "content": "# 售后退货退款政策
质量问题商品在签收后七天内可申请退货退款。用户需要提供问题描述和必要凭证。特殊定制商品不适用无理由退货，但质量问题仍需进入人工审核。"
  }'
```

Query ingestion run:

```bash
curl http://localhost:8080/api/admin/policies/ingest-runs/{runId}
```

Expected result:

```text
status = SUCCEEDED
chunkCount > 0
embeddedCount > 0 when vector store is enabled
```

## 4. Create Ticket

```bash
curl -X POST http://localhost:8080/api/tickets   -H "Content-Type: application/json"   -H "X-Request-Id: demo-v4-ticket-001"   -d '{
    "userId": "U-1001",
    "orderId": "O202605130001",
    "message": "我买的耳机左耳没声音，怀疑质量问题，想退货退款。"
  }'
```

## 5. Trigger AgentRun

```bash
curl -X POST http://localhost:8080/api/tickets/{ticketId}/agent-runs   -H "X-Request-Id: demo-v4-agentrun-001"
```

Expected chain:

```text
AgentRun
→ Planner
→ AgentSubtask RETURN
→ SkillRegistry
→ ReturnEligibilityAssessmentSkill
→ ToolRegistry
→ get_order_by_id
→ search_aftersale_policy with HYBRID mode
→ add_ticket_note
→ ToolCallTrace
→ AgentWorkspace.PolicyEvidence
→ final summary
```

## 6. Query Traces

```bash
curl http://localhost:8080/api/agent-runs/{runId}/traces
```

Expected tool calls:

```text
get_order_by_id
search_aftersale_policy
add_ticket_note
```

Expected `search_aftersale_policy` output contains:

```text
chunkId
documentId
documentTitle
score
retrievalMode
snippet
fallbackUsed
```

## 7. Query Execution Tree

```bash
curl http://localhost:8080/api/agent-runs/{runId}/execution-tree
```

Expected tree shape:

```text
AgentRun
├── Subtask: RETURN
│   ├── Skill: ReturnEligibilityAssessmentSkill
│   │   ├── ToolCall: get_order_by_id
│   │   ├── ToolCall: search_aftersale_policy
│   │   └── ToolCall: add_ticket_note
│   └── PolicyEvidence
│       ├── chunkId
│       ├── score
│       └── retrievalMode
└── ApprovalRequest optional
```

## 8. Query Ticket

```bash
curl http://localhost:8080/api/tickets/{ticketId}
```

Expected ticket note / suggestion:

```text
- includes item-level order evidence when available
- includes policy evidence snippet or reference
- does not claim refund has been executed
- routes high-risk or uncertain decisions to human approval when required
```

## 9. What To Explain In Interview

Key explanation:

```text
Planner decides what should be done.
Skill decides how to perform the task safely.
ToolRegistry executes atomic tools.
RAG provides policy evidence.
ToolCallTrace proves what was executed.
Workspace keeps structured run memory.
Execution Tree makes the run inspectable.
Approval prevents high-risk automation.
```

## 10. Demo Non-goals

This demo does not execute:

- real refund;
- real exchange;
- real compensation;
- real coupon issuance;
- real payment change;
- real logistics mutation;
- real inventory mutation;
- real dispute closure.
