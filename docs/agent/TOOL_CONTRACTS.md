# Agent Tool Contracts

M5 defines the first executable tool layer. M7 Agent orchestration calls tools through `ToolRegistry` and records
ToolCallTrace entries for each tool call executed inside an AgentRun context.

## Common Contract

Each tool declares:

- `toolName`
- `description`
- `inputSchema`
- `outputSchema`
- `riskLevel`
- `requiresApproval`

Tool execution returns one of:

- `SUCCEEDED`
- `FAILED`
- `REQUIRES_APPROVAL`

Failures must include `errorCode` and `message`. High-risk tools are stopped before business execution and return
`REQUIRES_APPROVAL`.

## Final V1 Implemented Tools

### create_aftersale_ticket

- Risk: `LOW`
- Requires approval: `false`
- Input: `userId`, `orderId`, `message`
- Output: `ticketId`, `status`
- Business path: `ToolRegistry -> CreateAfterSaleTicketTool -> TicketApplicationService`

### update_ticket_status

- Risk: `MEDIUM`
- Requires approval: `false`
- Input: `ticketId`, `status`, optional `reason`
- Output: `ticketId`, `status`
- Business path: `ToolRegistry -> UpdateTicketStatusTool -> TicketApplicationService`
- Note: closing a ticket is treated as an approval-required action and returns `REQUIRES_APPROVAL`.

### add_ticket_note

- Risk: `LOW`
- Requires approval: `false`
- Input: `ticketId`, `note`
- Output: `ticketId`, `status`, `internalNote`
- Business path: `ToolRegistry -> AddTicketNoteTool -> TicketApplicationService`

## M6 Tools

### get_order_by_id

- Risk: `LOW`
- Requires approval: `false`
- Input: `orderId`
- Output: `orderId`, `userId`, `productId`, `productName`, `orderStatus`, `paidAmount`, `paidAt`, `deliveredAt`,
  `aftersaleDeadline`, `whetherInAftersaleWindow`
- Missing order output: failed tool result with a clear `Order not found: {orderId}` message
- Business path: `ToolRegistry -> GetOrderByIdTool -> OrderApplicationService`
- Storage: V2.2 in-memory demo order repository

### get_user_orders

- Risk: `LOW`
- Requires approval: `false`
- Input: `userId`
- Output: `orders`
- Each order contains: `orderId`, `userId`, `productId`, `productName`, `orderStatus`, `paidAmount`, `paidAt`,
  `deliveredAt`, `aftersaleDeadline`, `whetherInAftersaleWindow`
- Business path: `ToolRegistry -> GetUserOrdersTool -> OrderApplicationService`
- Storage: V2.2 in-memory demo order repository

### search_aftersale_policy

- Risk: `LOW`
- Requires approval: `false`
- Input: `query`
- Output: `results`
- Each result contains: `policyId`, `category`, `productType`, `matchedText`, `matchReason`
- Empty result output: `results` is empty and `message` explains that no policy matched
- Business path: `ToolRegistry -> SearchAfterSalePolicyToolExecutor -> PolicyApplicationService -> PolicyRepository`
- Storage: V2.5 in-memory keyword repository, replaceable by future VectorStore / PGvector

## V2.2 Demo Tool Chain

The V2.2 Agent demo invokes:

1. `get_order_by_id`
2. `search_aftersale_policy`
3. `add_ticket_note`

The order tools use in-memory demo data only. They do not connect to a real order center, logistics provider, payment
system, or database. They provide order facts so Agent suggestions can cite both order evidence and policy evidence.

## V2.3 Multi-Intent Planning Tool Boundary

V2.3 allows `AgentPlan` / `MultiIntentAgentPlan` to contain multiple subtasks. Each subtask can declare
`plannedTools`, but the contract does not change:

- every planned tool must be registered in `ToolRegistry`;
- LLM output cannot create ad hoc tool names;
- subtasks cannot execute tools directly;
- `AgentApplicationService` must execute planned tools through `ToolRegistry`;
- every actual tool call must continue to produce a `ToolCallTrace`;
- trace input JSON includes subtask metadata for multi-intent runs;
- no subtask may declare real refund, real exchange, coupon compensation, payment change, or dispute closure as already
  completed.

V2.3 does not add a full coupon system, real refund tool, real exchange tool, real logistics integration, or real payment
integration.

## V2.4 Specialist Handler Tool Boundary

V2.4 adds Specialist Agent Handlers for subtask execution. Handler introduction does not change the tool contract:

- handlers must call tools through `ToolRegistry`;
- handlers cannot call business repositories directly;
- handlers cannot invent tool names outside registered `ToolDefinition`s;
- every handler-triggered tool call must produce `ToolCallTrace`;
- high-risk actions remain approval-bound and cannot be executed directly by handlers;
- no handler may perform real refund, real exchange, coupon compensation, payment mutation, logistics mutation, or
  dispute closure in V2.4.

## V2.5 Policy Retrieval Tool Boundary

V2.5 keeps `search_aftersale_policy` as the only policy retrieval tool exposed to Agent execution.

- handlers must retrieve policy snippets through `ToolRegistry`;
- handlers must not call `PolicyRepository` or `InMemoryPolicyRepository` directly;
- policy retrieval is LOW risk and read-only;
- empty results are valid structured outputs with `results: []` and a clear `message`;
- empty results must not be treated as invented policy evidence;
- current retrieval is local in-memory keyword matching only;
- no real VectorStore, PGvector, embedding service, network search, or real LLM dependency is introduced.

## V2.6 Agent Workspace Tool Boundary

V2.6 Agent Workspace / Structured Memory does not change the tool contract.

- tools still execute only through `ToolRegistry`;
- ToolRegistry still owns tool lookup, execution, risk checks, and ToolCallTrace recording;
- workspace may store `ToolResultSummary` records derived from tool outputs;
- workspace must not execute tools directly;
- workspace must not invent tool names or bypass registered `ToolDefinition`s;
- workspace must not replace ToolCallTrace;
- workspace must not store API keys, sensitive credentials, full long prompts, or raw long LLM outputs.

## V4 Tool vs Skill Contract

V4 introduces Skill as a first-class concept above Tool. V4.1 implements the Skill foundation and registry while keeping
the existing AgentRun runtime path on Specialist Handlers.

```text
Tool = atomic executable capability
Skill = reusable composite task capability
```

Tool remains the only actual executable unit recorded by ToolCallTrace. Skill can orchestrate Tools, but Skill must not bypass ToolRegistry.

### Tool Rules

- Tool must be registered in ToolRegistry.
- Tool must declare inputSchema, outputSchema, riskLevel, requiresApproval.
- Tool execution returns SUCCEEDED, FAILED, or REQUIRES_APPROVAL.
- Every actual Tool execution inside AgentRun must produce ToolCallTrace.

### Skill Rules

- Skill must be registered in SkillRegistry.
- Skill may call one or more Tools.
- Skill must call Tools through ToolRegistry.
- Skill execution produces SkillExecutionResult.
- Skill result may be shown in Execution Tree.
- ToolCallTrace remains the audit record for actual Tool execution.

V4.1 registered Skill definitions wrap existing Specialist Handlers through a compatibility adapter. This adapter does
not make tools executable outside ToolRegistry and does not bypass ToolCallTrace, Workspace, or Approval boundaries.
Execution Tree Skill nodes are a later V4 goal.

## V4 search_aftersale_policy Contract

`search_aftersale_policy` remains LOW-risk and read-only, but supports RAG retrieval.

V4.5.1 status: schema preparation only. The project defines `RetrievalMode`, RAG search query models, RAG evidence
models, and keyword/vector result mappers for future KEYWORD / VECTOR / HYBRID output. V4.5.1 does not change
`search_aftersale_policy` runtime, ToolRegistry execution, ToolCallTrace output, AgentWorkspace writes, or AgentRun
behavior. V4.5.2 handles keyword + vector merge service, V4.5.3 handles runtime HYBRID wiring, and V4.5.4 handles
ToolCallTrace / Workspace evidence wiring.

Input:

```text
query
retrievalMode: KEYWORD | VECTOR | HYBRID
topK
minScore optional
category optional
productType optional
effectiveAt optional
subtaskId optional
```

Output:

```text
results[]
  evidenceId
  policyId optional
  documentId optional
  chunkId optional
  documentTitle optional
  category
  productType
  snippet
  score
  keywordScore optional
  vectorScore optional
  retrievalMode
  source
  effectiveFrom optional
  effectiveTo optional
message
fallbackUsed
```

The tool must not mutate Ticket, Order, Payment, Inventory, Logistics, Coupon, ApprovalRequest, or any external business system.

RAG results are evidence only. They must not claim that refund, exchange, coupon compensation, payment change,
logistics change, inventory change, or dispute closure has already been completed. ToolRegistry remains the only Agent
tool execution entry.
