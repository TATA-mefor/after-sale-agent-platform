# V4.6.2 V4 RAG Demo Script

Date: 2026-05-28

Status: Completed

## Goal

Add an offline V4 RAG demo script for interview and local project review use. The script demonstrates the path from an
after-sale ticket through HYBRID policy search evidence, ToolCallTrace, AgentWorkspace, Execution Tree, and deterministic
RAG evaluation.

## Scope Completed

- Added `docs/demo/V4_RAG_DEMO_SCRIPT.md`.
- Documented demo purpose, prerequisites, default offline path, safety boundaries, and startup command.
- Added scenarios for HYBRID policy search output shape, AgentRun with RAG evidence, Execution Tree evidence visibility,
  and RAG evaluation metrics.
- Added short expected output snippets for tool output, traces, workspace evidence, execution tree evidence, and
  evaluation metrics.
- Linked the demo from README and evaluation docs.
- Updated V4 roadmap and quality docs to mark V4.6.2 complete.
- Added docs harness tests for demo content, completion record, links, boundary wording, and secret / local-path safety.

## What Changed

V4.6.2 is documentation and docs-test work only. It gives reviewers a concise path to demonstrate V4 RAG evidence without
requiring live providers, PGvector, Docker, API keys, or new runtime endpoints.

## Demo Script Boundary

The demo script documents how to start the app, create a ticket, trigger AgentRun, inspect ToolCallTrace, inspect
Execution Tree, and run the RAG evaluation test. It includes a direct ToolRegistry input/output shape for
`search_aftersale_policy`, but it does not add a new HTTP endpoint or ToolRegistry tool.

## Offline Demo Boundary

The default demo path uses existing default offline behavior and deterministic fake / in-memory RAG dependencies where
RAG evaluation is involved. It does not require PostgreSQL, PGvector, Docker, MySQL, Redis, real LLMs, real embedding
providers, API keys, or external network.

## Evidence-only Demo Boundary

The demo states that `search_aftersale_policy` remains LOW-risk read-only and that RAG evidence is policy evidence only.
It does not execute or claim real refund, exchange, compensation, payment, logistics, inventory, coupon issuance, or
dispute closure.

## Evaluation Demo Boundary

Scenario D points to the V4.6.1 deterministic RAG evaluation dataset and runner. It explains the key metrics and keeps
LLM-as-judge, real provider calls, PGvector, and external network out of the default path.

## Default Test Boundary

The docs harness tests are file-read assertions only. They do not start Spring Boot, call HTTP endpoints, connect to a
database, start Docker, call LLMs, call embedding providers, or write ToolCallTrace / Workspace / Execution Tree state.

## Validation Commands

```bash
mvn test
mvn checkstyle:check
mvn spotbugs:check
mvn test -Dtest=ArchitectureTest
```

## Known Limitations

- V4.6.2 does not add runtime behavior.
- V4.6.2 does not add a direct public tool execution API.
- V4.6.2 does not change `search_aftersale_policy` retrieval behavior.
- V4.6.2 does not change the RAG evaluation runner.
- V4.6.2 does not implement Actuator health indicators.
- V4.6.2 does not implement OpenAPI / API docs polish.
- Live provider and PGvector demos remain opt-in and separately documented.

## Follow-ups

- V4.6.3 can add Actuator health indicators.
- V4.6.4 can polish OpenAPI / API docs.
- Future demo work can add a dedicated public debug endpoint only if a separate execution plan accepts that boundary.

## Completion Signal

TASK_COMPLETE
