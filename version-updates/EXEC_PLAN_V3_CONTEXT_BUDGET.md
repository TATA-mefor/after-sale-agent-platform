# EXEC_PLAN_V3_CONTEXT_BUDGET

Status: completed

Completion date: 2026-05-18

## Goal

Add deterministic context budget and token observability controls before expanding real LLM AgentRun validation. The
LLM Planner must receive a layered, bounded prompt with clear critical/optional section handling and safe telemetry.

## Scope Completed

- Added typed prompt sections for critical and optional context.
- Added prompt budget configuration and default budget values.
- Added deterministic token estimation with `max(1, chars / 4)`.
- Added budget policy and applier outside `AgentPlannerPromptFactory`.
- Added compact tool catalog generation with only tool name, risk level, required input fields, and short purpose.
- Added prompt usage telemetry before LLM calls.
- Updated LLM planner prompt construction to use budgeted prompt build results.
- Added tests for token estimation, total budget calculation, critical-section protection, optional-section reduction,
  clear overflow errors, compact tool catalog content, sentinel phrase removal, and prompt factory collaborator
  boundaries.
- Updated README, EXEC_PLAN_V3, and QUALITY_SCORE.

## Critical Sections

Critical sections are required and must not be silently dropped:

- `systemInstructions`
- `outputSchema`
- `plannerContractSummary`
- `toolCatalogCompact`
- `riskPolicySummary`
- `ticketContext`

If critical content exceeds its section budget or is missing, the planner receives a clear prompt-budget failure instead
of a silently damaged prompt.

## Optional Sections

Optional sections can be reduced by policy:

- `conversationHistory`
- `ragContext`
- `examples`
- `debugHints`
- `extendedPolicyText`
- `nonEssentialDocs`

The project currently has no production conversation-history or RAG-context source in the default flow, so those
telemetry fields remain `0` unless optional sections are explicitly supplied.

## Overflow Strategy

When total input tokens exceed budget, reduction proceeds in this order:

1. Drop `debugHints`.
2. Drop `nonEssentialDocs` for long non-critical documents.
3. Truncate `examples`.
4. Truncate `conversationHistory`.
5. Compress `ragContext`.
6. Compress `extendedPolicyText`.
7. Compress non-critical `ticketContext` fields.
8. Throw `PromptBudgetExceededException` if the prompt still exceeds budget.

The policy never removes `outputSchema`, `toolCatalogCompact`, or `riskPolicySummary` to force the prompt into budget.

## Token Telemetry

`LlmAgentPlanner` logs estimated fields only:

- `systemPromptTokens`
- `plannerContractTokens`
- `toolCatalogTokens`
- `ticketContextTokens`
- `orderContextTokens`
- `historyTokens`
- `ragContextTokens`
- `optionalTokensDropped`
- `totalInputTokens`
- `maxOutputTokens`
- `budgetExceeded`
- `budgetAction`
- `outputTokens`
- `cacheReadTokens`

Provider output/cache token usage is `unknown` when unavailable. The implementation does not fabricate provider usage.

## Safety Boundaries

- Default tests do not call a real LLM.
- No tokenizer dependency was added.
- No external observability platform was added.
- Logs do not include full prompts, API keys, database passwords, sensitive credentials, or long raw text.
- ToolRegistry, Approval, Trace, Workspace, and Agent execution semantics were not changed.

## Validation Commands

```bash
mvn test
mvn checkstyle:check
mvn spotbugs:check
mvn test -Dtest=ArchitectureTest
```

## Completion Signal

TASK_COMPLETE
