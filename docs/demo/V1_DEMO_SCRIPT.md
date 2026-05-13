# V1 Demo Script

## 1. Demo Goal

This demo shows that AfterSale-Agent is not a plain chatbot. It is a Java Spring Boot after-sale ticket platform where
an Agent can read a ticket, classify intent, retrieve policy evidence, call business tools, write an auditable
suggestion, and expose tool traces for review.

The business problem is common in e-commerce support:

- A user describes an after-sale issue in natural language.
- Support staff need a consistent first-pass interpretation.
- Policy evidence must be visible.
- Automated actions must stay inside a controlled, low-risk boundary.
- Every Agent tool call must be auditable.

## 2. User Input

Use this request to create the demo ticket:

```json
{
  "userId": "U-1001",
  "orderId": "O202605130001",
  "message": "我买的耳机有质量问题，左耳没声音，想退货退款。"
}
```

The message intentionally contains quality issue and return/refund keywords so V1 can demonstrate deterministic intent
classification and policy retrieval without a real LLM.

## 3. Demo Steps

Start the application:

```bash
mvn spring-boot:run
```

Check health:

```bash
curl http://localhost:8080/api/health
```

Create the ticket:

```bash
curl -X POST http://localhost:8080/api/tickets \
  -H "Content-Type: application/json" \
  -d '{"userId":"U-1001","orderId":"O202605130001","message":"我买的耳机有质量问题，左耳没声音，想退货退款。"}'
```

Trigger AgentRun:

```bash
curl -X POST http://localhost:8080/api/tickets/{ticketId}/agent-runs
```

Query the ticket:

```bash
curl http://localhost:8080/api/tickets/{ticketId}
```

Query trace:

```bash
curl http://localhost:8080/api/agent-runs/{runId}/traces
```

## 4. What The Agent Does

For the demo input, the V1 Agent flow is:

1. Reads the ticket through `TicketApplicationService`.
2. Classifies the intent as `RETURN_AND_REFUND`.
3. Creates an `AgentRun` record.
4. Calls `search_aftersale_policy` through `ToolRegistry`.
5. Finds policy evidence such as `POL-QUALITY-RETURN-EXCHANGE`.
6. Builds a structured plan containing intent, steps, evidence, final suggestion, and tool calls.
7. Calls `add_ticket_note` through `ToolRegistry`.
8. Writes the Agent suggestion back to the ticket.
9. Marks the AgentRun as `SUCCEEDED`.
10. Records every tool call as `ToolCallTrace`.

## 5. Tools Called

The expected V1 trace contains:

- `search_aftersale_policy`: retrieves matching after-sale policy text and match reason.
- `add_ticket_note`: persists the Agent suggestion as an internal ticket note.

Both tools are `LOW` risk. They do not execute payment, refund, compensation, inventory, or logistics actions.

## 6. Why Trace Matters

The trace endpoint proves that Agent behavior is reviewable:

- `runId` links every tool call to one AgentRun.
- `toolName` shows what the Agent actually invoked.
- `inputJson` shows the exact structured input.
- `outputJson` shows the tool result used by the Agent.
- `status` shows success, failure, or approval-required outcome.
- `latencyMs`, `errorMessage`, and `createdAt` support debugging and audit review.

This is the main interview point: the Agent does not just return an answer. It leaves a machine-checkable execution
trail.

## 7. V1 Limits

V1 is intentionally constrained:

- No real LLM call; intent and planning are rule based.
- No real database; repositories are in-memory.
- No real payment, refund, inventory, or logistics integration.
- No multi-Agent orchestration.
- No production authentication or authorization.
- Policy retrieval is keyword/rule based, not vector RAG.

These limits keep the demo focused on the backend architecture, tool boundary, risk policy, and traceability.
