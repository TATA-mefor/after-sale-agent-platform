# EXEC_PLAN_V2_POLICY_RETRIEVAL_TOOL

Date: 2026-05-16

## Scope

This record closes V2.5: Policy Retrieval Tool.

The task upgrades `search_aftersale_policy` into a controlled, structured, LOW-risk policy retrieval tool. It does not
add real VectorStore, PGvector, embedding calls, network search, real LLM dependency, real refunds, real exchanges,
coupon compensation, payment mutation, logistics mutation, or real database integration.

## What Changed

- Added `PolicySearchQuery`.
- Added `PolicySnippet`.
- Added `PolicySearchResult`.
- Extended `PolicyRepository` with controlled search.
- Updated `InMemoryPolicyRepository` to perform local keyword retrieval.
- Updated `PolicyApplicationService` to delegate structured queries to the repository.
- Replaced the previous policy tool class with `SearchAfterSalePolicyToolExecutor`.
- Kept the public tool name as `search_aftersale_policy`.
- Kept the tool risk level as `LOW`.
- Ensured unsupported queries return `results: []` with a clear message.
- Updated specialist handler tool planning so policy retrieval runs before action tools such as `add_ticket_note`.
- Added tests for return, exchange, refund, empty query behavior, ToolRegistry execution, and handler ordering.

## Policy Retrieval Flow

```text
SpecialistAgentHandler
→ ToolRegistry
→ SearchAfterSalePolicyToolExecutor
→ PolicyApplicationService
→ PolicyRepository
→ InMemoryPolicyRepository
→ PolicySearchResult
→ ToolCallTrace
```

## Test Coverage

Covered:

- Return policy keyword retrieval.
- Exchange policy keyword retrieval.
- Refund policy keyword retrieval.
- ToolRegistry execution of `search_aftersale_policy`.
- LOW-risk tool definition for policy retrieval.
- Structured empty result for unsupported policy query.
- Handler reorders policy retrieval before action tools.
- Existing V2.4 single-intent and multi-intent flows.
- Default offline test path without real LLM, API key, PGvector, or network.

## Known Limitations

- Retrieval is deterministic in-memory keyword matching.
- No VectorStore, PGvector, embeddings, reranking, or hybrid search.
- Empty policy results are surfaced, but no human escalation workflow is added in this task.
- Policy snippets are not persisted as a separate execution-tree node yet.

## Validation

Required commands:

```bash
mvn test
mvn checkstyle:check
mvn spotbugs:check
mvn test -Dtest=ArchitectureTest
```

Final results are recorded in the Review Packet for this task.

## Completion Signal

TASK_COMPLETE
