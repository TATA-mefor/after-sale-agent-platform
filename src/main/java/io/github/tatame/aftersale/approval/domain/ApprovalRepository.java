package io.github.tatame.aftersale.approval.domain;

import java.util.List;
import java.util.Optional;

public interface ApprovalRepository {

    ApprovalRequest save(ApprovalRequest request);

    Optional<ApprovalRequest> findById(String approvalId);

    List<ApprovalRequest> findByStatus(ApprovalStatus status);

    List<ApprovalRequest> findByRunId(String runId);
}
