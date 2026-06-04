package io.github.tatame.aftersale.common.observability.correlation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

import org.junit.jupiter.api.Test;

class CorrelationIdsTest {

    @Test
    void safeIdentifierIsReused() {
        String candidate = "req.demo_2026-06-04:01";

        String resolved = CorrelationIds.safeOrGenerated(candidate);

        assertThat(resolved).isEqualTo(candidate);
    }

    @Test
    void missingOrBlankIdentifierIsGenerated() {
        assertThat(CorrelationIds.safeOrGenerated(null)).isNotBlank();
        assertThat(CorrelationIds.safeOrGenerated("")).isNotBlank();
        assertThat(CorrelationIds.safeOrGenerated("   ")).isNotBlank();
    }

    @Test
    void unsafeIdentifiersAreDiscardedAndRegenerated() {
        assertRegenerated("req with space");
        assertRegenerated("req\ncontrol");
        assertRegenerated("https://example.invalid/request");
        assertRegenerated("folder/file");
        assertRegenerated("api_key-demo");
        assertRegenerated("token-demo");
        assertRegenerated("secret-demo");
        assertRegenerated("password-demo");
        assertRegenerated("bearer-demo");
        assertRegenerated("sk-demo");
        assertRegenerated("a".repeat(CorrelationIds.MAX_LENGTH + 1));
    }

    @Test
    void unsafeIdentifiersDoNotEscapeThroughExceptions() {
        assertThatNoException().isThrownBy(() -> CorrelationIds.safeOrGenerated("token-demo"));
        assertThatNoException().isThrownBy(() -> CorrelationIds.safeOrGenerated("https://example.invalid/request"));
    }

    private static void assertRegenerated(String unsafeValue) {
        String resolved = CorrelationIds.safeOrGenerated(unsafeValue);

        assertThat(resolved).isNotBlank();
        assertThat(resolved).isNotEqualTo(unsafeValue);
        assertThat(resolved.length()).isLessThanOrEqualTo(CorrelationIds.MAX_LENGTH);
        assertThat(CorrelationIds.isSafe(resolved)).isTrue();
    }
}
