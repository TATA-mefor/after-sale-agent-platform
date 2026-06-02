# EXEC_PLAN_V3_DASHSCOPE_QWEN_PROVIDER

Status: completed

## Goal

Add DashScope / Qwen LLM provider support for the existing `LlmAgentPlanner` while preserving the OpenAI Responses
provider and keeping default tests offline and deterministic.

## What Changed

- Added provider selection for `openai-responses`, `dashscope-responses`, and `dashscope-chat-compatible`.
- Kept the legacy `openai` provider value as an alias for `openai-responses`.
- Added DashScope configuration for API key, base URL, responses endpoint, and chat completions endpoint.
- Added a provider-aware `LlmClientFactory`.
- Reused the Responses client for OpenAI Responses and DashScope Responses-compatible calls.
- Added a Chat Completions compatible client for DashScope compatible mode.
- Added sanitized provider error summaries with provider, endpoint host, model, status code, and truncated response body.
- Updated live smoke and HTTP live validation checks so DashScope runs are skipped unless `DASHSCOPE_API_KEY` is present.
- Updated README, live validation docs, LLM planner contract, and quality notes.

## Provider Flow

```text
agent.planner.mode=llm
-> agent.planner.llm.provider
-> LlmClientFactory
-> OpenAiLlmClient for responses providers
-> ChatCompletionsLlmClient for dashscope-chat-compatible
-> LlmAgentPlanner
-> AgentPlanParser
-> AgentPlanValidator
-> ToolRegistry execution by Java backend
```

## Configuration

OpenAI Responses:

```text
AFTERSALE_LLM_PROVIDER=openai-responses
OPENAI_API_KEY=...
OPENAI_RESPONSES_ENDPOINT=https://api.openai.com/v1/responses
AFTERSALE_LLM_MODEL=gpt-4.1-mini
```

DashScope Chat Completions compatible:

```text
AFTERSALE_LLM_PROVIDER=dashscope-chat-compatible
DASHSCOPE_API_KEY=...
DASHSCOPE_BASE_URL=https://dashscope.aliyuncs.com/api/v2/apps/protocols/compatible-mode/v1
AFTERSALE_LLM_MODEL=qwen3.6-plus
```

DashScope Responses compatible:

```text
AFTERSALE_LLM_PROVIDER=dashscope-responses
DASHSCOPE_API_KEY=...
DASHSCOPE_BASE_URL=https://dashscope.aliyuncs.com/api/v2/apps/protocols/compatible-mode/v1
AFTERSALE_LLM_MODEL=qwen3.6-plus
```

## Validation

Required default validation:

```bash
mvn test
mvn checkstyle:check
mvn spotbugs:check
mvn test -Dtest=ArchitectureTest
```

Optional live smoke:

```bash
mvn test -Dtest=LlmPlannerLiveSmokeTest -Dlive.llm=true
```

Optional full live validation:

```bash
mvn test -Dtest=RealAgentValidationLiveTest -Dlive.llm=true -Dlive.mysql=true
```

## Boundaries

- Default `mvn test` does not call OpenAI, DashScope, MySQL, Docker, or any external network.
- No real API keys, database passwords, or personal paths are committed.
- LLM providers only produce `AgentPlan` text.
- Java backend still parses, validates, and executes tools through `ToolRegistry`.
- Provider errors do not log API keys or full prompt text.

## Known Limitations

- DashScope model and endpoint compatibility is provider-specific. If `qwen3.6-plus` is rejected by a selected endpoint,
  use the endpoint required by DashScope for that model or try a compatible model such as `qwen-plus`.
- Provider usage token fields are still not treated as authoritative unless a client explicitly exposes safe usage data.
- The live validation path is intentionally opt-in and depends on local provider and MySQL setup.

## Completion Signal

TASK_COMPLETE
