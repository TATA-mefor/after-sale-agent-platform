package com.example.aftersale.policy.rag.ingestion.domain;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public final class PolicyIngestionStateMachine {

    private static final Map<PolicyIngestionStatus, Set<PolicyIngestionStatus>> ALLOWED_TRANSITIONS =
            new EnumMap<>(PolicyIngestionStatus.class);

    static {
        ALLOWED_TRANSITIONS.put(
                PolicyIngestionStatus.CREATED,
                EnumSet.of(PolicyIngestionStatus.RUNNING, PolicyIngestionStatus.CANCELLED));
        ALLOWED_TRANSITIONS.put(
                PolicyIngestionStatus.RUNNING,
                EnumSet.of(
                        PolicyIngestionStatus.CHUNKED,
                        PolicyIngestionStatus.FAILED,
                        PolicyIngestionStatus.CANCELLED));
        ALLOWED_TRANSITIONS.put(
                PolicyIngestionStatus.CHUNKED,
                EnumSet.of(
                        PolicyIngestionStatus.EMBEDDING,
                        PolicyIngestionStatus.FAILED,
                        PolicyIngestionStatus.CANCELLED));
        ALLOWED_TRANSITIONS.put(
                PolicyIngestionStatus.EMBEDDING,
                EnumSet.of(
                        PolicyIngestionStatus.COMPLETED,
                        PolicyIngestionStatus.PARTIALLY_FAILED,
                        PolicyIngestionStatus.FAILED,
                        PolicyIngestionStatus.CANCELLED));
    }

    private PolicyIngestionStateMachine() {
    }

    public static boolean canTransition(PolicyIngestionStatus currentStatus, PolicyIngestionStatus nextStatus) {
        if (currentStatus == null || nextStatus == null || currentStatus.isTerminal()) {
            return false;
        }
        return ALLOWED_TRANSITIONS.getOrDefault(currentStatus, Set.of()).contains(nextStatus);
    }

    public static void requireValidTransition(
            PolicyIngestionStatus currentStatus,
            PolicyIngestionStatus nextStatus) {
        if (!canTransition(currentStatus, nextStatus)) {
            throw new IllegalStateException(
                    "Illegal policy ingestion status transition: " + currentStatus + " -> " + nextStatus);
        }
    }
}
