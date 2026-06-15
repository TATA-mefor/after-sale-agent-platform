# Change Record — REPLACE_WITH_CHANGE_ID

Date: REPLACE_WITH_DATE
Change Type: REPLACE_WITH_TYPE (release / rollback / hotfix / config-change)
Status: REPLACE_WITH_STATUS (PLANNED / EXECUTED / ROLLED_BACK / SUPERSEDED)

## Change Summary

REPLACE_WITH_CHANGE_SUMMARY

## Before / After

| Field | Before | After |
|-------|--------|-------|
| Image Tag | `REPLACE_WITH_BEFORE_IMAGE_TAG` | `REPLACE_WITH_AFTER_IMAGE_TAG` |
| Git Commit SHA | `REPLACE_WITH_BEFORE_GIT_SHA` | `REPLACE_WITH_AFTER_GIT_SHA` |
| Chart Version | `REPLACE_WITH_BEFORE_CHART_VERSION` | `REPLACE_WITH_AFTER_CHART_VERSION` |
| App Version | `REPLACE_WITH_BEFORE_APP_VERSION` | `REPLACE_WITH_AFTER_APP_VERSION` |
| Active Profiles | `REPLACE_WITH_BEFORE_PROFILES` | `REPLACE_WITH_AFTER_PROFILES` |
| Migration Enabled | `REPLACE_WITH_BEFORE_MIGRATION` | `REPLACE_WITH_AFTER_MIGRATION` |
| Auth Profile | `REPLACE_WITH_BEFORE_AUTH` | `REPLACE_WITH_AFTER_AUTH` |
| Observability Profile | `REPLACE_WITH_BEFORE_OBSERVABILITY` | `REPLACE_WITH_AFTER_OBSERVABILITY` |
| Secret Source | `REPLACE_WITH_BEFORE_SECRET_SOURCE` | `REPLACE_WITH_AFTER_SECRET_SOURCE` |

## Validation Gates

- [ ] `mvn test` — REPLACE_WITH_RESULT
- [ ] `mvn checkstyle:check` — REPLACE_WITH_RESULT
- [ ] `mvn spotbugs:check` — REPLACE_WITH_RESULT
- [ ] `mvn test -Dtest=ArchitectureTest` — REPLACE_WITH_RESULT
- [ ] Docs harness tests — REPLACE_WITH_RESULT
- [ ] Optional Docker build — REPLACE_WITH_RESULT (if run)
- [ ] Optional Helm template — REPLACE_WITH_RESULT (if run)
- [ ] Optional kubectl dry-run — REPLACE_WITH_RESULT (if run)

## Post-change Verification

- [ ] Health probes: REPLACE_WITH_RESULT
- [ ] Auth smoke: REPLACE_WITH_RESULT
- [ ] No secrets in logs: REPLACE_WITH_RESULT
- [ ] Sensitive endpoints unexposed: REPLACE_WITH_RESULT
- [ ] RAG path intact: REPLACE_WITH_RESULT
- [ ] Approval boundary intact: REPLACE_WITH_RESULT

## Rollback

| Field | Value |
|-------|-------|
| Rollback Image Tag | `REPLACE_WITH_ROLLBACK_IMAGE_TAG` |
| Rollback Command | `REPLACE_WITH_ROLLBACK_COMMAND` |
| Rollback Executed | REPLACE_WITH_YES_NO |
| Rollback Change Record | `REPLACE_WITH_ROLLBACK_CHANGE_ID` |

## Known Issues

REPLACE_WITH_KNOWN_ISSUES

## Follow-ups

REPLACE_WITH_FOLLOW_UPS

## Sign-off

- Author: REPLACE_WITH_AUTHOR
- Reviewer: REPLACE_WITH_REVIEWER
- Rollback reviewer (if rollback): REPLACE_WITH_ROLLBACK_REVIEWER

## Completion Signal

CHANGE_RECORD_COMPLETE
