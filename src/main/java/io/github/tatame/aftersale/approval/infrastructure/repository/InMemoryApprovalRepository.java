package io.github.tatame.aftersale.approval.infrastructure.repository;

import io.github.tatame.aftersale.approval.domain.ApprovalRepository;
import io.github.tatame.aftersale.approval.domain.ApprovalRequest;
import io.github.tatame.aftersale.approval.domain.ApprovalStatus;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("!mysql")
public class InMemoryApprovalRepository implements ApprovalRepository {

    private final ConcurrentMap<String, ApprovalRequest> requests = new ConcurrentHashMap<>();

    @Override
    public ApprovalRequest save(ApprovalRequest request) {
        requests.put(request.getApprovalId(), request);
        return request;
    }

    @Override
    public Optional<ApprovalRequest> findById(String approvalId) {
        return Optional.ofNullable(requests.get(approvalId));
    }

    @Override
    public List<ApprovalRequest> findByStatus(ApprovalStatus status) {
        return requests.values().stream()
                .filter(request -> request.getStatus() == status)
                .sorted(approvalSort())
                .toList();
    }

    @Override
    public List<ApprovalRequest> findByRunId(String runId) {
        return requests.values().stream()
                .filter(request -> request.getRunId().equals(runId))
                .sorted(approvalSort())
                .toList();
    }

    private static Comparator<ApprovalRequest> approvalSort() {
        return Comparator.comparing(ApprovalRequest::getRequestedAt)
                .thenComparing(ApprovalRequest::getApprovalId);
    }
}
