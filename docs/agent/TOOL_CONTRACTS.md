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

### search_aftersale_policy

- Risk: `LOW`
- Requires approval: `false`
- Input: `query`
- Output: `results`
- Each result contains: `policyId`, `category`, `matchedText`, `matchReason`
- Empty result output: `results` is empty and `message` explains that no policy matched
- Business path: `ToolRegistry -> SearchAfterSalePolicyTool -> PolicyApplicationService`
- Storage: V1 in-memory policy repository

## V1 Demo Tool Chain

The final V1 Agent demo invokes:

1. `search_aftersale_policy`
2. `add_ticket_note`

The original V1 roadmap mentioned `get_order_by_id` and `get_user_orders`. Those tools are not implemented in the
final V1 codebase and are treated as V2 candidates. The V1 demo keeps `orderId` on the ticket and focuses on policy
evidence, low-risk tool execution, and auditable trace records.
