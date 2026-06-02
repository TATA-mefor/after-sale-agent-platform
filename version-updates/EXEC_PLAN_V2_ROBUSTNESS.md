# EXEC_PLAN_V2_ROBUSTNESS

Date: 2026-05-17
Status: Completed

## Scope

V2.10 improves deterministic rule-based fallback planning based on V2.9 evaluation findings. The task focuses on
`RuleBasedAgentPlanner` intent, subtask, and risk coverage. It does not change the LLM planner, ToolRegistry,
Approval, Trace, Workspace, persistence, or external infrastructure boundaries.

## Goals

- Improve refund-only recognition.
- Improve coupon-only and coupon consultation recognition.
- Improve two-intent subtask generation.
- Improve high-risk detection and approval requirement signaling.
- Keep evaluation offline, deterministic, and validated through `AgentPlanValidator`.
- Reduce evaluation failures without reading `caseId` or hard-coding dataset answers.

## Expected Code Changes

- Update `RuleBasedAgentPlanner` rule coverage and priority.
- Add focused planner tests for refund-only, coupon consultation, two-intent splits, and high-risk approval signals.
- Update evaluation tests to assert improved V2.10 metrics while preserving offline execution.

## Expected Document Changes

- Update `docs/evaluation/EVALUATION.md` with V2.10 robustness notes and fallback boundaries.
- Update `docs/quality/QUALITY_SCORE.md` with V2.10 status.
- Update `EXEC_PLAN_V2.md` to mark V2.10 completed.
- Update this execution plan with final validation results.

## Risks

- Broader keyword rules may over-classify unrelated messages.
- More high-risk keywords may increase approval routing for ambiguous consultations.
- Policy retrieval remains keyword-based and has no coupon policy content yet, so coupon evaluation remains bounded by
  current policy data.

## What Changed

- Expanded `RuleBasedAgentPlanner` refund-only, coupon consultation, logistics, return, exchange, and high-risk keyword
  coverage.
- Added generic subtask detection for one-intent coupon consultation and two-or-more intent combinations.
- Propagated `HIGH` plan risk to generated subtasks so approval boundaries remain visible.
- Adjusted in-memory policy keyword priority so special goods and repair queries are not swallowed by generic return or
  quality keywords.
- Added planner and evaluation regression tests for the V2.10 robustness cases.

## Evaluation Impact

- V2.10 evaluation tests now require at least 13 of 15 JSONL cases to pass.
- `planValidityRate` remains required at 100%.
- High-risk approval expectation is now asserted as a passing behavior for the known high-risk fallback scenario.
- The implementation does not read `caseId` or hard-code dataset answers.

## Boundary

- Default evaluation still uses `RuleBasedAgentPlanner`.
- No real LLM, API Key, external network, database, Redis, vector store, frontend, real refund, real exchange, or real
  coupon compensation was added.
- Agent plans still pass through `AgentPlanValidator`, and runtime execution still goes through `ToolRegistry`,
  Approval, Trace, and Workspace boundaries.

## Validation

Required commands passed:

```bash
mvn test
mvn checkstyle:check
mvn spotbugs:check
mvn test -Dtest=ArchitectureTest
```

## Completion Signal

TASK_COMPLETE
