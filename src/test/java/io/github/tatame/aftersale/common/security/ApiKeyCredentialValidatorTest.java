package io.github.tatame.aftersale.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ApiKeyCredentialValidatorTest {

    @Test
    void authenticatesConfiguredKeysWithoutExposingSecretInPrincipal() {
        SecurityProperties properties = disabledProperties(new SecurityProperties.ApiKeys(
                "admin-test-key",
                "supervisor-test-key",
                "operator-test-key",
                "system-test-key"));

        ApiKeyCredentialValidator validator = ApiKeyCredentialValidator.from(properties);

        assertThat(validator.authenticate("admin-test-key")).hasValue(new ApiKeyPrincipal(SecurityRole.ADMIN));
        assertThat(validator.authenticate("supervisor-test-key"))
                .hasValue(new ApiKeyPrincipal(SecurityRole.SUPERVISOR));
        assertThat(validator.authenticate("operator-test-key"))
                .hasValue(new ApiKeyPrincipal(SecurityRole.AGENT_OPERATOR));
        assertThat(validator.authenticate("system-test-key"))
                .hasValue(new ApiKeyPrincipal(SecurityRole.SYSTEM_SERVICE));
        assertThat(new ApiKeyPrincipal(SecurityRole.ADMIN).toString()).doesNotContain("admin-test-key");
    }

    @Test
    void ignoresBlankKeysAndRejectsUnknownCandidates() {
        SecurityProperties properties = disabledProperties(new SecurityProperties.ApiKeys(
                "admin-test-key",
                " ",
                "",
                ""));

        ApiKeyCredentialValidator validator = ApiKeyCredentialValidator.from(properties);

        assertThat(validator.hasConfiguredKeys()).isTrue();
        assertThat(validator.authenticate(null)).isEmpty();
        assertThat(validator.authenticate("")).isEmpty();
        assertThat(validator.authenticate("wrong-test-key")).isEmpty();
        assertThat(validator.authenticate(" admin-test-key ")).isEmpty();
    }

    @Test
    void failsFastWhenSecurityEnabledWithoutConfiguredKeys() {
        SecurityProperties properties = new SecurityProperties(
                true,
                "X-API-Key",
                new SecurityProperties.ApiKeys("", "", "", ""));

        assertThatThrownBy(() -> ApiKeyCredentialValidator.from(properties))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("API key security is enabled but no API keys are configured.")
                .hasMessageNotContaining("admin-test-key")
                .hasMessageNotContaining("operator-test-key");
    }

    private static SecurityProperties disabledProperties(SecurityProperties.ApiKeys apiKeys) {
        return new SecurityProperties(false, "X-API-Key", apiKeys);
    }
}
