# EXEC_PLAN_V4_PRE_FLIGHT_FIXES

Date: 2026-05-22

Status: Completed

## Goal

Complete V4.0 pre-flight fixes before starting Spring AI, RAG, VectorStore, PGvector, Policy Ingestion, or SkillRegistry
implementation.

## Scope Completed

- Aligned Planner-visible tools with the current AgentRun executable tool boundary.
- Reused the same allowed tool boundary in AgentPlan validation, Specialist Handler execution, and evaluation planning.
- Ensured AgentRun failures do not leave tickets indefinitely in `CREATED` or `AGENT_RUNNING`.
- Preserved human approval status during high-risk approval flows.
- Cleaned didactic or inaccurate comments while keeping boundary-oriented comments.
- Updated V4 execution plan and quality documentation.

## What Changed

- Added an AgentRun executable tool policy for the current supported tool set.
- Restricted AgentRun planning context to `get_order_by_id`, `search_aftersale_policy`, and `add_ticket_note`.
- Updated plan validation errors to clearly identify tools that are registered but not allowed for the current AgentRun.
- Added an execution guard in Specialist Handler shared behavior before calling ToolRegistry.
- Updated failure handling so early Planner/Validator failures and runtime failures write a Ticket `FAILED` state when
  the ticket is still owned by the AgentRun.
- Added focused tests for Planner tool visibility, disallowed tool validation, LLM catalog filtering, and failure state
  consistency.

## Agent Executable Tool Boundary

ToolRegistry remains the global registry and the only tool execution entry point. V4.0 does not change global tool
registration.

The current AgentRun execution path only exposes tools that it can map and execute safely:

- `get_order_by_id`
- `search_aftersale_policy`
- `add_ticket_note`

Registered tools such as `create_aftersale_ticket`, `update_ticket_status`, and `get_user_orders` remain available to
their existing contexts but are not shown to the AgentRun Planner until AgentRun has explicit input mapping and execution
support for them.

## AgentRun Failure State Boundary

If Planner execution, plan validation, or tool execution fails while the ticket is still `CREATED` or `AGENT_RUNNING`,
the Ticket is marked `FAILED` with the sanitized AgentRun failure summary.

`WAITING_HUMAN_APPROVAL` is not treated as an AgentRun failure. High-risk approval flows remain under the Approval
boundary.

## Comment Cleanup Boundary

V4.0 cleaned comments that explained basic Java or Spring syntax and kept comments that clarify:

- Agent orchestration boundaries;
- ToolRegistry as the tool execution boundary;
- Approval as the high-risk action boundary;
- ToolCallTrace and Workspace separation;
- LLM planning without direct execution;
- default offline test and profile boundaries.

## Validation Commands

```bash
mvn test
mvn checkstyle:check
mvn spotbugs:check
mvn test -Dtest=ArchitectureTest
```

## Known Limitations

- V4.0 does not implement Spring AI adapters.
- V4.0 does not implement RAG, VectorStore, PGvector, or policy ingestion.
- V4.0 does not implement SkillRegistry or AgentSkill execution.
- AgentRun executable tools remain intentionally small until V4 adds explicit Skill and RAG execution support.

## Follow-ups

- V4.1 can build Tool / Skill contracts on top of the executable tool policy without exposing unsupported tools.
- V4.2 can add Spring AI adapters without changing the rule that LLMs only produce AgentPlan output.
- V4.3+ can add vector-backed policy retrieval while keeping `search_aftersale_policy` behind ToolRegistry.

## Completion Signal

TASK_COMPLETE
