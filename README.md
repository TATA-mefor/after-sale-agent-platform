# AfterSale-Agent Platform

AfterSale-Agent is a Java Spring Boot platform for auditable e-commerce after-sale ticket handling with Agent execution traces.

## Project Overview

V1 proves a narrow enterprise backend loop:

```text
user after-sale message -> ticket -> rule-based AgentRun -> policy retrieval -> low-risk tool call -> trace -> suggestion
```

The project is intentionally built as a modular monolith with Harness Engineering documents, architecture tests, lint
checks, and executable tests as the guardrails.

## Tech Stack

- Java 17
- Spring Boot 3.3.x
- Maven
- JUnit 5
- ArchUnit
- Checkstyle
- SpotBugs
- In-memory repositories for V1 demo data

## Requirements

- Java 17+
- Maven 3.9+

## Run Locally

```bash
mvn spring-boot:run
```

Health checks:

```bash
curl http://localhost:8080/api/health
curl http://localhost:8080/actuator/health
```

## Demo Walkthrough

This walkthrough shows the V1 closed loop:

```text
create ticket -> trigger AgentRun -> query ticket -> query tool traces
```

Start the application:

```bash
mvn spring-boot:run
```

Check that the application is up:

```bash
curl http://localhost:8080/api/health
```

Expected result:

```json
{
  "status": "UP",
  "service": "after-sale-agent-platform"
}
```

Create an after-sale ticket:

```bash
curl -X POST http://localhost:8080/api/tickets \
  -H "Content-Type: application/json" \
  -d '{"userId":"U-1001","orderId":"O202605130001","message":"我买的耳机有质量问题，左耳没声音，想退货退款。"}'
```

Expected result:

```json
{
  "code": "SUCCESS",
  "message": "ok",
  "data": {
    "ticketId": "T-...",
    "status": "CREATED",
    "intentType": "UNKNOWN"
  }
}
```

Copy `data.ticketId`, then trigger the Agent:

```bash
curl -X POST http://localhost:8080/api/tickets/{ticketId}/agent-runs
```

Expected result:

```json
{
  "code": "SUCCESS",
  "message": "ok",
  "data": {
    "runId": "RUN-...",
    "status": "SUCCEEDED",
    "intent": "RETURN_AND_REFUND",
    "plan": "{...}",
    "finalSuggestion": "Intent RETURN_AND_REFUND identified...",
    "evidence": [
      "POL-QUALITY-RETURN-EXCHANGE: 质量问题退换货规则"
    ],
    "toolCalls": [
      "search_aftersale_policy",
      "add_ticket_note"
    ]
  }
}
```

Query the ticket again:

```bash
curl http://localhost:8080/api/tickets/{ticketId}
```

Expected result:

```json
{
  "code": "SUCCESS",
  "message": "ok",
  "data": {
    "ticketId": "T-...",
    "intentType": "RETURN_AND_REFUND",
    "status": "RESOLVED",
    "internalNote": "Intent RETURN_AND_REFUND identified...",
    "agentSuggestion": "Intent RETURN_AND_REFUND identified..."
  }
}
```

Copy `data.runId` from the AgentRun response, then query trace:

```bash
curl http://localhost:8080/api/agent-runs/{runId}/traces
```

Expected result:

```json
{
  "code": "SUCCESS",
  "message": "ok",
  "data": [
    {
      "runId": "RUN-...",
      "toolName": "search_aftersale_policy",
      "status": "SUCCEEDED",
      "inputJson": "{\"query\":\"...\"}",
      "outputJson": "{\"results\":[...]}"
    },
    {
      "runId": "RUN-...",
      "toolName": "add_ticket_note",
      "status": "SUCCEEDED",
      "inputJson": "{\"ticketId\":\"T-...\",\"note\":\"...\"}",
      "outputJson": "{\"ticketId\":\"T-...\",...}"
    }
  ]
}
```

API responses use a shared envelope:

```json
{
  "code": "SUCCESS",
  "message": "ok",
  "data": {}
}
```

## Validate

```bash
mvn test
mvn checkstyle:check
mvn spotbugs:check
mvn test -Dtest=ArchitectureTest
```

## V1 Capability Boundary

V1 includes:

- Ticket creation and ticket detail query APIs.
- Rule-based intent classification for the demo after-sale messages.
- In-memory after-sale policy retrieval.
- Tool boundary through `ToolRegistry`.
- Low-risk tool execution for `search_aftersale_policy` and `add_ticket_note`.
- `AgentRun` and `ToolCallTrace` records exposed through APIs.
- Architecture checks that prevent API/repository coupling, domain/Spring Web coupling, direct Agent repository access,
  direct tool repository access, and business-module dependency on Agent orchestration.

## V1 Does Not Do

- No real LLM provider.
- No real database, Redis, payment, refund, inventory, or logistics integration.
- No real order lookup tool in the final V1 implementation; the demo carries `orderId` on the ticket and focuses on
  policy evidence plus auditable tool execution.
- No complex frontend or production authentication.
- No multi-Agent orchestration.
- No automatic execution of high-risk actions.

Start with these project documents:

- `SPEC.md`
- `WORKFLOW.md`
- `AGENTS.md`
- `ARCHITECTURE.md`
- `EXEC_PLAN_V1.md`

## V2 Roadmap

V1 当前是规则型 AgentRun 闭环，已经支持：

```text
创建售后工单
触发 AgentRun
检索售后政策
写入工单备注
记录 ToolCallTrace
查询执行轨迹
```

V2 的第一优先级是接入真实 LLM Planner Adapter。

### V2.1 LLM Planner Adapter

目标：

```text
用户售后问题
→ Ticket
→ LLM Planner 生成结构化 AgentPlan
→ Java 后端校验 AgentPlan
→ ToolRegistry 执行工具
→ ToolCallTrace 记录轨迹
```

设计边界：

- LLM 只负责规划；
- Java 后端负责执行；
- ToolRegistry 仍然是唯一工具执行入口；
- 高风险动作仍然需要审批；
- 默认测试不依赖真实 LLM；
- API Key 只能来自环境变量或本地配置。

### Planner Mode Configuration

The application selects the Agent planner with:

```yaml
agent:
  planner:
    mode: rule
```

Supported modes:

- `rule`: default mode. Uses `RuleBasedAgentPlanner`, preserves V1 behavior, and requires no API Key.
- `fake`: deterministic test mode. Uses `FakeAgentPlanner` and does not call any external network.
- `llm`: LLM adapter mode. Requires an API Key from environment or local configuration.

Example local LLM configuration:

```yaml
agent:
  planner:
    mode: llm
    llm:
      provider: openai
      model: ${AFTERSALE_LLM_MODEL:gpt-4o-mini}
      api-key: ${OPENAI_API_KEY:}
      endpoint: ${OPENAI_RESPONSES_ENDPOINT:https://api.openai.com/v1/responses}
      timeout-seconds: 30
```

If `agent.planner.mode=llm` is selected without `agent.planner.llm.api-key` / `OPENAI_API_KEY`, startup fails with a
clear configuration error. The default `mvn test` path uses `rule` or `fake` and does not require a real LLM, API Key,
or external network.

Current V2.1.1 status: the LLM adapter can call an OpenAI-compatible Responses endpoint when `llm` mode is explicitly
enabled and configuration is complete. The LLM must return structured JSON, which the Java backend parses, validates,
and then executes only through `ToolRegistry`. Tests still use `rule`, `fake`, or a fake `LlmClient`; they never require
a real LLM, API Key, or external network.

### LLM Planner Live Smoke Test

The live smoke test is explicit opt-in. It is not part of the default `mvn test` path.

To run it locally:

```bash
mvn test -Dtest=LlmPlannerLiveSmokeTest -Dlive.llm=true
```

Required local environment:

```text
OPENAI_API_KEY
```

Optional local environment:

```text
AFTERSALE_LLM_MODEL
OPENAI_RESPONSES_ENDPOINT
```

If `-Dlive.llm=true` is omitted, the test is disabled. If `OPENAI_API_KEY` is missing, the live test is skipped with a
clear message. The smoke test calls only `LlmAgentPlanner` and validates the returned `AgentPlan`; it does not execute
business tools, create `AgentRun`, write `ToolCallTrace`, or mutate tickets.

### V2 后续方向

- Order Query Tools；
- MySQL Persistence；
- Approval APIs；
- Agent Evaluation Dataset；
- Vector or Hybrid Policy Retrieval；
- Docker Compose and Observability。

### 真实 LLM 本地运行说明

默认本地运行仍使用 `rule` 模式。若要手动启用真实 LLM Planner，请只在本机环境变量或本地未提交配置中设置：

```text
OPENAI_API_KEY
AFTERSALE_LLM_MODEL
OPENAI_RESPONSES_ENDPOINT
```

`AFTERSALE_LLM_MODEL` 和 `OPENAI_RESPONSES_ENDPOINT` 可选；未设置时分别使用 `gpt-4o-mini` 和 OpenAI Responses
API 默认 endpoint。不要将真实 API Key 写入代码、测试、README、docs、`application.yml` 或提交历史。
