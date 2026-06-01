# Project Review Correction Stage 6：部署加固路线

Date: 2026-06-01

Status: Completed

## Goal

把项目审查中的部署缺口转化为中文 deployment hardening decision / roadmap，明确当前 baseline、缺口、后续路线和
默认离线边界，同时不修改 runtime。

## Scope Completed

- 新增部署加固决策文档。
- 新增部署加固路线图。
- 更新 README、production config docs、整改方案、active correction plan、quality docs、validation docs 和 release
  summary。
- 新增只读 docs harness test。

## What Changed

- 记录当前已有 `docker-compose.yml`、`docker-compose-rag.yml`、`.env.rag.example`、
  `application-prod.example.yml`、`application-mysql.yml`、`application-rag-postgres.yml`、Actuator health、
  OpenAPI docs 和默认离线验证。
- 明确 Dockerfile、CI/CD、Kubernetes / Helm、secret manager、production auth/RBAC、production monitoring、
  live PGvector validation、`JdbcPolicyVectorRepository`、Flyway / Liquibase、readiness/liveness 独立策略、
  release/rollback checklist 和 production external integrations 仍未完成。

## Deployment Baseline Boundary

阶段 6 只描述现有本地开发 / opt-in profile / docs baseline。`docker-compose.yml` 和 `docker-compose-rag.yml`
继续是本地路径，不是生产部署方案。

## Dockerfile / CI Boundary

Dockerfile is not implemented。CI/CD is not implemented。阶段 6 只记录 Dockerfile hardening checklist 和 CI quality
gate strategy，不新增 Dockerfile、workflow 或部署自动化。

## Profile / Secret Boundary

`application-prod.example.yml` 是模板，不是默认生产配置。真实 secret 必须来自部署系统、外部配置中心、
secret manager 或未提交的环境变量。secret manager is not implemented。

## PGvector Deployment Boundary

PGvector 当前是 profile、schema、compose、repository contract 和 fake / in-memory 默认路径。
`JdbcPolicyVectorRepository` is not implemented。live PGvector validation is not completed。

## Readiness / Liveness Boundary

阶段 6 不新增 readiness/liveness endpoint，不改变 Actuator health behavior。后续 readiness / liveness 需要区分
进程健康、profile 依赖和 sanitized details。

## Production Hardening Boundary

production deployment is not completed。production auth/RBAC is not completed。production monitoring is not
completed。真实退款、换货、补偿、支付、物流和生产外部系统集成仍不是当前已完成能力。

## Runtime Non-change Boundary

阶段 6 不修改 `src/main/java` runtime/business code；本阶段实际也没有修改 `src/main/java`、
`src/main/resources`、`pom.xml`、Docker / Compose 文件、ToolRegistry、
`search_aftersale_policy` runtime、RAG runtime、ingestion pipeline、health indicators、OpenAPI config、
ToolCallTrace、Workspace、Execution Tree 或 AgentApplicationService。

## Default Offline Boundary

默认验证仍不需要 real LLM、API Key、PostgreSQL、PGvector、Docker、MySQL、Redis、external network、real embedding
provider、Spring AI live provider calls、secret manager、CI runner、Kubernetes / Helm、Prometheus、Grafana 或
OpenTelemetry collector。

## Validation Commands

```bash
mvn test -Dtest=DeploymentHardeningRoadmapDocsTest,RagQualityDecisionDocsTest,SpringAiDeepeningDecisionDocsTest,AsyncStreamingBatchApiDecisionDocsTest
mvn test
mvn checkstyle:check
mvn spotbugs:check
mvn test -Dtest=ArchitectureTest
```

## Known Limitations

- 阶段 6 不实现 Dockerfile hardening。
- 阶段 6 不实现 CI/CD pipeline。
- 阶段 6 不实现 Kubernetes / Helm。
- 阶段 6 不实现 secret manager。
- 阶段 6 不实现 production deployment、production auth/RBAC 或 production monitoring。
- 阶段 6 不实现 live PGvector validation 或 `JdbcPolicyVectorRepository`。

## Follow-ups

- 后续阶段或 V5：Dockerfile hardening。
- 后续阶段或 V5：CI/CD quality gate。
- 后续阶段或 V5：secret manager、production auth/RBAC、production monitoring。
- 后续阶段或 V5：database migration、PGvector deployment、release/rollback。

## Completion Signal

TASK_COMPLETE
