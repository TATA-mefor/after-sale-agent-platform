# AfterSale-Agent Platform

[![CI](https://github.com/TATAme/after-sale-agent-platform/actions/workflows/ci.yml/badge.svg)](https://github.com/TATAme/after-sale-agent-platform/actions/workflows/ci.yml)
[![Java 17](https://img.shields.io/badge/Java-17-blue)](https://adoptium.net/)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

AfterSale-Agent 是一个基于 Java Spring Boot 的智能电商售后工单 Agent 平台，面向面试展示、
架构评审和工程实践，而非直连生产电商系统。

## Project Overview

当用户提出售后诉求时，系统能够创建售后工单，由 Agent 读取订单信息、检索售后政策、
规划处理步骤、调用业务工具、生成处理建议，并在高风险动作前进入人工确认流程。

核心闭环：

```text
用户售后消息 → 工单创建 → Agent 规划 → 政策检索 → 低风险工具调用 → 执行轨迹 → 处理建议
```

项目采用模块化单体架构，以 Harness Engineering 文档、ArchUnit 架构测试、Checkstyle / SpotBugs
代码检查和 JUnit 测试作为工程护栏。

> **当前状态**：V4 完成 Agent / RAG / Tool / Skill / Spring AI / PGvector 基础。
> V5.A 完成 RAG production path foundation。V5.B Production Hardening current planned
> scope completed（容器、CI、配置、migration、可观测性、API Key 认证、K8s / Helm、
> Release / Rollback 治理）。真实生产部署、registry push、release/rollback 自动化、
> 生产级 IAM、生产监控和外部业务集成仍未完成。
>
> 详细版本路线请见 [部署加固路线图](docs/deploy/DEPLOYMENT_HARDENING_ROADMAP.md) 和
> [项目整改方案](docs/quality/PROJECT_REMEDIATION_PLAN.md)。

## Core Capabilities

- 创建和查询售后工单，支持分页和状态过滤。
- 触发确定性 AgentRun 执行，支持单意图和多意图售后任务规划。
- 通过 ToolRegistry 查询演示订单数据、检索售后政策证据。
- Specialist Handler 分派退货、换货、优惠券、物流、普通咨询和人工升级子任务。
- ToolCallTrace 审计每次工具调用。
- 高风险动作进入 Approval 人工确认流程。
- 只读 Execution Tree 展示 AgentRun 执行结构。
- 离线确定性的 RAG 策略检索评估与 Agent 规划评测。
- 默认使用内存仓储离线运行；显式 `mysql` profile 支持持久化。
- Docker Compose 本地 app + MySQL 开发环境。
- `X-Correlation-Id` / `X-Request-Id` HTTP 日志关联。
- Actuator 健康探针（readiness / liveness）。
- Micrometer 低基数业务指标。
- Prometheus opt-in 暴露。
- Spring Security API Key 认证（opt-in `security-api-key` profile）。
- Kubernetes manifest 模板和 Helm chart 骨架。
- OpenAPI / Swagger UI 本地 API 文档。

## Tech Stack

- Java 17
- Spring Boot 3.3.x
- Maven
- JUnit 5 / ArchUnit / Checkstyle / SpotBugs
- Spring AI（ChatClient / EmbeddingModel adapter）
- Spring Security（API Key auth）
- Spring Boot Actuator / Micrometer / Prometheus
- Flyway（默认关闭）
- MySQL / PostgreSQL + PGvector（显式 opt-in profile）
- Docker / Docker Compose（本地开发）
- Kubernetes / Helm（部署模板基础）

## Quick Start

```bash
mvn spring-boot:run
```

默认本地启动使用内存仓储，不需要 MySQL、Docker、Redis、真实 LLM、API Key 或外部网络。

```bash
curl http://localhost:8080/api/health
curl http://localhost:8080/actuator/health
curl http://localhost:8080/v3/api-docs
```

Swagger UI: `http://localhost:8080/swagger-ui/index.html`

## Validate

```bash
mvn test
mvn checkstyle:check
mvn spotbugs:check
mvn test -Dtest=ArchitectureTest
```

默认验证离线、确定性，不依赖真实 LLM、API Key、PostgreSQL、PGvector、Docker、MySQL、
Redis、真实 embedding provider、Spring AI live call 或外部网络。

## Key Documentation

| 文档 | 说明 |
| --- | --- |
| [SPEC.md](SPEC.md) | 项目目标与边界 |
| [ARCHITECTURE.md](ARCHITECTURE.md) | 架构分层与依赖规则 |
| [WORKFLOW.md](WORKFLOW.md) | 任务执行流程 |
| [AGENTS.md](AGENTS.md) | 智能体入口导航 |
| [OpenAPI / Swagger](docs/api/OPENAPI.md) | API 文档 |
| [Observability](docs/OBSERVABILITY.md) | 可观测性总览 |
| [Project Remediation Plan](docs/quality/PROJECT_REMEDIATION_PLAN.md) | 项目整改方案 |
| [Deployment Hardening Roadmap](docs/deploy/DEPLOYMENT_HARDENING_ROADMAP.md) | 部署加固路线图 |
| [Production Hardening Summary](docs/deploy/PRODUCTION_HARDENING_COMPLETION_SUMMARY.md) | V5.B 完成总结 |
| [V5.B Completion Record](docs/exec-plans/completed/EXEC_PLAN_V5_B_PRODUCTION_HARDENING_COMPLETION.md) | V5.B 完成记录 |
| [Production Config Template](docs/deploy/PRODUCTION_CONFIG_TEMPLATE.md) | 生产配置模板 |
| [Container + CI Hardening](docs/deploy/CONTAINER_CI_HARDENING.md) | 容器与 CI 基础 |
| [Auth / RBAC Boundary](docs/deploy/AUTH_RBAC_BOUNDARY.md) | 认证授权边界 |
| [Auth Runtime Foundation](docs/deploy/AUTH_RUNTIME_FOUNDATION.md) | API Key 认证 |
| [K8s / Helm Foundation](docs/deploy/K8S_HELM_FOUNDATION.md) | K8s 部署模板 |
| [K8s Manifests](deploy/k8s/README.md) | K8s manifest |
| [Helm Chart](deploy/helm/after-sale-agent-platform/README.md) | Helm chart |
| [Release / Rollback Foundation](docs/deploy/RELEASE_ROLLBACK_FOUNDATION.md) | 发布与回滚治理 |
| [Release Checklist](docs/deploy/release-templates/RELEASE_CHECKLIST_TEMPLATE.md) | 发布检查清单 |
| [Rollback Checklist](docs/deploy/release-templates/ROLLBACK_CHECKLIST_TEMPLATE.md) | 回滚检查清单 |
| [Change Record Template](docs/deploy/release-templates/CHANGE_RECORD_TEMPLATE.md) | 变更记录模板 |
| [Validation Commands](docs/quality/VALIDATION_COMMANDS.md) | 验证命令全集 |
| [Quality Score](docs/quality/QUALITY_SCORE.md) | 质量评分 |
| [V4 完整口径说明](version-updates/V4_FACTS.md) | V4 completed 边界 |
| [面试演示指南](docs/demo/DEMO_INTERVIEW_GUIDE.md) | Interview Guide |
| [Demo Walkthrough](docs/demo/DEMO_WALKTHROUGH.md) | 演示流程 |
| [MySQL Profile](docs/deploy/MYSQL_PROFILE.md) | MySQL profile |
| [Docker Compose](docs/deploy/DOCKER_COMPOSE.md) | 本地 compose |

## Known Limitations

- 默认运行时使用内存仓储，重启后数据重置。
- MySQL 持久化需显式启用 `mysql` profile。
- Docker Compose 是本地开发环境，不是生产部署。
- 默认 Agent Planner 是确定性规则降级；真实 LLM 模式需显式 opt-in。
- 未接入真实退款、换货、优惠券补偿、支付、库存、物流或争议关闭系统。
- Approval API 记录人工决策，但不执行真实高风险业务动作。
- 生产级 IAM（OAuth2 / OIDC、JWT、用户数据库）仍未实现。
- 生产监控、OpenTelemetry、分布式追踪仍未实现。
- Release / rollback 治理基础已完成，自动化仍未实现。
- K8s / Helm 是部署模板基础，尚未执行真实集群部署。
- 真实 API Key、数据库密码、token 和本地绝对路径不得进入仓库。
