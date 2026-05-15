package com.example.aftersale.policy.application;

import com.example.aftersale.policy.domain.AfterSalePolicy;
import com.example.aftersale.policy.domain.PolicyRepository;
import com.example.aftersale.policy.domain.PolicySearchQuery;
import com.example.aftersale.policy.domain.PolicySearchResult;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PolicyApplicationService {

    private final PolicyRepository policyRepository;

    public PolicyApplicationService(PolicyRepository policyRepository) {
        this.policyRepository = policyRepository;
    }

    public List<AfterSalePolicy> listPolicies() {
        return policyRepository.findAll();
    }

    public PolicySearchResult search(String query) {
        return search(PolicySearchQuery.of(query));
    }

    public PolicySearchResult search(PolicySearchQuery query) {
        return policyRepository.search(query);
    }
}
