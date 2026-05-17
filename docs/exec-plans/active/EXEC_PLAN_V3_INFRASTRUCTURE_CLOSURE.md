# EXEC_PLAN_V3_INFRASTRUCTURE_CLOSURE

Date: 2026-05-17
Status: Active

## Current Stage

V3 进入 Infrastructure Closure 阶段。当前任务只建立 V3 Harness 文档，不实现 Java 代码、不新增 Maven 依赖、
不新增 `docker-compose.yml`、不接 MySQL。

## First Priority

V3.1 MySQL Persistence 是第一优先级。

原因：

- Ticket、AgentRun、ToolCallTrace 和 ApprovalRequest 是系统审计与执行状态的核心数据；
- Docker Compose 需要先有明确的 MySQL profile、schema 和 seed 策略；
- Observability 需要稳定的业务 ID 和持久化 ID 作为日志关联字段；
- in-memory/test profile 必须在引入 MySQL 前先明确保留边界。

## Execution Order

1. 建立 `EXEC_PLAN_V3.md`，明确 V3.1 到 V3.4 的目标、范围、非目标、验收标准和验证命令。
2. 新增 V3 infrastructure closure 决策日志。
3. 更新 `SPEC.md`，补充 V3 基础设施目标和非目标。
4. 更新 `ARCHITECTURE.md`，补充 MySQL repository、profile、infrastructure 和 Docker Compose 边界。
5. 更新 `AGENTS.md` 和 `WORKFLOW.md`，补充 persistence / external infrastructure 工作规则。
6. 更新 `README.md`，加入 V3 roadmap，保持计划状态，不写成已完成。
7. 更新 `docs/quality/QUALITY_SCORE.md`，加入 V3 质量目标。
8. 运行默认验证命令。
9. 输出 Review Packet 和完成信号。

## Risks

- 文档可能把 V3 计划误写成已实现能力；
- 后续 persistence 实现可能让默认测试意外依赖本地 MySQL；
- MySQL profile 和 in-memory profile 可能出现行为不一致；
- seed data 可能与现有 in-memory demo 数据漂移；
- Docker Compose 可能被误解为生产部署；
- structured logging 可能输出敏感配置或过长 LLM 内容；
- persistence 实现可能绕过 ApplicationService 或破坏 Agent/Handler 边界。

## Validation Commands

```bash
mvn test
mvn checkstyle:check
mvn spotbugs:check
mvn test -Dtest=ArchitectureTest
```

## Completion Signal

TASK_COMPLETE
