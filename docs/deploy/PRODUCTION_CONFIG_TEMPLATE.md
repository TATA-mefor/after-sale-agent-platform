# 生产配置模板说明

Date: 2026-06-01

Status: Completed

## 目的

本文件说明 `src/main/resources/application-prod.example.yml` 的使用边界。该模板用于把项目审查中指出的
“缺少生产配置模板”问题转化为可审查、可复制、不会泄露密钥的配置样例。

这是配置模板和文档说明，不是生产部署方案，也不代表生产认证、生产监控、CI/CD、Kubernetes、Helm、
secret manager、真实退款、真实换货、真实支付或真实物流已经接入。

部署加固路线见：

- `docs/decisions/DECISION_PROJECT_REVIEW_DEPLOYMENT_HARDENING.md`
- `docs/deploy/DEPLOYMENT_HARDENING_ROADMAP.md`

这些文档记录后续 Dockerfile、CI/CD、Kubernetes / Helm、secret manager、database migration、PGvector deployment、
readiness/liveness、observability、security/auth 和 release/rollback 路线；它们不表示 production deployment
已完成。

## 模板文件

模板路径：

```text
src/main/resources/application-prod.example.yml
```

该文件使用 `.example.yml` 后缀，默认不会被 Spring Boot profile 自动加载。需要真实环境使用时，应由部署系统、
未提交的本地配置或外部配置中心提供真实值，不要把真实配置写回仓库。

## 环境变量分组

### 基础服务

- `SERVER_PORT`
- `AFTERSALE_SHUTDOWN_TIMEOUT`
- `LOG_LEVEL_ROOT`
- `LOG_LEVEL_AFTERSALE`

### 数据库

- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `SPRING_DATASOURCE_DRIVER`
- `AFTERSALE_HIKARI_MAX_POOL_SIZE`
- `AFTERSALE_HIKARI_MIN_IDLE`

这些变量只是模板占位。默认验证不会读取生产数据库，也不会创建 live 连接。

### LLM / Spring AI

- `AFTERSALE_AGENT_PLANNER_MODE`
- `AFTERSALE_LLM_PROVIDER`
- `AFTERSALE_LLM_MODEL`
- `OPENAI_API_KEY`
- `OPENAI_RESPONSES_ENDPOINT`
- `DASHSCOPE_API_KEY`
- `DASHSCOPE_BASE_URL`
- `SPRING_AI_ENABLED`
- `SPRING_AI_CHAT_ENABLED`
- `SPRING_AI_EMBEDDING_ENABLED`
- `SPRING_AI_OPENAI_API_KEY`

真实 provider 仍是显式 opt-in 路径。默认 `mvn test` 不需要 API Key，不调用真实 LLM，也不调用真实
embedding provider。

### RAG / PGvector

- `AFTERSALE_RAG_ENABLED`
- `AFTERSALE_VECTOR_STORE_PROVIDER`
- `AFTERSALE_PGVECTOR_ENABLED`
- `AFTERSALE_PGVECTOR_URL`
- `AFTERSALE_PGVECTOR_USERNAME`
- `AFTERSALE_PGVECTOR_PASSWORD`
- `AFTERSALE_PGVECTOR_SCHEMA`
- `AFTERSALE_PGVECTOR_INITIALIZE_SCHEMA`
- `AFTERSALE_EMBEDDING_DIMENSION`

PGvector 在当前项目中是 profile、schema、compose、repository contract、opt-in foundation 和 V5.A.1 显式
opt-in `JdbcPolicyVectorRepository` adapter。默认路径不连接 PostgreSQL / PGvector，不调用 Spring AI
`VectorStore`，也不证明 live PGvector validation 已完成。

V5.A.2 adds schema version baseline `2026-06-01-001` to `schema-rag-postgres.sql` as a reference for current manual
initialization and future migration planning. It is not a production database migration framework. Flyway / Liquibase
selection remains pending V5.B.2, live PGvector validation remains pending V5.A.3, and production DB migration /
deployment is not completed.

### Actuator / OpenAPI

- `AFTERSALE_HEALTH_SHOW_DETAILS`
- `AFTERSALE_OPENAPI_ENABLED`
- `AFTERSALE_SWAGGER_UI_ENABLED`

模板保持默认 actuator exposure 只包含 `health`。Swagger UI 在模板中默认关闭，避免把本地 review 入口误当成
生产 public API 文档入口。

### 可观测性 / 监控

阶段 2 的可观测性决策见
`docs/decisions/DECISION_PROJECT_REVIEW_OBSERVABILITY_HARDENING.md`。当前模板不接入 Prometheus、Grafana、
OpenTelemetry collector 或外部日志平台，也不默认暴露 prometheus、env、beans、configprops、heapdump、
threaddump 等 actuator endpoint。

当前项目默认可观测性基线是 MDC / structured logs、`X-Request-Id`、ToolCallTrace、ApprovalRequest、Execution
Tree、`/actuator/health`、RAG readiness diagnostics、OpenAPI docs 和 offline RAG evaluation metrics。生产
metrics、distributed tracing、provider latency / cost metrics、dashboard 和日志采集仍是 future / opt-in path。

## Secret / Path Safety

仓库内只能保存环境变量占位，不能保存真实值：

- 不提交真实 API Key；
- 不提交数据库真实密码；
- 不提交 token 或 private endpoint；
- 不提交本地绝对路径；
- 不提交 raw prompt、raw dataset 或客户隐私数据。

如果需要在本地验证 live provider 或 live database，应使用未提交的 shell 环境变量、部署系统注入或本地
gitignored 配置。

## 默认离线边界

默认验证命令仍然是：

```bash
mvn test
mvn checkstyle:check
mvn spotbugs:check
mvn test -Dtest=ArchitectureTest
```

这些命令不需要任何 live provider 或外部基础设施：

- 不需要 real LLM；
- 不需要 API Key；
- 不需要 PostgreSQL；
- 不需要 PGvector；
- 不需要 Docker；
- 不需要 MySQL；
- 不需要 Redis；
- 不需要 external network；
- 不需要 real embedding provider；
- 不需要 Spring AI live provider calls。

`application-prod.example.yml` 只是模板，不参与默认测试 profile，也不改变默认 in-memory / fake 路径。

## Live / Opt-in 边界

如果后续需要 live 验证，应作为显式 opt-in：

- live LLM / Spring AI；
- live embedding provider；
- live PostgreSQL / PGvector；
- MySQL persistence；
- production-like deployment smoke check。

缺少配置时，live 测试应跳过或给出清晰 setup message，不能让默认测试失败。

## 与后续阶段关系

阶段 1 只补生产配置模板和说明。以下能力仍是后续阶段或 V5 production hardening：

- production authentication / authorization；
- secret manager 集成；
- Prometheus / metrics dashboard；
- distributed tracing；
- OpenTelemetry collector；
- external logging platform；
- CI/CD pipeline；
- Kubernetes / Helm；
- Dockerfile hardening；
- deployment hardening implementation；
- `JdbcPolicyVectorRepository` live validation；
- live PGvector validation；
- production ingestion API / admin UI；
- 真实退款、换货、优惠券补偿、支付或物流系统接入。

## Completion Signal

TASK_COMPLETE
