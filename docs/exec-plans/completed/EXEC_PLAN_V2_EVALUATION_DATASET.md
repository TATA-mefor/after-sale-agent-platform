# EXEC_PLAN_V2_EVALUATION_DATASET

Date: 2026-05-17

## Scope

This record closes the V2.9 Evaluation Dataset implementation task.

V2.9 implements a versioned JSONL evaluation dataset and an offline deterministic runner for current after-sale Agent
planning behavior. It evaluates planning output, planned tools, policy retrieval direction, risk level, approval
requirement, and plan validity. It does not call a real LLM, use LLM-as-judge, connect to databases, Redis, vector
stores, or mutate business state.

## What Changed

- Added `docs/evaluation/aftersale_cases.jsonl` with 15 cases.
- Added `docs/evaluation/EVALUATION.md`.
- Added `EvaluationApplicationService`.
- Added `EvaluationCase`.
- Added `EvaluationExpected`.
- Added `EvaluationResult`.
- Added `EvaluationReport`.
- Added `EvaluationMetric`.
- Added `EvaluationFailure`.
- Added evaluation tests for loading, report generation, passing single-intent and multi-intent cases, planned tool
  checks, high-risk approval gap reporting, invalid dataset handling, and offline rule-based execution.

## Evaluation Flow

```text
JSONL case
→ EvaluationApplicationService
→ RuleBasedAgentPlanner
→ AgentPlanValidator
→ search_aftersale_policy through ToolRegistry
→ field-level comparisons
→ EvaluationResult
→ EvaluationReport
```

## Metrics

The report includes:

```text
totalCases
passedCases
failedCases
intentAccuracy
subtaskTypeAccuracy
toolCallAccuracy
riskLevelAccuracy
policyMatchAccuracy
approvalRequirementAccuracy
planValidityRate
```

## Boundary

- Default evaluation uses `RuleBasedAgentPlanner`.
- Future LLM evaluation must be explicit opt-in.
- Policy category checks use the controlled policy retrieval tool.
- Evaluation failures include `caseId` and failed field.
- Evaluation does not create or mutate tickets, agent runs, traces, or approvals.
- No default dependency on real LLM, API key, database, Redis, vector store, or network.

## Validation

Required commands:

```bash
mvn test
mvn checkstyle:check
mvn spotbugs:check
mvn test -Dtest=ArchitectureTest
```

Final results are recorded in the Review Packet.

## Completion Signal

TASK_COMPLETE
