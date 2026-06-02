package io.github.tatame.aftersale.policy.domain;

import java.util.List;

public interface PolicyRepository {

    List<AfterSalePolicy> findAll();

    PolicySearchResult search(PolicySearchQuery query);
}
