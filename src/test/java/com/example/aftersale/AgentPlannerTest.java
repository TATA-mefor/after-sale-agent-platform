package com.example.aftersale;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.aftersale.agent.application.AgentApplicationService;
import com.example.aftersale.agent.application.planner.AgentPlan;
import com.example.aftersale.agent.application.planner.AgentPlanner;
import com.example.aftersale.agent.application.planner.AgentPlanningContext;
import com.example.aftersale.agent.application.planner.FakeAgentPlanner;
import com.example.aftersale.agent.application.planner.RuleBasedAgentPlanner;
import com.example.aftersale.agent.infrastructure.llm.AgentPlannerConfiguration;
import com.example.aftersale.agent.infrastructure.llm.LlmAgentPlanner;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.aftersale.ticket.domain.IntentType;
import com.example.aftersale.ticket.domain.TicketStatus;
import com.example.aftersale.tool.domain.ToolRiskLevel;
import java.lang.reflect.Constructor;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class AgentPlannerTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(AgentPlannerConfiguration.class)
            .withBean(ObjectMapper.class);

    @Test
    void ruleBasedPlannerGeneratesV1EquivalentPlan() {
        AgentPlanningContext context = planningContext("我买的耳机有质量问题，左耳没声音，想退货退款。");

        AgentPlan plan = new RuleBasedAgentPlanner().plan(context);

        assertThat(plan.intent()).isEqualTo(IntentType.RETURN_AND_REFUND);
        assertThat(plan.riskLevel()).isEqualTo(ToolRiskLevel.LOW);
        assertThat(plan.policyQuery()).isEqualTo(context.rawUserMessage());
        assertThat(plan.finalSuggestion()).contains("RETURN_AND_REFUND");
        assertThat(plan.plannedTools())
                .extracting("toolName")
                .containsExactly("search_aftersale_policy", "add_ticket_note");
    }

    @Test
    void defaultPlannerModeIsRuleAndDoesNotNeedLlmApiKey() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(AgentPlanner.class);
            assertThat(context.getBean(AgentPlanner.class)).isInstanceOf(RuleBasedAgentPlanner.class);
        });
    }

    @Test
    void fakePlannerModeSelectsFakePlannerForDeterministicTests() {
        contextRunner
                .withPropertyValues("agent.planner.mode=fake")
                .run(context -> {
                    assertThat(context).hasSingleBean(AgentPlanner.class);
                    assertThat(context.getBean(AgentPlanner.class)).isInstanceOf(FakeAgentPlanner.class);
                });
    }

    @Test
    void llmPlannerModeWithoutApiKeyFailsWithClearError() {
        contextRunner
                .withPropertyValues("agent.planner.mode=llm")
                .run(context -> {
                    assertThat(context.getStartupFailure()).isNotNull();
                    assertThat(context.getStartupFailure())
                            .hasRootCauseInstanceOf(IllegalStateException.class)
                            .hasMessageContaining("agent.planner.mode=llm requires");
                });
    }

    @Test
    void agentApplicationServiceConstructorDependsOnAgentPlannerAbstraction() {
        Constructor<?> constructor = AgentApplicationService.class.getConstructors()[0];
        List<Class<?>> parameterTypes = Arrays.asList(constructor.getParameterTypes());

        assertThat(parameterTypes).contains(AgentPlanner.class);
        assertThat(parameterTypes).doesNotContain(RuleBasedAgentPlanner.class, FakeAgentPlanner.class,
                LlmAgentPlanner.class);
    }

    private static AgentPlanningContext planningContext(String message) {
        return new AgentPlanningContext(
                "T-PLANNER-1",
                "U-PLANNER-1",
                "O-PLANNER-1",
                message,
                TicketStatus.CREATED,
                List.of("search_aftersale_policy", "add_ticket_note", "create_aftersale_ticket",
                        "update_ticket_status"),
                "High-risk actions require human approval.",
                Instant.parse("2026-05-14T00:00:00Z"));
    }
}
