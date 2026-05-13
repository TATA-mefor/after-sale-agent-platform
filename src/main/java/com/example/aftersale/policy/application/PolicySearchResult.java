package com.example.aftersale.policy.application;

import com.example.aftersale.policy.domain.AfterSalePolicy;
import java.util.Objects;

public record PolicySearchResult(
        String policyId,
        String category,
        String productType,
        String matchedText,
        String matchReason) {

    public PolicySearchResult {
        policyId = requireText(policyId, "policyId");
        category = requireText(category, "category");
        productType = requireText(productType, "productType");
        matchedText = requireText(matchedText, "matchedText");
        matchReason = requireText(matchReason, "matchReason");
    }

    public static PolicySearchResult from(AfterSalePolicy policy, String matchReason) {
        return new PolicySearchResult(
                policy.getPolicyId(),
                policy.getCategory(),
                policy.getProductType(),
                policy.getPolicyText(),
                matchReason);
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
