## V2 Roadmap

V1 当前是规则型 AgentRun 闭环，已经支持：

```text
创建售后工单
触发 AgentRun
查询内存 demo 订单
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
      provider: ${AFTERSALE_LLM_PROVIDER:openai-responses}
      model: ${AFTERSALE_LLM_MODEL:gpt-4.1-mini}
      api-key: ${OPENAI_API_KEY:}
      endpoint: ${OPENAI_RESPONSES_ENDPOINT:https://api.openai.com/v1/responses}
      timeout-seconds: 30
      dashscope:
        api-key: ${DASHSCOPE_API_KEY:}
        base-url: ${DASHSCOPE_BASE_URL:https://dashscope.aliyuncs.com/api/v2/apps/protocols/compatible-mode/v1}
        responses-endpoint: ${DASHSCOPE_RESPONSES_ENDPOINT:}
        chat-completions-endpoint: ${DASHSCOPE_CHAT_COMPLETIONS_ENDPOINT:}
      budget:
        system-prompt-tokens: 2000
        history-tokens: 4000
        rag-context-tokens: 8000
        tool-catalog-tokens: 2000
        max-output-tokens: 1000
        total-input-tokens: 16000
```

Supported live providers:

- `openai-responses`: OpenAI Responses API. Uses `OPENAI_API_KEY` and `OPENAI_RESPONSES_ENDPOINT`.
- `dashscope-responses`: DashScope Responses-compatible endpoint. Uses `DASHSCOPE_API_KEY` and either
  `DASHSCOPE_RESPONSES_ENDPOINT` or `${DASHSCOPE_BASE_URL}/responses`.
- `dashscope-chat-compatible`: DashScope OpenAI-compatible Chat Completions endpoint. Uses `DASHSCOPE_API_KEY` and
  either `DASHSCOPE_CHAT_COMPLETIONS_ENDPOINT` or `${DASHSCOPE_BASE_URL}/chat/completions`.

If `agent.planner.mode=llm` is selected without the provider-specific API Key, startup fails with a clear configuration
error. The default `mvn test` path uses `rule` or `fake` and does not require a real LLM, API Key, or external network.

Current V2.1.1 status: the LLM adapter can call an OpenAI-compatible Responses endpoint when `llm` mode is explicitly
enabled and configuration is complete. The LLM must return structured JSON, which the Java backend parses, validates,
and then executes only through `ToolRegistry`. Tests still use `rule`, `fake`, or a fake `LlmClient`; they never require
a real LLM, API Key, or external network.

V3.10 adds DashScope / Qwen provider adapters without changing the planner boundary. The LLM still only returns an
`AgentPlan`; Java still validates it and executes tools through `ToolRegistry`.

DashScope Chat Completions compatible example for PowerShell:

```powershell
$env:AFTERSALE_LLM_PROVIDER="dashscope-chat-compatible"
# Set DASHSCOPE_API_KEY in your local shell before running.
$env:DASHSCOPE_BASE_URL="https://dashscope.aliyuncs.com/api/v2/apps/protocols/compatible-mode/v1"
$env:AFTERSALE_LLM_MODEL="qwen3.6-plus"
```

DashScope Responses compatible example for PowerShell:

```powershell
$env:AFTERSALE_LLM_PROVIDER="dashscope-responses"
# Set DASHSCOPE_API_KEY in your local shell before running.
$env:DASHSCOPE_BASE_URL="https://dashscope.aliyuncs.com/api/v2/apps/protocols/compatible-mode/v1"
$env:AFTERSALE_LLM_MODEL="qwen3.6-plus"
```

If a provider reports that a model does not match the selected endpoint, switch to the endpoint required by DashScope
for that model or try a compatible model such as `qwen-plus`. Provider errors are summarized with provider, endpoint
host, model, status code, and a sanitized response body; API keys and full prompts are not logged.

### Context Budget / Token Observability

V3.8 adds a deterministic prompt budget layer before `LlmAgentPlanner` calls an LLM provider. The planner prompt is
assembled from typed sections instead of one uncontrolled string.

Critical sections are never silently dropped:

- `systemInstructions`
- `outputSchema`
- `plannerContractSummary`
- `toolCatalogCompact`
- `riskPolicySummary`
- `ticketContext`

Optional sections can be reduced by budget policy:

- `conversationHistory`
- `ragContext`
- `examples`
- `debugHints`
- `extendedPolicyText`
- `nonEssentialDocs`

Overflow handling drops or compresses optional content first: debug hints, examples, conversation history, RAG context,
extended policy text, then non-critical ticket context fields. If the prompt still exceeds the configured input budget,
the planner returns a clear prompt-budget error instead of truncating the output schema, compact tool catalog, or risk
policy summary.

The tool catalog sent to the LLM is compact and planner-safe. It includes only tool name, risk level, required input
fields, and short purpose. Full tool contracts are not copied into the prompt.

Before each LLM request, the planner logs only estimated token telemetry and budget actions:

```text
systemPromptTokens
plannerContractTokens
toolCatalogTokens
ticketContextTokens
orderContextTokens
historyTokens
ragContextTokens
optionalTokensDropped
totalInputTokens
maxOutputTokens
budgetExceeded
budgetAction
```

The estimate is intentionally simple: `max(1, chars / 4)`. Logs do not contain full prompt text, API keys, database
passwords, credentials, or long raw documents. Provider output token usage is recorded as `unknown` unless the provider
client exposes a safe usage field.

### LLM Planner Live Smoke Test

The live smoke test is explicit opt-in. It is not part of the default `mvn test` path.

To run it locally:

```bash
mvn test -Dtest=LlmPlannerLiveSmokeTest -Dlive.llm=true
```

Required local environment:

```text
OPENAI_API_KEY for openai-responses, or DASHSCOPE_API_KEY for dashscope providers
```

Optional local environment:

```text
AFTERSALE_LLM_PROVIDER
AFTERSALE_LLM_MODEL
OPENAI_RESPONSES_ENDPOINT
DASHSCOPE_BASE_URL
DASHSCOPE_RESPONSES_ENDPOINT
DASHSCOPE_CHAT_COMPLETIONS_ENDPOINT
```

If `-Dlive.llm=true` is omitted, the test is disabled. If the selected provider API key is missing, the live test is
skipped with a clear message. The smoke test calls only `LlmAgentPlanner` and validates the returned `AgentPlan`; it
does not execute business tools, create `AgentRun`, write `ToolCallTrace`, or mutate tickets.

### Real LLM + MySQL Seed Data Opt-In Validation

V3.9 adds a separate live validation path for the full HTTP AgentRun chain. It is disabled by default and runs only when
both live flags and all required environment variables are present.

Run command:

```bash
mvn test -Dtest=RealAgentValidationLiveTest -Dlive.llm=true -Dlive.mysql=true
```

Required environment:

```text
OPENAI_API_KEY for openai-responses, or DASHSCOPE_API_KEY for dashscope providers
AFTERSALE_MYSQL_URL
AFTERSALE_MYSQL_USERNAME
AFTERSALE_MYSQL_PASSWORD
```

Optional environment:

```text
AFTERSALE_LLM_PROVIDER
OPENAI_RESPONSES_ENDPOINT
DASHSCOPE_BASE_URL
DASHSCOPE_RESPONSES_ENDPOINT
DASHSCOPE_CHAT_COMPLETIONS_ENDPOINT
AFTERSALE_LLM_MODEL
AFTERSALE_LIVE_ORDER_ID
```

If `AFTERSALE_LIVE_ORDER_ID` is not set, the test uses the documented seed order `O202605130001`. The test starts the
Spring application with the `mysql` profile and `agent.planner.mode=llm`, then drives the same HTTP APIs used by a
manual demo:

```text
POST /api/tickets
POST /api/tickets/{ticketId}/agent-runs
GET /api/agent-runs/{runId}/execution-tree
GET /api/agent-runs/{runId}/traces
```

The live validation checks that the real LLM planner produces a valid `AgentPlan`, the backend validates it, tools still
execute through `ToolRegistry`, MySQL seed order facts include `orderItems`, `ToolCallTrace` rows are written, and the
execution tree exposes item-level recommendation evidence. Missing flags or environment variables skip the test before
loading the MySQL/LLM application context. Provider 403, `INSUFFICIENT_BALANCE`, or insufficient-balance responses are
reported as provider/account-balance setup errors for the explicit live run. See
[docs/demo/REAL_AGENT_VALIDATION.md](docs/demo/REAL_AGENT_VALIDATION.md) for the full setup checklist.

### V2 后续方向

- MySQL Persistence；
- Vector or Hybrid Policy Retrieval；
- Docker Compose and Observability。

### V2.2 Order Query Tools

V2.2 adds two low-risk order tools backed by in-memory demo data:

- `get_order_by_id`
- `get_user_orders`

The rule-based AgentRun now plans `get_order_by_id` before policy retrieval, so the final suggestion and trace can show
both order facts and policy evidence. This is still demo data only; the project does not connect to a real order center,
real logistics provider, real payment provider, or production order database.

V3.6 enriches `get_order_by_id` with structured `orderItems`. Each item includes:

- `orderItemId`
- `productId`
- `productName`
- `category`
- `quantity`
- `unitPrice`
- `itemStatus`
- `supportReturn`
- `supportExchange`
- `isSpecialItem`

`get_user_orders` may include the same structured item list because it uses the shared order tool output mapper, but it
remains intended as a lightweight user-order lookup. ToolCallTrace and Execution Tree output can inspect the serialized
`orderItems` through the `get_order_by_id` `outputJson`.

V3.7 uses the same `orderItems` evidence inside Return and Exchange specialist handlers. The handlers still obtain order
data only through `ToolRegistry`, then write item-level recommendations into the final summary and Ticket note. A
recommendation includes the selected `orderItemId`, `productId`, `productName`, `category`, support flags, special-item
flag, recommendation text, and reason.

Current deterministic matching rules are intentionally simple:

- Prefer an item whose `productName` appears in the subtask target or user message.
- Otherwise match by `category`.
- Treat common clothing words such as `裙子`, `衣服`, `上衣`, `裤子`, `服装`, and `尺码` as coarse clothing signals.
- If no item matches, fall back to the first item and state that fallback in the reason.

Unsupported or special items do not produce a direct return/exchange recommendation. The handler recommends policy or
manual-review handling instead and does not execute real refund, exchange, inventory, logistics, or payment actions.

### V2.3 Multi-Intent Planning

V2.3 adds Multi-Intent Planning while preserving the V2.2 single-intent flow.

Implemented flow:

```text
complex after-sale message
→ RuleBasedAgentPlanner / LLM AgentPlan with subtasks
→ structured AgentSubtask list
→ Java validation
→ sequential ToolRegistry execution
→ ToolCallTrace with subtask metadata
→ summarized final suggestion
```

Example user message:

```text
我买了三件衣服，其中一件有污渍要退货，另一件要换尺码，还有一张优惠券没用上怎么退？
```

Expected subtask types:

- `RETURN`
- `EXCHANGE`
- `COUPON_CONSULTATION`

The rule-based planner detects this pattern and creates multiple subtasks. Each subtask uses ToolRegistry tools in
priority order, and trace `inputJson` includes subtask metadata such as `subtaskId` and `subtaskType`.

V2.3 remains a single-process planning and execution design. It does not add multi-Agent microservices, queues, parallel
execution, voting consensus, a full coupon system, real refunds, real exchanges, real logistics, or real payment
integration. V2.4 adds handler-based dispatch on top of this subtask model, and V2.8 exposes the resulting execution
tree as a read-only API.

### V2.4 Specialist Agent Handler

V2.4 adds Specialist Agent Handler dispatch for multi-intent subtasks while preserving the V2.2 single-intent flow and
the V2.3 sequential execution model.

Implemented flow:

```text
AgentPlan with subtasks
→ AgentApplicationService
→ SpecialistAgentHandlerRegistry
→ SpecialistAgentHandler
→ ToolRegistry
→ ToolCallTrace
→ SubtaskExecutionResult
→ final summary
```

Implemented handlers:

- `ReturnAgentHandler`
- `ExchangeAgentHandler`
- `CouponAgentHandler`
- `LogisticsAgentHandler`
- `GeneralConsultationHandler`
- `HumanEscalationHandler`

Each handler declares the `SubtaskType` it supports, and `SpecialistAgentHandlerRegistry` rejects duplicate coverage.
Unsupported subtask types return a structured failed `SubtaskExecutionResult`. Handlers call only registered tools
through `ToolRegistry`, so `ToolCallTrace` continues to record handler-triggered calls.

V2.4 remains a modular-monolith strategy-class design. It does not add multi-Agent microservices, queues, parallel
execution, voting consensus, real refunds, real exchanges, real coupon compensation, real logistics, real payment
integration, or a real LLM dependency in default tests.

### V2.5 Policy Retrieval Tool

V2.5 completes controlled policy retrieval for handler execution. The public tool remains:

```text
search_aftersale_policy
```

Implemented boundary:

```text
SpecialistAgentHandler
→ ToolRegistry
→ SearchAfterSalePolicyToolExecutor
→ PolicyApplicationService
→ PolicyRepository
→ InMemoryPolicyRepository
```

The retrieval result is structured as policy snippets and empty matches return `results: []` with a clear message.
V2.5 does not add VectorStore, PGvector, embeddings, network search, real LLM dependency, real refunds, real exchanges,
coupon compensation, payment changes, logistics changes, or a real database.

### V2.6 Agent Workspace / Structured Memory

V2.6 adds single-`AgentRun` structured workspace memory.

Implemented flow:

```text
AgentRun creates AgentWorkspace
→ SpecialistAgentHandler reads workspace context
→ Handler executes tools through ToolRegistry
→ Handler writes order facts, policy evidence, tool summaries, subtask memory, and risk flags
→ final summary is assembled from workspace
```

Candidate models:

- `AgentWorkspace`
- `OrderFact`
- `PolicyEvidence`
- `SubtaskMemory`
- `ToolResultSummary`
- `RiskFlag`

V2.6 is scoped to one `AgentRun`. It is not long-term memory, user profiling, vector memory, cross-session memory,
Redis, MySQL, or a vector database. Workspace must not store API keys, sensitive credentials, full long prompts, or raw
long LLM outputs, and it must not replace `ToolCallTrace` or bypass `ToolRegistry`.

### V2.7 Approval APIs

V2.7 adds in-memory approval APIs for high-risk Agent decisions. Approval records are structured and queryable, but
approval does not execute real refunds, exchanges, coupon compensation, payment changes, or logistics changes.

Implemented APIs:

```bash
curl http://localhost:8080/api/approval-requests/pending

curl http://localhost:8080/api/approval-requests/{approvalRequestId}

curl -X POST http://localhost:8080/api/approval-requests/{approvalRequestId}/approve \
  -H "Content-Type: application/json" \
  -d '{"reviewerId":"operator-1","reason":"Evidence is sufficient for manual processing."}'

curl -X POST http://localhost:8080/api/approval-requests/{approvalRequestId}/reject \
  -H "Content-Type: application/json" \
  -d '{"reviewerId":"operator-1","reason":"Policy evidence is insufficient."}'
```

High-risk subtasks create `ApprovalRequest` records and move the ticket to `WAITING_HUMAN_APPROVAL`. Low-risk tools do
not create approval requests.

### V2.8 Execution Tree

V2.8 adds a read-only execution tree API for inspecting one `AgentRun` across plan, subtasks, tool traces, approval
requests, and workspace-derived summary.

Implemented API:

```bash
curl http://localhost:8080/api/agent-runs/{runId}/execution-tree
```

The response contains root AgentRun fields, root-level tool calls without `subtaskId`, subtask nodes with attached tool
calls, approval request nodes, errors, and timestamps. Tool calls are associated to subtasks through
`ToolCallTrace.inputJson.subtaskId` when present. Approval requests are associated through `runId` and `subtaskId` when
available.

This API is query-only. It does not modify tickets, AgentRun records, traces, approvals, refunds, exchanges, coupons,
payments, logistics, or inventory.

### V2.9 Evaluation Dataset

V2.9 adds an offline evaluation dataset and runner for the current after-sale Agent planner.

Dataset:

```text
docs/evaluation/aftersale_cases.jsonl
```

Evaluation guide:

```text
docs/evaluation/EVALUATION.md
```

Run the focused evaluation tests:

```bash
mvn test -Dtest=EvaluationApplicationServiceTest
```

The default runner uses `RuleBasedAgentPlanner`, validates every generated plan with `AgentPlanValidator`, and computes
intent, subtask, tool, risk, policy-category, approval-requirement, and plan-validity metrics. It does not call a real
LLM provider, require an API key, use LLM-as-judge, mutate tickets or approvals, or connect to external infrastructure.
