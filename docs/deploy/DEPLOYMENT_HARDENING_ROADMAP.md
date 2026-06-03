# 部署加固路线图

Date: 2026-06-01

Status: Completed; V5.B.1 Container + CI foundation completed; V5.B.2.1 config / secret boundary completed;
V5.B.2.2 Flyway migration foundation completed; V5.B.2.3 through V5.B.4 planned

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
migration foundation；Liquibase、profile matrix runtime validation 和 production deployment 仍未完成。

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
Flyway migration foundation 已完成且默认关闭；V5.B.2.3 Profile Matrix Validation、V5.B.3 Observability runtime
hardening、V5.B.4 Auth + Kubernetes / Helm + Release / Rollback 仍为 planned。

## V5.B.2.1 Config + Secret Boundary status

V5.B.2.1 已完成配置、密钥和迁移治理的文档基线：

- 新增 `docs/deploy/CONFIG_SECRET_MIGRATION_PLAN.md`。
- 新增 `docs/decisions/DECISION_V5_B2_CONFIG_SECRET_MIGRATION.md`。
- 明确 default / mysql / rag-postgres / prod-template profile matrix。
- 明确 Dockerfile 不 bake secrets，CI default gate 不注入 live secrets。
- 明确 Flyway migration foundation 已在 V5.B.2.2 完成，Liquibase 未引入，profile matrix runtime validation
  仍为后续任务。
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

V5.B.2.2 不引入 Liquibase，不默认启用 Flyway，不实现 profile matrix runtime validation，不修改 Dockerfile /
CI / compose，不修改业务 runtime，不完成 production deployment。

## 推荐后续里程碑

1. V5.B.2.3 Profile / config matrix：default / mysql / rag-postgres / prod-template 的 runtime validation。
2. V5.B.2 Secret management：选择 secret manager 或部署注入策略。
3. V5.B.2 PGvector deployment：在 V5.A.1 opt-in `JdbcPolicyVectorRepository` 基础上补 broader opt-in live
   validation。
4. V5.B.3 Readiness / liveness：区分 liveness 与 readiness。
5. V5.B.3 Observability：Prometheus / Grafana / OpenTelemetry / log aggregation。
6. V5.B.4 Security / auth：production auth/RBAC 和 trace access control。
7. V5.B.4 Release / rollback：版本、迁移、配置和健康检查回滚方案。

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

- liveness 只判断应用进程健康。
- readiness 判断部署环境依赖是否满足当前 profile。
- readiness details 必须 sanitize。
- 不暴露 API Key、数据库密码、tokens、raw prompts、raw provider responses 或 local paths。
- 默认 actuator exposure 仍保持最小化。

## observability checklist

- 定义 low-cardinality metrics。
- 规划 Prometheus registry。
- 规划 Grafana dashboard。
- 规划 OpenTelemetry tracing。
- 规划 provider latency / cost metrics。
- 规划 external log aggregation。
- 不把 secrets、raw prompts 或 full user content 写入 metrics labels / spans / logs。

## security/auth checklist

- 设计 production authentication。
- 设计 production RBAC。
- 保护 approval、trace、execution-tree 和 admin surfaces。
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
- production auth/RBAC is not completed。
- production monitoring is not completed。
- production external integrations are not completed。
- real refund / exchange / compensation / payment / logistics integrations are not completed。

## Completion Signal

TASK_COMPLETE
