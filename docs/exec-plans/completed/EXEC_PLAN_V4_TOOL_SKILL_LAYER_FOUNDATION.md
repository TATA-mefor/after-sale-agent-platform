# V4.1 Tool / Skill Layer Foundation Completion Record

Date: 2026-05-22

Status: Completed

## Goal

Establish Skill as a first-class Agent platform concept without changing the current AgentRun business semantics.

V4.1 is a foundation task. It does not implement Spring AI, RAG, PGvector, VectorStore, Policy Ingestion, LangChain
sidecar, or full SkillRegistry runtime migration.

## Scope Completed

- Added the Java Skill contract model.
- Added a read-only SkillRegistry.
- Added Specialist Handler compatibility through a lightweight Skill adapter.
- Added Skill risk validation based on required tool risk.
- Added tests for registry lookup, duplicate skill names, expected definitions, adapter compatibility, and high-risk
  behavior.
- Added architecture rules for Skill boundaries.
- Updated V4 roadmap, Tool / Skill contracts, risk policy, LLM planner boundary, README, and quality score.

## What Changed

New Skill foundation classes:

```text
AgentSkill
SkillDefinition
SkillExecutionContext
SkillExecutionResult
SkillExecutionStatus
SkillExecutionException
SkillRegistry
SkillRiskEvaluator
SpecialistHandlerSkillAdapter
SpecialistSkillConfiguration
```

Registered Skill definitions:

```text
ReturnEligibilityAssessmentSkill
ExchangeRecommendationSkill
CouponConsultationSkill
LogisticsIssueAnalysisSkill
GeneralAfterSaleConsultationSkill
HumanApprovalRoutingSkill
```

## Tool vs Skill Boundary

Tool remains the atomic executable capability. ToolRegistry remains the only tool execution entry point.

Skill is a composite Agent capability. A Skill may coordinate existing handler behavior and tool evidence, but it must
not call repositories, vector stores, Spring AI clients, LLM infrastructure, or concrete tool executors directly.

## SkillRegistry Boundary

SkillRegistry indexes existing `AgentSkill` beans by unique `skillName` and by supported `SubtaskType`.

Duplicate `skillName` values fail during registry construction. Multiple Skill candidates for one `SubtaskType` are
returned as a deterministic list; V4.1 does not add implicit random selection or primary-skill arbitration.

SkillRegistry exposes Skill definitions for future planner context and execution-tree work, but it is not the AgentRun
runtime dispatcher in V4.1.

## Specialist Handler Compatibility

`SpecialistHandlerSkillAdapter` delegates to the existing `SpecialistAgentHandler` implementation.

This preserves:

- ToolRegistry execution;
- ToolCallTrace recording;
- AgentWorkspace writes;
- Approval handoff;
- existing `SubtaskExecutionResult` semantics.

`AgentApplicationService` still uses `SpecialistAgentHandlerRegistry` for current AgentRun execution.

## plannedSkills Boundary

`plannedSkills` remains documented as a future backward-compatible extension.

V4.1 does not change `AgentPlan`, `AgentPlanParser`, `AgentPlanValidator`, `RuleBasedAgentPlanner`, or
`LlmAgentPlanner` to generate, parse, validate, or execute `plannedSkills`.

## Skill Risk Boundary

`SkillRiskEvaluator` verifies that a Skill risk level is not lower than the highest risk level among its required tools.

High-risk Skill behavior remains approval-bound. V4.1 does not add real refund, real exchange, real coupon
compensation, real payment, real logistics, real inventory, or dispute-closing execution.

## Validation Commands

Required validation:

```bash
mvn test
mvn checkstyle:check
mvn spotbugs:check
mvn test -Dtest=ArchitectureTest
```

Default validation must remain offline and deterministic. It must not require real LLMs, API keys, PostgreSQL,
PGvector, Docker, MySQL, Redis, or external network.

## Known Limitations

- AgentRun is not fully migrated to SkillRegistry execution.
- Execution Tree does not yet show Skill nodes.
- RAG evidence Skill is not implemented.
- `plannedSkills` is not enabled in runtime planning or validation.
- Spring AI and VectorStore integration remain later V4 phases.

## Follow-ups

- V4.2 Spring AI Adapter.
- V4.3 PGvector / VectorStore.
- V4.4 Policy Ingestion.
- V4.5 Hybrid RAG Policy Search Tool.
- Later Skill runtime migration and Execution Tree Skill node support.

## Completion Signal

TASK_COMPLETE
