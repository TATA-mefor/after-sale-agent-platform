package io.github.tatame.aftersale.policy.application;

import io.github.tatame.aftersale.policy.domain.AfterSalePolicy;
import io.github.tatame.aftersale.policy.domain.PolicyRepository;
import io.github.tatame.aftersale.policy.domain.PolicySearchQuery;
import io.github.tatame.aftersale.policy.domain.PolicySearchResult;
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
