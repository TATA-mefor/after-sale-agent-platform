# Agent Risk Policy

M5 enforces risk at the tool boundary.

## LOW

Allowed to execute directly when inputs are valid.

Examples:

- `get_order_by_id`
- `get_user_orders`
- `create_aftersale_ticket`
- `add_ticket_note`
- `search_aftersale_policy`

## MEDIUM

Allowed to execute directly only when the requested action stays within the ticket lifecycle and does not close a
dispute or affect funds, inventory, or user rights.

Example:

- `update_ticket_status`

`update_ticket_status` returns `REQUIRES_APPROVAL` when the requested status is `CLOSED`.

## HIGH

Must return `REQUIRES_APPROVAL` before the business executor runs.

Examples:

- refund
- compensation
- payment status change
- dispute closure

M5 does not implement real refund, compensation, payment, inventory, or database-side approval execution.

## V2.2 Boundary

V2.2 adds low-risk order query tools backed by in-memory demo data. They read demo order facts only and do not connect
to a real order center, logistics provider, payment provider, database, refund system, or inventory system.

Closing a ticket through `update_ticket_status` remains blocked with `REQUIRES_APPROVAL`.

## V2.3 Multi-Intent Risk Boundary

V2.3 introduces planned subtasks for complex after-sale messages. Risk is evaluated per subtask and per planned tool.

Rules:

- `LOW` subtasks may plan read-only tools or low-risk note writing;
- `MEDIUM` subtasks may plan controlled ticket lifecycle actions, but not funds, inventory, coupons, payment, or dispute
  closure;
- `HIGH` subtasks must stop at planning or approval handoff and cannot be executed automatically;
- subtask `riskLevel` must be one of `LOW`, `MEDIUM`, or `HIGH`;
- subtask output must not claim that refund, exchange, coupon compensation, payment change, or dispute closure has
  already completed;
- ToolRegistry and the existing tool risk contract remain the execution boundary.

V2.3 does not implement real refund, real exchange, real coupon compensation, real logistics, or real payment actions.

## V2.4 Specialist Handler Risk Boundary

V2.4 Specialist Handlers are execution strategies, not privileged business actors.

Rules:

- handler risk is bounded by the `AgentSubtask.riskLevel` and each planned tool's risk level;
- handlers must use ToolRegistry so existing tool risk checks still apply;
- handlers must not bypass approval rules for `HIGH` actions;
- handlers must not directly execute real refund, exchange, coupon compensation, payment, logistics, or dispute closure;
- handlers must return a structured result when human approval is required;
- handler failures must be visible in AgentRun results and ToolCallTrace where tools were attempted.

## V2.5 Policy Retrieval Risk Boundary

`search_aftersale_policy` remains a LOW-risk read-only tool.

Rules:

- Specialist Handlers may call policy retrieval before low-risk action tools such as `add_ticket_note`;
- policy retrieval must go through `ToolRegistry`;
- policy retrieval must produce `ToolCallTrace` when executed in an AgentRun context;
- empty policy results are allowed and must be surfaced as structured empty results;
- empty policy results must not be converted into invented policy evidence;
- V2.5 does not add real VectorStore, PGvector, embedding calls, network retrieval, refunds, exchanges, compensation,
  payment mutation, or logistics mutation.

## V2.6 Agent Workspace Risk Boundary

Agent Workspace is structured execution memory for one `AgentRun`, not a privileged execution actor.

Rules:

- workspace must not bypass ToolRegistry;
- workspace must not directly access repositories;
- workspace may store `RiskFlag` records for later summary or approval routing;
- workspace must not mark high-risk business actions as completed;
- workspace must not store API keys, sensitive credentials, full long prompts, or raw long LLM outputs;
- workspace must not become long-term memory, user profile memory, vector memory, or cross-session memory;
- ToolCallTrace remains the audit trail for tool calls.

## V4 Skill Risk Boundary

V4 extends risk control from Tool level to Skill level.

Skill risk is calculated as:

```text
max(subtask riskLevel, requiredTools riskLevel, intended business outcome riskLevel)
```

In V4.1, `SkillRiskEvaluator` enforces the required-tools portion of this rule at SkillRegistry construction time:
a Skill cannot declare a risk level lower than the highest risk level among its `requiredTools`. Subtask risk and
intended business outcome risk remain enforced by the existing Specialist Handler, RiskPolicy, and Approval boundaries
until the later Skill runtime migration.

### LOW Skill

Allowed to execute directly when it only performs read-only or low-risk actions.

Examples:

- order fact lookup;
- policy evidence retrieval;
- item-level recommendation text generation;
- internal ticket note writing.

### MEDIUM Skill

Allowed only for controlled ticket lifecycle operations that do not affect funds, inventory, logistics, payment state, coupons, or user rights finalization.

### HIGH Skill

Must stop at approval handoff before business execution.

Examples:

- refund;
- compensation;
- coupon issuance;
- payment status change;
- logistics mutation;
- inventory mutation;
- dispute closure.

V4 RAG evidence does not lower risk. If a policy snippet says a user may be eligible for refund, the system may recommend human review or approval flow, but must not execute a real refund.

V4.1 does not add real refund, exchange, payment, logistics, inventory, or coupon compensation actions. The high-risk
human-routing Skill is a boundary signal only and must not execute external business mutations.
