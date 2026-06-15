# 部署加固路线图

Date: 2026-06-01

Status: Completed; V5.B.1 Container + CI foundation completed; V5.B.2.1 config / secret boundary completed;
V5.B.2.2 Flyway migration foundation completed; V5.B.2.3 Profile Matrix Validation completed; V5.B.3 through V5.B.4
roadmap remains in progress; V5.B.3.1 readiness / liveness actuator probe boundary completed; V5.B.3.2 Micrometer
metrics foundation completed; V5.B.3.3 Prometheus opt-in exposure completed; V5.B.3.4 tracing / correlation boundary
completed; V5.B.3.5 observability docs + completion record completed; V5.B.4 in progress overall; V5.B.4.1 Production
Auth / RBAC Boundary Decision completed; V5.B.4.2 Spring Security / API Key Auth Foundation completed; V5.B.4.3 K8s /
Helm Foundation completed; V5.B.4.4 Release / Rollback Foundation completed; V5.B.4 current scope
completed. V5.B Production Hardening current planned scope completed. Production deployment,
release automation, rollback automation, and production monitoring remain future work.

## 目的

本路线图面向维护者和 reviewer，用于说明 AfterSale-Agent 当前部署基线、阶段 6 完成范围、后续生产加固
里程碑和默认离线边界。它不是生产部署手册，也不表示 production deployment 已完成。

## 当前 deployment baseline

- `docker-compose.yml`：本地 app + MySQL 开发环境。
- `docker-compose-rag.yml`：本地 PGvector infrastructure 调试环境。
- `.env.rag.example`：RAG 本地 placeholder 配置示例。
- `application-prod.example.yml`：生产配置模板，不会被默认加载。
- `application-mysql.yml`：显式 MySQL profile。
- `application-rag-postgres.yml`：显式 RAG / PostgreSQL / PGvector profile。
- Actuator health：默认 `/actuator/health`。
- OpenAPI docs：`/v3/api-docs` 和 Swagger UI。
- default offline validation：`mvn test`、`mvn checkstyle:check`、`mvn spotbugs:check`、
  `mvn test -Dtest=ArchitectureTest`。

## 阶段 6 完成范围

阶段 6 完成的是部署加固路线：

- 新增 deployment hardening decision。
- 新增 deployment hardening roadmap。
- 更新 production config docs、README、quality docs、validation docs、release summary 和 active correction plan。
- 新增 docs harness test。

阶段 6 不实现 runtime，不新增 Dockerfile，不新增 CI/CD，不新增 Kubernetes / Helm，不接入 secret manager，
不接入 production monitoring，不做 production live PGvector validation。V5.A.1 后续新增了显式 opt-in
`JdbcPolicyVectorRepository`，V5.A.2 记录了 schema version baseline `2026-06-01-001`，V5.A.3 新增了显式
opt-in PGvector connectivity smoke，V5.A.4 完成 V5.A 总收口。V5.B.2.2 后续选择 Flyway 并新增默认关闭的
migration foundation；V5.B.2.3 后续完成 file-based profile matrix validation harness。Liquibase 和 production
deployment 仍未完成。

## V5.B.1 Container + CI status

V5.B.1 已完成 container + CI foundation：

- `Dockerfile` 使用 Java 17 multi-stage build 和非 root runtime 用户。
- `.dockerignore` 排除 `.env*`、key/certificate 文件、`target`、Git metadata、IDE 文件、logs、temp 和本地数据目录。
- `.github/workflows/ci.yml` 运行 `mvn test`、`mvn checkstyle:check`、`mvn spotbugs:check` 和
  `mvn test -Dtest=ArchitectureTest`。
- CI 额外执行 `docker build -t after-sale-agent-platform:ci .`，但不 push image、不 registry login、不部署。
- 默认 CI 不运行 live LLM、live Spring AI、live PGvector、live MySQL、Redis、Docker Compose 或外部业务服务。
- 详细说明见 `docs/deploy/CONTAINER_CI_HARDENING.md` 和
  `version-updates/EXEC_PLAN_V5_B1_CONTAINER_CI.md`。

V5.B.1 不等于 production deployment。V5.B.2.1 Config + Secret Boundary 已完成文档基线；V5.B.2.2
Flyway migration foundation 已完成且默认关闭；V5.B.2.3 Profile Matrix Validation 已完成 file-based harness。
V5.B.2 current scope completed。V5.B.3.1 Readiness / Liveness Boundary 已完成最小 Actuator probe 边界。
V5.B.3.2 Micrometer metrics foundation 已完成。V5.B.3.3 Prometheus opt-in exposure 已完成。V5.B.3.4
tracing / correlation boundary 已完成。V5.B.3.5 observability docs + completion record 已完成。
V5.B.4 Auth + Kubernetes / Helm + Release / Rollback 仍为 in progress overall；V5.B.4.1 Production Auth / RBAC
Boundary Decision 已完成文档决策，V5.B.4.2 Spring Security / API Key Auth Foundation completed，V5.B.4.3 K8s /
Helm Foundation completed，V5.B.4.4 Release / Rollback Foundation completed。Production monitoring backend、
dashboards、alerting、log aggregation 和 OpenTelemetry 仍为 future / opt-in。

## V5.B.2.1 Config + Secret Boundary status

V5.B.2.1 已完成配置、密钥和迁移治理的文档基线：

- 新增 `docs/deploy/CONFIG_SECRET_MIGRATION_PLAN.md`。
- 新增 `docs/decisions/DECISION_V5_B2_CONFIG_SECRET_MIGRATION.md`。
- 明确 default / mysql / rag-postgres / prod-template profile matrix。
- 明确 Dockerfile 不 bake secrets，CI default gate 不注入 live secrets。
- 明确 Flyway migration foundation 已在 V5.B.2.2 完成，Liquibase 未引入，profile matrix validation harness 已在
  V5.B.2.3 完成。
- 默认验证仍保持离线、确定性。

V5.B.2.1 不修改 application yml runtime 语义，不修改 Dockerfile / CI / compose，不实现 secret manager，不实现
Flyway / Liquibase，不实现 production deployment。

## V5.B.2.2 Flyway Migration Foundation status

V5.B.2.2 已完成 Flyway migration foundation：

- `pom.xml` 新增 Flyway 依赖，版本由 Spring Boot dependency management 管理。
- `application.yml` 默认关闭 Flyway。
- `application-mysql.yml` 通过 `AFTERSALE_FLYWAY_ENABLED:false` 显式 opt-in MySQL migration location。
- `application-rag-postgres.yml` 通过 `AFTERSALE_RAG_FLYWAY_ENABLED:false` 显式 opt-in PGvector migration
  location。
- 新增 MySQL schema-only baseline migration，不包含 `data-mysql.sql` demo seed。
- 新增 PGvector schema-only baseline migration，复制 `schema-rag-postgres.sql` version `2026-06-01-001` 的
  schema 语义。
- 新增 `docs/deploy/MIGRATION_FOUNDATION.md` 和
  `docs/exec-plans/completed/EXEC_PLAN_V5_B2_2_FLYWAY_MIGRATION_FOUNDATION.md`。

V5.B.2.2 不引入 Liquibase，不默认启用 Flyway，不修改 Dockerfile / CI / compose，不修改业务 runtime，不完成
production deployment。V5.B.2.3 后续补充 profile matrix validation harness，runtime profile behavior was not
changed。

## V5.B.2.3 Profile Matrix Validation status

V5.B.2.3 已完成 Profile Matrix Validation：

- 新增 `ProfileMatrixValidationTest`，只读取配置、CI、迁移文件和 live smoke 测试源码。
- 新增 `ProfileMatrixValidationDocsTest`，只读取文档和完成记录。
- 覆盖 default offline / local baseline、`mysql`、`rag-postgres`、`application-prod.example.yml` template only、
  Flyway disabled-by-default、CI default offline gate 和 live PGvector smoke opt-in 边界。
- 保持 `AFTERSALE_PGVECTOR_URL`、`AFTERSALE_PGVECTOR_USERNAME`、`AFTERSALE_PGVECTOR_PASSWORD`、
  `AFTERSALE_PGVECTOR_SCHEMA` 现有变量命名。
- 记录 `CREATE EXTENSION IF NOT EXISTS vector` 权限限制和 live smoke skip 边界。

V5.B.2.3 不修改 runtime profile behavior，不连接 MySQL / PostgreSQL / PGvector，不运行 Docker，不调用 real LLM、
real embedding provider 或 Spring AI live provider。

## V5.B.3.1 Readiness / Liveness Boundary status

V5.B.3.1 已完成 readiness / liveness actuator probe boundary：

- `application.yml` 启用 `management.endpoint.health.probes.enabled=true`。
- 增加 `liveness` 和 `readiness` health groups。
- Actuator web exposure remains health-only。
- `/actuator/health`、`/actuator/health/liveness` 和 `/actuator/health/readiness` 可用。
- `/actuator/env`、`/actuator/beans`、`/actuator/configprops`、`/actuator/heapdump`、
  `/actuator/threaddump` 和 `/actuator/prometheus` 默认不可用。
- 新增 `ReadinessLivenessBoundaryTest` 和 `ReadinessLivenessBoundaryDocsTest`。
- 新增 `docs/deploy/OBSERVABILITY_READINESS_LIVENESS.md` 和
  `docs/exec-plans/completed/EXEC_PLAN_V5_B3_1_READINESS_LIVENESS_BOUNDARY.md`。

V5.B.3.1 不实现 Prometheus registry、Micrometer business metrics、OpenTelemetry、collector、Grafana dashboard、
production monitoring、live DB / PGvector / LLM / embedding readiness checks、production auth、Kubernetes / Helm 或
release / rollback hardening。

## V5.B.3.2 Micrometer Metrics Foundation status

V5.B.3.2 已完成 low-cardinality Micrometer metrics foundation：

- 新增 `ApplicationMetricsRecorder`、`MetricNames`、`MetricTags`、`MetricOutcome` 和 tag sanitizer。
- AgentRun、ToolCall、Approval、RAG search 和 provider-call 路径记录 best-effort metrics。
- Metric names 使用 `aftersale.*` prefix。
- Metric tags 限定为低基数字段，并清理 secret、path、URL、JDBC URL、raw prompt、query、snippet 和 raw text。
- Actuator web exposure 仍为 health-only；`/actuator/metrics` 和 `/actuator/prometheus` 默认不可用。
- 新增 `docs/deploy/OBSERVABILITY_METRICS_FOUNDATION.md` 和
  `docs/exec-plans/completed/EXEC_PLAN_V5_B3_2_MICROMETER_METRICS_FOUNDATION.md`。

V5.B.3.2 不实现 Prometheus registry、OpenTelemetry、collector、Grafana dashboard、production monitoring backend、
provider cost dashboard、production auth、Kubernetes / Helm、release / rollback hardening 或真实外部业务系统接入。

## V5.B.3.3 Prometheus Opt-in Exposure status

V5.B.3.3 已完成 Prometheus opt-in exposure：

- `pom.xml` 新增 Boot-managed `micrometer-registry-prometheus` dependency，不写散落版本号。
- `application.yml` 默认禁用 Prometheus endpoint，并保持 Actuator web exposure health-only。
- `application-observability-prometheus.yml` 新增 `observability-prometheus` profile，只在显式 opt-in 时暴露
  `/actuator/prometheus`。
- `/actuator/metrics`、`/actuator/env`、`/actuator/beans`、`/actuator/configprops`、`/actuator/heapdump` 和
  `/actuator/threaddump` 仍不暴露。
- 新增 `PrometheusOptInExposureTest` 和 `PrometheusOptInDocsTest`。
- 新增 `docs/deploy/OBSERVABILITY_PROMETHEUS_OPT_IN.md` 和
  `docs/exec-plans/completed/EXEC_PLAN_V5_B3_3_PROMETHEUS_OPT_IN_EXPOSURE.md`。

V5.B.3.3 不实现 OpenTelemetry、distributed tracing、cross-service propagation、Grafana dashboard、scrape jobs、
alert rules、production monitoring backend、production auth、Kubernetes / Helm、release / rollback hardening 或
真实外部业务系统接入。

## V5.B.3.4 Tracing / Correlation Boundary status

V5.B.3.4 已完成 local HTTP tracing / correlation boundary：

- 新增安全的 `X-Correlation-Id` 和 `X-Request-Id` 解析与生成边界。
- 响应头返回已清理的 `X-Correlation-Id` 和 `X-Request-Id`。
- MDC 增加 `correlationId`，并保留 `requestId`。
- 日志 pattern 增加 `correlationId` 字段。
- 空白、过长、控制字符、空白字符、URL-like、path-like 和 credential-like header 值不会被 echo，也不会进入
  MDC。
- correlation ID 和 request ID 不作为 Micrometer tags。
- 新增 `CorrelationIdsTest`、`CorrelationIdFilterBoundaryTest`、`CorrelationObservabilityBoundaryTest` 和
  `TracingCorrelationDocsTest`。
- 新增 `docs/deploy/OBSERVABILITY_TRACING_CORRELATION.md` 和
  `docs/exec-plans/completed/EXEC_PLAN_V5_B3_4_TRACING_CORRELATION_BOUNDARY.md`。

V5.B.3.4 不实现 OpenTelemetry、distributed tracing、cross-service propagation、W3C trace context、Jaeger、Zipkin、
collector、Grafana dashboard、production tracing backend、production monitoring backend、production auth、
Kubernetes / Helm、release / rollback hardening 或真实外部业务系统接入。

## V5.B.3.5 Observability Docs + Completion status

V5.B.3.5 已完成 observability docs + completion record：

- 新增 `docs/deploy/OBSERVABILITY_DOCS_COMPLETION.md`。
- 新增 `docs/exec-plans/completed/EXEC_PLAN_V5_B3_5_OBSERVABILITY_DOCS_COMPLETION_RECORD.md`。
- 汇总 V5.B.3.1 readiness / liveness、V5.B.3.2 Micrometer metrics、V5.B.3.3 Prometheus opt-in exposure 和
  V5.B.3.4 tracing / correlation boundary。
- 明确 production monitoring backend、Grafana dashboards、alerting、scrape jobs、log aggregation、
  OpenTelemetry、distributed tracing、cross-service propagation、production auth、Kubernetes / Helm 和
  release / rollback hardening 仍为 future / opt-in。
- 新增 `ObservabilityDocsCompletionDocsTest`。

V5.B.3.5 不修改 runtime，不新增 production monitoring backend，不新增 OpenTelemetry，不新增 dashboards、alerts、
scrape jobs、log aggregation 或 external observability platform。

## V5.B.4.1 Production Auth / RBAC Boundary status

V5.B.4.1 已完成 production auth / RBAC boundary decision：

- 新增 `docs/decisions/DECISION_V5_B4_AUTH_RBAC_BOUNDARY.md`。
- 新增 `docs/deploy/AUTH_RBAC_BOUNDARY.md`。
- 新增 `docs/exec-plans/completed/EXEC_PLAN_V5_B4_1_AUTH_RBAC_BOUNDARY_DECISION.md`。
- 固定 planned RBAC role vocabulary：`CUSTOMER`、`AGENT_OPERATOR`、`SUPERVISOR`、`ADMIN`、
  `SYSTEM_SERVICE`。
- 记录 Ticket、AgentRun、Approval、ToolCallTrace、Execution Tree、health、OpenAPI / Swagger UI、
  Prometheus opt-in endpoint、future admin ingestion API 和 ToolRegistry direct access 的 API access matrix。
- 明确 ToolRegistry direct access never public，ToolRegistry remains internal Agent execution boundary。
- 明确 high-risk actions require Approval。
- 明确 `search_aftersale_policy` remains LOW-risk read-only。
- 明确 RAG evidence-only：policy evidence only，不是 business decision，也不执行业务动作。

V5.B.4.1 不实现 Spring Security、JWT、API key runtime、OAuth2 / OIDC、session login、runtime RBAC enforcement、
Kubernetes / Helm、release automation、rollback automation、secret manager integration 或 production deployment。
V5.B.4.2 后续已完成 opt-in API key auth foundation。当前 API surface 如未启用 `security-api-key` profile，
仍不应直接暴露到 public internet。V5.B.4 overall 仍未完成，后续拆分为：

- V5.B.4.2 Spring Security / API Key Auth Foundation completed。
- V5.B.4.3 K8s / Helm Foundation completed。
- V5.B.4.4 Release / Rollback Foundation completed。

## V5.B.4.2 Spring Security / API Key Auth Foundation status

V5.B.4.2 已完成 opt-in Spring Security API key auth foundation：

- 新增 `spring-boot-starter-security`，版本由 Spring Boot dependency management 管理。
- 默认 profile 保持 `agent.security.enabled=false` 和 permit-all。
- 新增 `security-api-key` profile，通过 `X-API-Key` header 执行 stateless API key auth。
- 支持 `ADMIN`、`SUPERVISOR`、`AGENT_OPERATOR` 和 `SYSTEM_SERVICE` runtime role mapping。
- Health probes remain public。
- Ticket、AgentRun、Approval、Trace、ExecutionTree、OpenAPI / Swagger UI 和 opt-in Prometheus 在 security
  profile 下受保护。
- OpenAPI / Swagger UI 需要 `ADMIN` 或 `SUPERVISOR`。
- `/actuator/prometheus` 在 `observability-prometheus + security-api-key` 下需要 `ADMIN` 或 `SYSTEM_SERVICE`。
- Sensitive actuator endpoints 仍不暴露。
- 新增 `docs/deploy/AUTH_RUNTIME_FOUNDATION.md` 和
  `docs/exec-plans/completed/EXEC_PLAN_V5_B4_2_SPRING_SECURITY_API_KEY_AUTH_FOUNDATION.md`。

V5.B.4.2 不实现 OAuth2 / OIDC、JWT issuer / JWKS、session login、user database、secret manager、tenant
isolation、rate limiting、Kubernetes / Helm、release / rollback automation 或 full production IAM。

## 推荐后续里程碑

1. V5.B.2 Secret management：选择 secret manager 或部署注入策略。
2. V5.B.2 PGvector deployment：在 V5.A.1 opt-in `JdbcPolicyVectorRepository` 基础上补 broader opt-in live
   validation。
3. Future production monitoring：Grafana / log aggregation / alerting implementation；OpenTelemetry / cross-service
   propagation remains future / opt-in unless a later task scopes it separately。
5. V5.B.4 Security / auth：production auth/RBAC 和 trace access control。
6. V5.B.4 Release / rollback：版本、迁移、配置和健康检查回滚方案。

## Dockerfile checklist

- 选择多阶段构建或 Spring Boot layertools。
- 使用非 root 用户运行。
- 固定基础镜像系列并记录升级策略。
- 不把 secret、`.env` 或真实配置写入镜像。
- 明确 JVM container memory 参数。
- 增加本地镜像 smoke check。

## CI quality gate checklist

- 运行 `mvn test`。
- 运行 `mvn checkstyle:check`。
- 运行 `mvn spotbugs:check`。
- 运行 `mvn test -Dtest=ArchitectureTest`。
- 缺少 live provider / database 配置时，live tests 应 skip，不得让默认 gate 失败。

## profile matrix checklist

- default profile：in-memory / fake，默认离线。
- `mysql` profile：显式 opt-in MySQL。
- `rag-postgres` profile：显式 opt-in PostgreSQL / PGvector。
- prod template：`application-prod.example.yml` 只作为模板。
- 每个 profile 的 required env vars 和 non-goals 必须文档化。

## secret management checklist

- 不提交真实 API Key。
- 不提交数据库真实密码。
- 不提交 tokens 或 private endpoints。
- 不提交 local absolute paths。
- 真实 secret 由部署系统、外部配置中心或 secret manager 注入。
- secret manager 仍是 future work。

## database migration checklist

- 使用 V5.B.2.2 Flyway migration foundation 作为当前版本化 schema 起点。
- Liquibase 未引入；如后续确需 changeset DSL 或 rollback DSL，再独立评估。
- 区分 schema migration、demo seed 和 production data migration。
- 定义 migration rollback strategy。
- migration 不得成为默认离线测试的外部依赖。
- 数据库密码只通过环境变量或 secret manager 注入。

## PGvector deployment checklist

- 使用 V5.A.1 opt-in `JdbcPolicyVectorRepository` 作为 JDBC adapter baseline。
- 使用 V5.A.2 `schema-rag-postgres.sql` baseline 作为当前手动初始化参考。
- 使用 V5.A.3 opt-in `JdbcPolicyVectorRepositorySmokeTest` 作为 connectivity smoke baseline。
- 定义 vector schema migration。
- 定义 index creation / refresh strategy。
- 增加 broader opt-in production PGvector validation。
- 保留 `FakeEmbeddingClient` 和 in-memory vector store 默认路径。
- 不让默认 `mvn test` 连接 PostgreSQL / PGvector。

## readiness/liveness checklist

- V5.B.3.1 已完成最小 probe 边界：liveness 只判断应用进程和 lifecycle state。
- V5.B.3.1 已完成默认 profile 基础 readiness：用于当前 offline / local profile 的基本 traffic readiness。
- live dependency readiness checks 仍是 future / opt-in，不属于默认 readiness。
- readiness details 必须 sanitize。
- 不暴露 API Key、数据库密码、tokens、raw prompts、raw provider responses 或 local paths。
- 默认 actuator exposure 仍保持最小化。

## observability checklist

- 定义 low-cardinality metrics。
- V5.B.3.3 已完成 Prometheus opt-in exposure；生产 scrape jobs / dashboards 仍是后续任务。
- V5.B.3.4 已完成 local HTTP correlation boundary；production tracing backend 仍是后续任务。
- 规划 Grafana dashboard。
- 规划 OpenTelemetry tracing。
- 规划 provider latency / cost metrics。
- 规划 external log aggregation。
- 不把 secrets、raw prompts 或 full user content 写入 metrics labels / spans / logs。

## security/auth checklist

- V5.B.4.1 已完成 production authentication / RBAC boundary decision。
- V5.B.4.2 已完成 opt-in Spring Security / API Key Auth Foundation。
- 保护 approval、trace、execution-tree、OpenAPI / Swagger UI、Prometheus opt-in endpoint 和 admin surfaces。
- 保持 ToolRegistry direct access never public。
- 保持 high-risk actions require Approval。
- 保持 `search_aftersale_policy` LOW-risk read-only。
- 增加 rate limit / abuse protection。
- 明确 audit log 与 ToolCallTrace 的边界。

## release/rollback checklist

- artifact versioning。
- config versioning。
- database migration rollback。
- feature flag 或 profile rollback。
- health gate。
- release notes。
- known limitations。

## default offline boundary

default offline validation 不需要：

- real LLM；
- API Key；
- PostgreSQL；
- PGvector；
- Docker；
- MySQL；
- Redis；
- external network；
- real embedding provider；
- Spring AI live provider calls；
- secret manager；
- CI runner；
- Kubernetes / Helm；
- Prometheus / Grafana / OpenTelemetry collector。

## 什么不是当前已完成能力

- CD / release automation is not implemented。
- Kubernetes / Helm is not implemented。
- secret manager is not implemented。
- production deployment is not completed。
- live PGvector validation is not completed。
- JdbcPolicyVectorRepository live validation is not completed。
- full production auth/RBAC is not completed。
- full production IAM is not completed。
- V5.B.4.2 completed only the opt-in API key auth foundation。
- production monitoring is not completed。
- production external integrations are not completed。
- real refund / exchange / compensation / payment / logistics integrations are not completed。

## Completion Signal

TASK_COMPLETE
