package com.example.aftersale.approval.infrastructure.repository;

import com.example.aftersale.approval.domain.ApprovalRepository;
import com.example.aftersale.approval.domain.ApprovalRequest;
import com.example.aftersale.approval.domain.ApprovalStatus;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Repository;

@Repository
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
                .sorted(Comparator.comparing(ApprovalRequest::getRequestedAt)
                        .thenComparing(ApprovalRequest::getApprovalId))
                .toList();
    }
}
