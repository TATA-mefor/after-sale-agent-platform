package io.github.tatame.aftersale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/**
 * Opt-in Spring AI chat smoke. It does not create tickets, AgentRuns, traces, or database records.
 */
@Tag("live")
@EnabledIfSystemProperty(named = "live.spring-ai", matches = "true")
@EnabledIfSystemProperty(named = "live.llm", matches = "true")
class SpringAiLlmClientLiveSmokeTest {

    @Test
    void liveSpringAiChatConfigurationIsPresent() {
        assumeTrue("true".equalsIgnoreCase(System.getenv("SPRING_AI_ENABLED")),
                "Set SPRING_AI_ENABLED=true for Spring AI live smoke.");
        assumeTrue("true".equalsIgnoreCase(System.getenv("SPRING_AI_CHAT_ENABLED")),
                "Set SPRING_AI_CHAT_ENABLED=true for Spring AI chat live smoke.");
        assumeTrue(hasText(System.getenv("SPRING_AI_OPENAI_API_KEY")),
                "Set SPRING_AI_OPENAI_API_KEY for Spring AI chat live smoke.");

        assertThat(System.getenv("SPRING_AI_OPENAI_API_KEY")).isNotBlank();
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
