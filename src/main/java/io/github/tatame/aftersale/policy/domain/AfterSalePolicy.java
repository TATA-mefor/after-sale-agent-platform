package io.github.tatame.aftersale.policy.domain;

import java.time.Instant;
import java.util.Objects;

public final class AfterSalePolicy {

    private final String policyId;
    private final String category;
    private final String productType;
    private final String policyText;
    private final Instant effectiveFrom;
    private final Instant effectiveTo;

    public AfterSalePolicy(
            String policyId,
            String category,
            String productType,
            String policyText,
            Instant effectiveFrom,
            Instant effectiveTo) {
        this.policyId = requireText(policyId, "policyId");
        this.category = requireText(category, "category");
        this.productType = requireText(productType, "productType");
        this.policyText = requireText(policyText, "policyText");
        this.effectiveFrom = Objects.requireNonNull(effectiveFrom, "effectiveFrom must not be null");
        this.effectiveTo = Objects.requireNonNull(effectiveTo, "effectiveTo must not be null");
        if (effectiveTo.isBefore(effectiveFrom)) {
            throw new IllegalArgumentException("effectiveTo must not be before effectiveFrom");
        }
    }

    public String getPolicyId() {
        return policyId;
    }

    public String getCategory() {
        return category;
    }

    public String getProductType() {
        return productType;
    }

    public String getPolicyText() {
        return policyText;
    }

    public Instant getEffectiveFrom() {
        return effectiveFrom;
    }

    public Instant getEffectiveTo() {
        return effectiveTo;
    }

    public boolean isEffectiveAt(Instant checkedAt) {
        Objects.requireNonNull(checkedAt, "checkedAt must not be null");
        return !checkedAt.isBefore(effectiveFrom) && !checkedAt.isAfter(effectiveTo);
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
