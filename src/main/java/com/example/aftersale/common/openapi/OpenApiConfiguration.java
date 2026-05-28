package com.example.aftersale.common.openapi;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.tags.Tag;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {

    @Bean
    public OpenAPI afterSaleOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("AfterSale-Agent API")
                        .version("V4")
                        .description("""
                                Enterprise after-sale ticket Agent platform. APIs expose ticket intake, AgentRun
                                orchestration, approval-gated high-risk actions, ToolCallTrace audit data,
                                read-only Execution Tree views, and offline readiness health signals. V4 RAG
                                policy retrieval is ToolRegistry-controlled, supports KEYWORD / VECTOR / HYBRID
                                modes, is evidence-only, and does not execute refunds, exchanges, compensation,
                                payment, logistics, or dispute closure.
                                The default demo path is offline and uses fake or in-memory dependencies.
                                """))
                .tags(List.of(
                        tag("Tickets", "Create and read after-sale tickets."),
                        tag("Agent Runs", "Trigger offline AgentRun orchestration for an existing ticket."),
                        tag("Approvals", "Review approval-gated high-risk actions without automatic execution."),
                        tag("Execution Tree", "Read-only AgentRun explanation tree with tool and evidence nodes."),
                        tag("Tool Traces", "ToolCallTrace audit output, including RAG evidence JSON when present."),
                        tag("Platform Health", "Lightweight platform health endpoint; actuator health is separate.")))
                .externalDocs(new ExternalDocumentation()
                        .description("OpenAPI usage and API boundary notes")
                        .url("docs/api/OPENAPI.md"));
    }

    private static Tag tag(String name, String description) {
        return new Tag().name(name).description(description);
    }
}
