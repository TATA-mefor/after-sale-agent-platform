# Release Checklist — REPLACE_WITH_RELEASE_VERSION

Date: REPLACE_WITH_DATE
Release Type: REPLACE_WITH_TYPE (major / minor / patch / hotfix)
Status: PENDING / IN_PROGRESS / COMPLETED / ROLLED_BACK

## Pre-flight Checks

- [ ] Branch clean: no uncommitted changes.
- [ ] No secret committed: no real API keys, passwords, tokens, private endpoints, or local absolute paths.
- [ ] `mvn test` passes.
- [ ] `mvn checkstyle:check` passes.
- [ ] `mvn spotbugs:check` passes.
- [ ] `mvn test -Dtest=ArchitectureTest` passes.
- [ ] All docs harness tests pass.
- [ ] Profile matrix reviewed:
  - [ ] default profile: offline baseline intact.
  - [ ] `mysql` profile: opt-in, Flyway disabled by default.
  - [ ] `rag-postgres` profile: opt-in, Flyway disabled by default.
  - [ ] `security-api-key` profile: opt-in only.
  - [ ] `observability-prometheus` profile: opt-in only.
- [ ] Migration reviewed:
  - [ ] Flyway disabled by default (`spring.flyway.enabled: false`).
  - [ ] Migration locations correct.
  - [ ] No destructive migration without separate human review.
- [ ] Security profile reviewed:
  - [ ] API keys provisioned via environment variables or external secret source.
  - [ ] No API keys committed.
  - [ ] Health probes remain public.
  - [ ] Protected endpoints have correct role boundary.
- [ ] Readiness / liveness reviewed:
  - [ ] `/actuator/health/readiness` wired.
  - [ ] `/actuator/health/liveness` wired.
  - [ ] Sensitive actuator endpoints unexposed.
- [ ] Prometheus opt-in reviewed:
  - [ ] `/actuator/prometheus` unexposed by default.
  - [ ] Opt-in profile documented.
- [ ] Rollback target identified:
  - [ ] Previous known-good image tag: `REPLACE_WITH_PREVIOUS_IMAGE_TAG`
  - [ ] Previous Git commit SHA: `REPLACE_WITH_PREVIOUS_GIT_SHA`

## Optional Local Checks (Not Default Gate)

These require Docker, Helm, kubectl installed locally.

- [ ] `docker build -t after-sale-agent-platform:local .`
- [ ] `helm template after-sale-agent-platform deploy/helm/after-sale-agent-platform`
- [ ] `kubectl apply --dry-run=client -f deploy/k8s/`

## Release Info

| Field | Value |
|-------|-------|
| Version | `REPLACE_WITH_VERSION` |
| Image Tag | `REPLACE_WITH_IMAGE_TAG` |
| Git Commit SHA | `REPLACE_WITH_GIT_SHA` |
| Chart Version | `REPLACE_WITH_CHART_VERSION` |
| App Version | `REPLACE_WITH_APP_VERSION` |
| Active Profiles | `REPLACE_WITH_SPRING_PROFILES_ACTIVE` |
| Migration Enabled | `REPLACE_WITH_FLYWAY_ENABLED` |
| Auth Profile | `REPLACE_WITH_AUTH_PROFILE` |
| Observability Profile | `REPLACE_WITH_OBSERVABILITY_PROFILE` |

## Change Summary

REPLACE_WITH_CHANGE_SUMMARY

## Post-release Verification

- [ ] `/actuator/health` UP.
- [ ] `/actuator/health/liveness` UP.
- [ ] `/actuator/health/readiness` UP.
- [ ] Auth smoke test passes (if `security-api-key` enabled).
- [ ] OpenAPI / Swagger accessible under protected profile.
- [ ] Prometheus accessible only if opt-in.
- [ ] Logs contain correlation IDs.
- [ ] No secrets in logs or health details.
- [ ] No exposed sensitive actuator endpoints.
- [ ] RAG default/offline path untouched.
- [ ] Approval / high-risk boundary intact.
- [ ] Rollback command documented: `kubectl set image deployment/after-sale-agent-platform after-sale-agent-platform=REPLACE_WITH_PREVIOUS_IMAGE_TAG`

## Sign-off

- [ ] Maintainer review: REPLACE_WITH_NAME
- [ ] Security review (if auth/secret changes): REPLACE_WITH_NAME
- [ ] Rollback target confirmed: REPLACE_WITH_NAME

## Completion Signal

RELEASE_CHECKLIST_COMPLETE
