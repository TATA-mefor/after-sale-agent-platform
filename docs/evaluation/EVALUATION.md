# AfterSale Agent Evaluation

## Goal

V2.9 adds an offline, deterministic evaluation dataset for the after-sale Agent planner. The goal is to measure current
planning behavior across intent classification, subtask planning, tool planning, policy retrieval direction, risk level,
approval requirement, and plan validity.

This is not an LLM-as-judge workflow. The default runner uses `RuleBasedAgentPlanner` and runs without real LLM calls,
API keys, network access, databases, Redis, or vector stores.

## Dataset

The versioned dataset lives at:

```text
docs/evaluation/aftersale_cases.jsonl
```

Each line is one JSON object with these fields:

```text
caseId
userId
orderId
input
expectedIntent
expectedSubtaskTypes
expectedTools
expectedRiskLevel
expectedPolicyCategories
expectedRequiresApproval
notes
```

The initial dataset contains 15 cases covering:

- quality return / refund;
- size exchange;
- refund-only;
- repair consultation;
- signed-but-not-received logistics issue;
- coupon consultation;
- special goods return restriction;
- after-sale deadline question;
- paid-but-not-shipped cancellation;
- multi-intent return + exchange;
- multi-intent return + coupon;
- multi-intent return + exchange + coupon;
- high-risk manual approval;
- general consultation;
- unknown input.

V2.9 used this dataset to expose deterministic fallback gaps. V2.10 keeps the same dataset and improves the
rule-based fallback so refund-only, coupon consultation, two-intent combinations, high-risk terms, and approval
requirements are covered without reading `caseId` or hard-coding dataset answers.

## Metrics

The evaluation report includes:

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

Metric definitions:

- `intentAccuracy`: expected intent equals actual `AgentPlan.intent`.
- `subtaskTypeAccuracy`: expected subtask type list equals actual planned subtask type list.
- `toolCallAccuracy`: expected tool set equals actual planned tool set.
- `riskLevelAccuracy`: expected risk level equals actual `AgentPlan.riskLevel`.
- `policyMatchAccuracy`: policy categories returned by the controlled policy tool contain all expected categories.
- `approvalRequirementAccuracy`: expected approval requirement equals actual HIGH-risk requirement.
- `planValidityRate`: generated plan passes `AgentPlanValidator`.

Every failure records:

```text
caseId
field
expected
actual
message
```

## How To Run

The evaluation runner is covered by the default Maven test suite:

```bash
mvn test -Dtest=EvaluationApplicationServiceTest
```

The full project validation remains:

```bash
mvn test
mvn checkstyle:check
mvn spotbugs:check
mvn test -Dtest=ArchitectureTest
```

## Current Limits

- Evaluation currently checks planning and read-only policy retrieval direction.
- It does not execute full `AgentRun` flows by default.
- It does not mutate tickets, agent runs, approval requests, or traces.
- It does not use LLM-as-judge.
- It does not call a real LLM provider.
- It does not connect to MySQL, Redis, PGvector, or external networks.

## V2.10 Robustness Notes

V2.10 improves `RuleBasedAgentPlanner` as the deterministic fallback used by default evaluation. The fallback now:

- recognizes refund-only wording such as only-refund, no-return refund, not-yet-shipped cancellation, and missing-goods
  refund consultation;
- recognizes coupon-only and coupon consultation wording and emits `COUPON_CONSULTATION` subtasks;
- splits two-intent messages such as return + exchange, return + coupon, and logistics + refund-only;
- marks high-risk refund, complaint, platform escalation, compensation, dispute-closing, repeated after-sale, and
  high-amount language as `HIGH` risk or high-risk subtasks;
- keeps every generated plan under `AgentPlanValidator`, `ToolRegistry`, approval, trace, and workspace boundaries.

The evaluation regression target is now stronger than V2.9: default rule-based evaluation must pass at least 13 of the
15 versioned cases, with all generated plans valid. The current V2.10 rule set is still keyword-based. It is not a
semantic model, does not call a real LLM, does not judge free-form final answer quality, and does not add real coupon,
refund, exchange, logistics, database, Redis, or vector integrations.

## Future LLM Evaluation

Future LLM evaluation must be explicit opt-in. It must:

- keep the JSONL dataset versioned;
- run through `AgentPlanParser` and `AgentPlanValidator`;
- never run as part of default `mvn test`;
- require explicit local configuration for API keys;
- keep failures structured by `caseId` and field;
- avoid LLM-as-judge unless a separate decision record defines that boundary.
