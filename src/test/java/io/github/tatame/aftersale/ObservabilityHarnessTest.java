package io.github.tatame.aftersale;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class ObservabilityHarnessTest {

    @Test
    void applicationLoggingPatternContainsRequiredMdcFields() throws IOException {
        String config = resourceText("application.yml");

        assertThat(config).contains(
                "correlationId=%X{correlationId:-}",
                "requestId=%X{requestId:-}",
                "ticketId=%X{ticketId:-}",
                "agentRunId=%X{agentRunId:-}",
                "subtaskId=%X{subtaskId:-}",
                "toolName=%X{toolName:-}",
                "approvalRequestId=%X{approvalRequestId:-}");
    }

    private static String resourceText(String path) throws IOException {
        ClassPathResource resource = new ClassPathResource(path);
        return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }
}
