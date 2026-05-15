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
- Each result contains: `policyId`, `category`, `matchedText`, `matchReason`
- Empty result output: `results` is empty and `message` explains that no policy matched
- Business path: `ToolRegistry -> SearchAfterSalePolicyTool -> PolicyApplicationService`
- Storage: V1 in-memory policy repository

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

V2.4 may add Specialist Agent Handlers for subtask execution. Handler introduction does not change the tool contract:

- handlers must call tools through `ToolRegistry`;
- handlers cannot call business repositories directly;
- handlers cannot invent tool names outside registered `ToolDefinition`s;
- every handler-triggered tool call must produce `ToolCallTrace`;
- high-risk actions remain approval-bound and cannot be executed directly by handlers;
- no handler may perform real refund, real exchange, coupon compensation, payment mutation, logistics mutation, or
  dispute closure in V2.4.
