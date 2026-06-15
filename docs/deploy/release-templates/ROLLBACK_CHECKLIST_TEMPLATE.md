# Rollback Checklist — REPLACE_WITH_ROLLBACK_ID

Date: REPLACE_WITH_DATE
Rollback Type: REPLACE_WITH_TYPE (image / config / helm-values / profile / secret / full)
Trigger: REPLACE_WITH_TRIGGER
Status: PENDING / IN_PROGRESS / COMPLETED

## Rollback Trigger

| Field | Value |
|-------|-------|
| Trigger Symptom | REPLACE_WITH_SYMPTOM |
| Detection Method | REPLACE_WITH_DETECTION |
| Severity | REPLACE_WITH_SEVERITY |
| Time Detected | REPLACE_WITH_TIME |

## Current State (Before Rollback)

| Field | Value |
|-------|-------|
| Image Tag | `REPLACE_WITH_CURRENT_IMAGE_TAG` |
| Git Commit SHA | `REPLACE_WITH_CURRENT_GIT_SHA` |
| Active Profiles | `REPLACE_WITH_CURRENT_PROFILES` |
| Migration Status | `REPLACE_WITH_CURRENT_MIGRATION_STATUS` |
| Auth Profile | `REPLACE_WITH_CURRENT_AUTH_PROFILE` |
| Observability Profile | `REPLACE_WITH_CURRENT_OBSERVABILITY_PROFILE` |

## Rollback Target

| Field | Value |
|-------|-------|
| Image Tag | `REPLACE_WITH_ROLLBACK_IMAGE_TAG` |
| Git Commit SHA | `REPLACE_WITH_ROLLBACK_GIT_SHA` |
| Active Profiles | `REPLACE_WITH_ROLLBACK_PROFILES` |
| Migration Status | `REPLACE_WITH_ROLLBACK_MIGRATION_STATUS` |

## Rollback Steps

- [ ] Confirm rollback decision with reviewer.
- [ ] Stop traffic to affected deployment (if applicable).
- [ ] Image rollback: `kubectl set image deployment/after-sale-agent-platform after-sale-agent-platform=REPLACE_WITH_ROLLBACK_IMAGE_TAG`
- [ ] Config rollback: revert ConfigMap to last known-good version.
- [ ] Secret rollback: re-provision from known-good source.
- [ ] Profile rollback: remove problematic profile from `SPRING_PROFILES_ACTIVE`.
- [ ] Restart pods.
- [ ] Verify pod startup completes.

## Migration Caution

- [ ] Migration rollback is NOT automatic. Do NOT undo schema changes without human review.
- [ ] If migration was applied during the failed release, contact DB reviewer before any schema action.
- [ ] Destructive migration recovery requires backup verification.

## Post-rollback Verification

- [ ] `/actuator/health` UP.
- [ ] `/actuator/health/liveness` UP.
- [ ] `/actuator/health/readiness` UP.
- [ ] Pod restart count stable for 5+ minutes.
- [ ] Auth smoke test passes.
- [ ] No secrets in logs.
- [ ] Correlation IDs present.
- [ ] Sensitive actuator endpoints unexposed.
- [ ] RAG default path operational.
- [ ] Approval boundary intact.
- [ ] 5xx rate returns to baseline.
- [ ] No residual errors from rollback procedure.

## Rollback Decision

- [ ] Rollback successful — service restored.
- [ ] Rollback partially successful — follow-up actions required.
- [ ] Rollback failed — escalate.

## Escalation

If rollback fails or root cause is unknown:
- Escalate to: REPLACE_WITH_ESCALATION_CONTACT
- Incident record: REPLACE_WITH_INCIDENT_REF

## Lessons Learned

REPLACE_WITH_LESSONS_LEARNED

## Completion Signal

ROLLBACK_CHECKLIST_COMPLETE
