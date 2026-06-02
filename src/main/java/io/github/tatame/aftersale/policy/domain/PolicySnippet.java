package io.github.tatame.aftersale.policy.domain;

import java.util.Objects;

public record PolicySnippet(
        String policyId,
        String category,
        String productType,
        String snippetText,
        String matchReason) {

    public PolicySnippet {
        policyId = requireText(policyId, "policyId");
        category = requireText(category, "category");
        productType = requireText(productType, "productType");
        snippetText = requireText(snippetText, "snippetText");
        matchReason = requireText(matchReason, "matchReason");
    }

    public static PolicySnippet from(AfterSalePolicy policy, String matchReason) {
        return new PolicySnippet(
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
