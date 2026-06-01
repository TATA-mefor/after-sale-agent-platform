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

## V4.6.1 RAG Policy Evidence Evaluation

V4.6.1 adds a separate offline, deterministic evaluation path for RAG policy evidence retrieval. It evaluates
KEYWORD / VECTOR / HYBRID policy retrieval quality; it does not evaluate Agent planning, does not create tickets, does
not create AgentRuns, and does not write ToolCallTrace, AgentWorkspace, or Execution Tree state.

The versioned RAG dataset lives at:

```text
docs/evaluation/rag_policy_cases.jsonl
```

Each JSONL case expresses:

```text
caseId
query
retrievalMode
topK
minScore
category optional
productType optional
expected.requiredRetrievalMode
expected.requiredEvidenceSources
expected.requiredCategories
expected.requiredAnySnippetContains
expected.forbiddenSnippetContains
expected.minEvidenceCount
expected.maxEvidenceCount
expected.expectFallbackUsed
expected.expectEmptyResult
```

The dataset is intentionally small and reviewable. It covers return, exchange, refund-only / not shipped refund,
logistics issues, coupon consultation, special-goods restrictions, repair / quality issues, unsupported queries, empty
evidence, vector-only evidence, keyword-only fallback, hybrid dedup, low-score filtering, and evidence-only safety.

RAG metrics are deterministic and rule-based:

- `evidenceRecallPassRate`: required category/source/snippet expectations are satisfied.
- `evidenceSourcePassRate`: expected evidence source appears, such as KEYWORD_POLICY, VECTOR_CHUNK, or MERGED_HYBRID.
- `retrievalModePassRate`: returned retrievalMode matches the expected mode.
- `fallbackAccuracy`: fallbackUsed matches the case expectation.
- `emptyResultAccuracy`: empty-result behavior matches the case expectation.
- `citationCompletenessRate`: non-empty evidence includes policyId or documentId/chunkId traceability.
- `safetyPassRate`: evidence, messages, and failures avoid forbidden completed-action and sensitive text.
- `averageEvidenceCount`: average number of returned evidence items across all cases.

V2.9 evaluation and V4.6.1 evaluation are deliberately separate:

- V2.9 evaluates Agent planner output, subtask planning, tool planning, risk level, and plan validity.
- V4.6.1 evaluates policy evidence retrieval quality for the RAG search application boundary.

The V4.6.1 runner uses `FakeEmbeddingClient`, `InMemoryPolicyVectorRepository`, and in-memory keyword policy data. It
does not use LLM-as-judge, does not call a real LLM, does not call a real embedding provider, does not call Spring AI,
does not connect PostgreSQL / PGvector, and does not require Docker, MySQL, Redis, API keys, raw datasets, or external
network.

### V4 RAG Demo Scenario D

V4.6.2 links this retrieval evaluation path from `docs/demo/V4_RAG_DEMO_SCRIPT.md` as Scenario D. Run the demo
evaluation check with:

```bash
mvn test -Dtest=RagEvaluationApplicationServiceTest
```

The expected report highlights:

- `passRate`
- `evidenceRecallPassRate`
- `citationCompletenessRate`
- `safetyPassRate`
- `fallbackAccuracy`

This is a deterministic retrieval evaluation, not LLM-as-judge. It does not call real providers and does not create
Ticket, AgentRun, ToolCallTrace, Workspace, or Execution Tree state.

V4.6.2 completed the V4 RAG demo script. V4.6.3 remains Actuator health indicator work. V4.6.4 remains OpenAPI / API
documentation polish.

## RAG Retrieval Quality Improvement Roadmap

Project review correction stage 5 records the RAG quality improvement decision in
`docs/decisions/DECISION_PROJECT_REVIEW_RAG_QUALITY_IMPROVEMENT.md`.

The current RAG retrieval baseline remains KEYWORD / VECTOR / HYBRID policy evidence retrieval. Stage 5 does not
modify runtime retrieval algorithms, does not expand the RAG evaluation runner, and does not implement reranking,
query rewriting, RRF, or chunk window expansion.

Future RAG quality changes should be evaluation-driven:

- expand deterministic JSONL cases before changing ranking logic;
- classify failures as query mismatch, ranking issue, snippet context issue, fallback issue, or source coverage issue;
- keep no LLM-as-judge by default;
- require fake / deterministic tests for reranking, query rewriting, RRF, and chunk window expansion candidates;
- keep real reranker, real embedding provider, live PGvector, and Spring AI VectorStore paths explicit opt-in.

RAG evaluation remains retrieval evaluation only. RAG evidence is evidence-only policy evidence and must not be treated
as business decision confidence or as completed refund, exchange, coupon compensation, payment, logistics, inventory,
or dispute-closure action.

## V4 RAG / Skill Evaluation Extension

V4 evaluation extends the existing deterministic evaluation runner with RAG and Skill checks.

New metrics:

```text
policyEvidenceRecallAccuracy
ragCitationCompleteness
unsupportedQueryNoFabricationRate
skillSelectionAccuracy
skillExecutionBoundaryPassRate
hybridRetrievalMergeCorrectness
workspaceEvidenceCompleteness
executionTreeEvidenceCompleteness
```

Default runner must use fake embedding and fake vector repository. It must not call a real LLM, Spring AI provider, PostgreSQL, PGvector, Docker, MySQL, Redis, or external network.

Evaluation failures must include caseId, field, expected, actual, and message. RAG failures should also include retrievalMode, returnedChunkIds, returnedCategories, and fallbackUsed.
