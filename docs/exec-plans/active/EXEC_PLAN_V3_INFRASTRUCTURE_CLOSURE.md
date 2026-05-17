# EXEC_PLAN_V3_INFRASTRUCTURE_CLOSURE

Date: 2026-05-17
Status: Active

## Current Stage

V3 进入 Infrastructure Closure 阶段。V3.1 MySQL Persistence 已完成实现；当前活动计划继续跟踪 V3.2 Docker
Compose、V3.3 Observability 和 V3.4 Final Review。

## First Priority

V3.2 Docker Compose 是下一优先级。

原因：

- V3.1 已提供明确的 MySQL profile、schema 和 seed 策略；
- Docker Compose 可以在此基础上提供本地 app + mysql 一键启动路径；
- 默认 Maven 测试仍必须保留 in-memory 离线路径；
- Docker Compose 只能代表本地开发环境，不代表生产部署。

## Execution Order

1. V3.1 MySQL Persistence：completed。
2. V3.2 Docker Compose：新增本地 app + mysql compose 配置和 README 启动说明。
3. V3.3 Observability：新增 requestId、ticketId、agentRunId、subtaskId、toolName、approvalRequestId 等结构化日志字段。
4. V3.4 Final Review：复盘系统能力、限制、demo flow 和后续方向。
5. 每个阶段运行默认验证命令。
6. 每个阶段输出 Review Packet 和完成信号。

## Risks

- 文档可能把 V3 计划误写成已实现能力；
- 后续 Docker Compose 任务可能让默认测试意外依赖 Docker 或本地 MySQL；
- MySQL profile 和 in-memory profile 可能随新功能出现行为不一致；
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
