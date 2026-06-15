package io.github.tatame.aftersale.common.security;

public enum SecurityRole {

    ADMIN("ROLE_ADMIN"),
    SUPERVISOR("ROLE_SUPERVISOR"),
    AGENT_OPERATOR("ROLE_AGENT_OPERATOR"),
    SYSTEM_SERVICE("ROLE_SYSTEM_SERVICE");

    private final String authority;

    SecurityRole(String authority) {
        this.authority = authority;
    }

    public String authority() {
        return authority;
    }
}
