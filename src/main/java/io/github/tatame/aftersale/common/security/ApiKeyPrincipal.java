package io.github.tatame.aftersale.common.security;

public record ApiKeyPrincipal(SecurityRole role) {

    @Override
    public String toString() {
        return "ApiKeyPrincipal[role=" + role + "]";
    }
}
