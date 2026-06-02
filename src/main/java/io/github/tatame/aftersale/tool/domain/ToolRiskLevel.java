package io.github.tatame.aftersale.tool.domain;

public enum ToolRiskLevel {
    LOW(false),
    MEDIUM(false),
    HIGH(true);

    private final boolean requiresApproval;

    ToolRiskLevel(boolean requiresApproval) {
        this.requiresApproval = requiresApproval;
    }

    public boolean requiresApproval() {
        return requiresApproval;
    }
}
