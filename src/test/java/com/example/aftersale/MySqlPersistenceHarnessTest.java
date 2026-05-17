package com.example.aftersale;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class MySqlPersistenceHarnessTest {

    @Test
    void mysqlProfileConfigurationIsExplicitOptInAndUsesEnvironmentPlaceholders() throws IOException {
        String config = classpathText("application-mysql.yml");

        assertThat(config).contains("on-profile: mysql");
        assertThat(config).contains("${AFTERSALE_MYSQL_URL:");
        assertThat(config).contains("${AFTERSALE_MYSQL_USERNAME:");
        assertThat(config).contains("${AFTERSALE_MYSQL_PASSWORD:}");
        assertThat(config).contains("classpath:schema-mysql.sql");
        assertThat(config).contains("classpath:data-mysql.sql");
    }

    @Test
    void defaultConfigurationDoesNotDeclareMysqlConnectionDetails() throws IOException {
        String config = classpathText("application.yml");

        assertThat(config).doesNotContain("spring.datasource");
        assertThat(config).doesNotContain("AFTERSALE_MYSQL_PASSWORD");
        assertThat(config).doesNotContain("jdbc:mysql:");
    }

    @Test
    void schemaCoversCoreTablesAndFields() throws IOException {
        String schema = classpathText("schema-mysql.sql");

        assertTableFields(schema, "tickets",
                "ticket_id", "user_id", "order_id", "raw_user_message", "intent_type", "priority", "status",
                "internal_note", "agent_suggestion", "created_at", "updated_at");
        assertTableFields(schema, "agent_runs",
                "run_id", "ticket_id", "status", "plan_json", "final_answer", "error_message", "started_at",
                "finished_at");
        assertTableFields(schema, "tool_call_traces",
                "trace_id", "run_id", "tool_name", "input_json", "status", "output_json", "latency_ms",
                "error_message", "created_at");
        assertTableFields(schema, "approval_requests",
                "approval_id", "ticket_id", "run_id", "subtask_id", "tool_name", "requested_action",
                "risk_level", "status", "reviewer_id", "decision_reason", "requested_at", "reviewed_at");
        assertTableFields(schema, "orders",
                "order_id", "user_id", "product_id", "product_name", "order_status", "paid_amount", "paid_at",
                "delivered_at", "aftersale_deadline");
        assertTableFields(schema, "aftersale_policies",
                "policy_id", "category", "product_type", "policy_text", "effective_from", "effective_to");
    }

    @Test
    void mysqlSeedDataKeepsDemoOrdersAndPoliciesAvailable() throws IOException {
        String data = classpathText("data-mysql.sql");

        assertThat(data).contains("O202605130001");
        assertThat(data).contains("O-PAID-NOT-SHIPPED");
        assertThat(data).contains("POL-QUALITY-RETURN-EXCHANGE");
        assertThat(data).contains("POL-LOGISTICS-NOT-RECEIVED");
        assertThat(data).contains("ON DUPLICATE KEY UPDATE");
    }

    @Test
    void mysqlHarnessDoesNotCommitRealSecrets() throws IOException {
        String config = classpathText("application-mysql.yml");
        String data = classpathText("data-mysql.sql");
        String schema = classpathText("schema-mysql.sql");

        assertThat(config + data + schema)
                .doesNotContain("password: root")
                .doesNotContain("password: 123")
                .doesNotContain("password: mysql")
                .doesNotContain("OPENAI_API_KEY=");
    }

    private static void assertTableFields(String schema, String tableName, String... fields) {
        assertThat(schema).contains("CREATE TABLE IF NOT EXISTS " + tableName);
        for (String field : fields) {
            assertThat(schema).contains(field);
        }
    }

    private static String classpathText(String path) throws IOException {
        ClassPathResource resource = new ClassPathResource(path);
        assertThat(resource.exists()).isTrue();
        return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }
}
