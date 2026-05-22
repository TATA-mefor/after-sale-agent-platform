# EXEC_PLAN_ENGINEERING_COMMENTS

## Goal

Add selective Chinese Javadocs and local comments to clarify engineering boundaries in the current Java backend without changing behavior.

## Scope

- Agent orchestration, planning, specialist handlers, workspace, and execution tree.
- LLM planner, provider abstraction, prompt budgeting, and parser boundaries.
- Tool execution, trace recording, approval orchestration, observability, and persistence profile classes.
- Focused test classes where comments explain opt-in, architecture, or risk-boundary intent.

## Non-goals

- No business logic changes.
- No broad mechanical comments for getters, setters, constructors, DTOs, or simple branches.
- No new dependencies, secrets, prompt text, credentials, or historical background in comments.

## Steps

1. Inspect target packages and current uncommitted files.
2. Add class-level Javadocs to boundary-bearing classes.
3. Add method-level Javadocs only to public or business-critical methods.
4. Add sparse inline comments for non-obvious execution, approval, trace, prompt-budget, and persistence-profile choices.
5. Run required validation commands and fix comment formatting issues if any fail.

## Validation

Run:

```bash
mvn test
mvn checkstyle:check
mvn spotbugs:check
mvn test -Dtest=ArchitectureTest
```
