# Decision: V3 Infrastructure Closure

Date: 2026-05-17
Status: Accepted

## Context

V2 已完成 Agent 平台的主要业务执行边界：Planner、LLM adapter、order tools、multi-intent planning、
specialist handler、policy retrieval、workspace、approval APIs、execution tree、evaluation dataset 和
rule-based robustness。当前系统仍主要依赖 in-memory repository，适合离线测试和本地 demo，但不适合验证重启后
数据保留、可复现环境和线上诊断路径。

如果 V3 继续增加 Agent 能力，会进一步扩大行为复杂度，但当前基础设施仍不足以支撑更复杂的运行、回归和审计。
因此 V3 需要先收口持久化、容器化和可观测性。

## Decision

V3 定位为 Infrastructure Closure，不继续堆叠新的 Agent 行为能力。

具体决策：

- V3.1 优先实现 MySQL Persistence，覆盖 Ticket、AgentRun、ToolCallTrace、ApprovalRequest、Order demo data 和
  Policy data；
- 保留 in-memory/test profile，默认测试仍然离线、确定性运行；
- Docker Compose 放在 MySQL profile 之后，因为 compose 应复用已经明确的 datasource、schema 和 seed 策略；
- Observability 先做 structured logging 和 actuator health，不先引入复杂 Prometheus / Grafana 平台；
- V3 继续保持模块化单体，不拆微服务；
- V3 不改变 Agent、ToolRegistry、Approval、Trace、Workspace 或 RiskPolicy 边界；
- V3 不实现真实退款、真实换货、真实优惠券补偿、真实支付或真实物流动作。

## Consequences

正向影响：

- 核心业务数据可以在本地 MySQL profile 下跨重启保留；
- in-memory/test profile 继续支撑快速离线测试；
- Docker Compose 可以基于已完成的 MySQL profile 提供更稳定的一键启动；
- structured logging 能提升 request、ticket、agentRun、subtask、tool 和 approval 维度的排查能力；
- 模块化单体保持开发和验证成本可控。

代价与约束：

- V3 不会显著提升 Agent 语义理解能力；
- MySQL 持久化会增加 repository、schema 和 profile 配置复杂度；
- Docker Compose 只解决本地开发复现，不代表生产部署；
- structured logging 只提供基础诊断能力，不提供完整监控告警平台；
- 保留 in-memory 和 MySQL 两套实现会增加 contract 测试维护成本。

## Alternatives Considered

1. 继续增强 Agent 能力。

   未采用。V2 已经覆盖主要 Agent 行为边界，继续叠加能力会让缺少持久化和可观测性的运行风险更高。

2. 先做 Docker Compose，再做 MySQL Persistence。

   未采用。compose 应承载明确的 app + mysql 运行形态；如果持久化 schema、profile 和 seed 策略未定，compose
   会变成空壳环境。

3. 默认测试直接依赖 MySQL 或 Testcontainers。

   未采用。项目要求默认 `mvn test` 可离线运行，不能强制依赖 Docker、本地 MySQL 或外部网络。Testcontainers
   可以作为 opt-in integration test。

4. 直接引入 Prometheus / Grafana / distributed tracing。

   未采用。当前阶段更需要稳定的 requestId 和业务 ID 结构化日志。复杂监控平台会扩大配置和运维面。

5. 拆分微服务。

   未采用。当前系统的核心价值是 Agent 执行边界、审计和 Harness 工程约束。模块化单体配合 ArchUnit 已能表达
   清晰架构边界，微服务会增加部署、事务和调试复杂度。
