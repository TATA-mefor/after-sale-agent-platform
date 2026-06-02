## V3 Roadmap

V3 is the infrastructure closure phase. V3.1 MySQL Persistence and V3.2 Docker Compose are implemented for explicit
local infrastructure profiles. V3.3 Structured Logging / Observability is implemented for request correlation and
structured diagnostic fields. V3.4 Final Review is completed as the infrastructure closure review. V3.5 Demo Dataset
Enrichment is implemented for optional local seed generation. V3.6 Order Items Tool Enrichment is implemented so
existing order tools can expose product-level order detail. V3.7 Item-Specific Recommendation is implemented so Return
and Exchange specialist handlers can produce item-level suggestions from those tool results. V3.8 Context Budget /
Token Observability is implemented so real LLM planning can be introduced behind explicit budget and telemetry
controls. V3 does not change the Agent business capability boundary.

### V3.1 MySQL Persistence

Implemented focus:

- Persist Ticket, AgentRun, ToolCallTrace, and ApprovalRequest records.
- Seed order demo data and policy data.
- Add a local `mysql` profile.
- Keep in-memory/test profile for offline deterministic tests.
- Keep default `mvn test` independent from MySQL, Docker, Redis, real LLMs, API keys, and external network.
- Keep database credentials in environment variables or uncommitted local configuration only.

### V3.2 Docker Compose

Implemented focus:

- Add local app + mysql startup through Docker Compose.
- Build the app from the local Dockerfile.
- Initialize MySQL from `schema-mysql.sql` and `data-mysql.sql`.
- Run the app with the explicit `mysql` profile.
- Keep Redis out of the default compose environment.
- Manage local credentials through placeholder defaults and environment variable overrides.
- Do not commit real secrets.
- Treat Docker Compose as local development setup, not production deployment.

### V3.3 Structured Logging / Observability

Implemented focus:

- Add `X-Request-Id` request propagation and response header return.
- Add MDC-backed structured log fields such as requestId, ticketId, agentRunId, subtaskId, toolName, and
  approvalRequestId.
- Log key Ticket, AgentRun, Specialist Handler, ToolRegistry, Approval, and Execution Tree paths.
- Keep actuator health checks available.
- Do not replace ToolCallTrace or Execution Tree with logs.
- Do not introduce Prometheus/Grafana, ELK, OpenTelemetry, or external logging platforms.

### V3.4 Final Review

Completed focus:

- Review system capability list.
- Document known limitations.
- Verify demo flow and validation commands.
- Record follow-up directions without claiming unfinished capabilities as completed.

### V3.5 Demo Dataset Enrichment

Implemented focus:

- Add `products` and `order_items` MySQL tables for richer local demo data.
- Keep minimal product and order-item seed in `data-mysql.sql`.
- Keep raw downloaded datasets out of Git in a local gitignored raw dataset directory.
- Generate optional enrichment SQL and JSONL cases under `data/generated`.
- Document mapping from public order, Chinese review, and clothing feedback datasets.
- Keep default startup and default tests independent from external dataset files.

### V3.6 Order Items Tool Enrichment

Implemented focus:

- Extend the order domain and tool output with structured `orderItems`.
- Query `order_items` joined with `products` in the explicit MySQL profile.
- Keep default in-memory order demo data with at least one item per seeded order.
- Preserve Agent, Handler, ToolRegistry, Approval, Trace, and Workspace execution boundaries.
- Keep default tests independent from MySQL, Docker, raw datasets, real LLMs, API keys, and external network.

### V3.7 Item-Specific Recommendation

Implemented focus:

- Parse `get_order_by_id` `orderItems` into AgentWorkspace order facts.
- Generate item-level return recommendations in `ReturnAgentHandler`.
- Generate item-level exchange recommendations in `ExchangeAgentHandler`.
- Match items deterministically by product name, category, or coarse clothing keywords.
- Fall back to the first order item with an explicit reason when no specific item can be matched.
- Respect Java-derived `supportReturn`, `supportExchange`, and `isSpecialItem` flags.
- Keep Handler access to order data behind ToolRegistry and avoid real refund/exchange/inventory actions.

### V3.8 Context Budget / Token Observability

Implemented focus:

- Split LLM planner prompts into typed critical and optional sections.
- Apply a deterministic input-token budget using a simple `max(1, chars / 4)` estimate.
- Keep critical schema, compact tool catalog, risk policy summary, and ticket context from being silently dropped.
- Reduce optional context in a fixed order before returning a clear budget error.
- Send only a compact tool catalog to the planner prompt.
- Log estimated token telemetry and budget actions without logging full prompt text or secrets.
- Keep default tests independent from real LLMs, MySQL, Docker, API keys, and external network.

### V3.9 Real LLM + MySQL Seed Data Opt-In Validation

Implemented focus:

- Add `RealAgentValidationLiveTest` as an explicit live-only HTTP validation path.
- Require both `-Dlive.llm=true` and `-Dlive.mysql=true` before the test can run.
- Require LLM and MySQL environment variables before the Spring `mysql` profile context is loaded.
- Exercise Ticket creation, AgentRun creation, LLM planning, ToolRegistry execution, traces, and execution-tree query
  through HTTP APIs.
- Use the MySQL seed order `O202605130001` by default, with `AFTERSALE_LIVE_ORDER_ID` as a local override.
- Keep default tests independent from real LLMs, MySQL, Docker, API keys, and external network.
