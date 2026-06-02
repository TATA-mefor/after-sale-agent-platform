package io.github.tatame.aftersale;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.tatame.aftersale.agent.application.AgentApplicationService;
import io.github.tatame.aftersale.agent.application.planner.AgentPlan;
import io.github.tatame.aftersale.agent.application.planner.AgentPlanner;
import io.github.tatame.aftersale.agent.application.planner.AgentPlanningContext;
import io.github.tatame.aftersale.agent.application.planner.FakeAgentPlanner;
import io.github.tatame.aftersale.agent.application.planner.RuleBasedAgentPlanner;
import io.github.tatame.aftersale.agent.application.planner.SubtaskType;
import io.github.tatame.aftersale.agent.infrastructure.llm.AgentPlannerConfiguration;
import io.github.tatame.aftersale.agent.infrastructure.llm.LlmAgentPlanner;
import io.github.tatame.aftersale.ticket.domain.IntentType;
import io.github.tatame.aftersale.ticket.domain.TicketStatus;
import io.github.tatame.aftersale.tool.domain.ToolRiskLevel;
import com.fasterxml.jackson.databind.ObjectMapper;
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
        assertThat(plan.policyQuery()).contains("质量问题", "退货");
        assertThat(plan.finalSuggestion()).contains("RETURN_AND_REFUND");
        assertThat(plan.plannedTools())
                .extracting("toolName")
                .containsExactly("get_order_by_id", "search_aftersale_policy", "add_ticket_note");
    }

    @Test
    void ruleBasedPlannerSplitsComplexAfterSaleMessageIntoSubtasks() {
        AgentPlanningContext context = planningContext(
                "我买了三件衣服，其中一件有污渍要退货，另一件要换尺码，还有一张优惠券没用上怎么退？");

        AgentPlan plan = new RuleBasedAgentPlanner().plan(context);

        assertThat(plan.intent()).isEqualTo(IntentType.MULTI_INTENT);
        assertThat(plan.hasSubtasks()).isTrue();
        assertThat(plan.subtasks())
                .extracting("type")
                .containsExactly(SubtaskType.RETURN, SubtaskType.EXCHANGE, SubtaskType.COUPON_CONSULTATION);
        assertThat(plan.subtasks())
                .allSatisfy(subtask -> assertThat(subtask.plannedTools())
                        .extracting("toolName")
                        .containsExactly("get_order_by_id", "search_aftersale_policy", "add_ticket_note"));
    }

    @Test
    void ruleBasedPlannerRecognizesRefundOnlyIntent() {
        AgentPlanningContext context = planningContext("商品还没有发货，我只想仅退款，不需要退货。");

        AgentPlan plan = new RuleBasedAgentPlanner().plan(context);

        assertThat(plan.intent()).isEqualTo(IntentType.REFUND_ONLY);
        assertThat(plan.subtasks()).isEmpty();
        assertThat(plan.policyQuery()).contains("仅退款");
    }

    @Test
    void ruleBasedPlannerRecognizesCouponConsultationSubtask() {
        AgentPlanningContext context = planningContext("下单时有一张优惠券没用上，还能退回或补给我吗？");

        AgentPlan plan = new RuleBasedAgentPlanner().plan(context);

        assertThat(plan.intent()).isEqualTo(IntentType.GENERAL_CONSULTATION);
        assertThat(plan.subtasks())
                .extracting("type")
                .containsExactly(SubtaskType.COUPON_CONSULTATION);
    }

    @Test
    void ruleBasedPlannerSplitsReturnAndExchangeIntoTwoSubtasks() {
        AgentPlanningContext context = planningContext("一件衣服有污渍想退货，另一件尺码不合适想换货。");

        AgentPlan plan = new RuleBasedAgentPlanner().plan(context);

        assertThat(plan.intent()).isEqualTo(IntentType.MULTI_INTENT);
        assertThat(plan.subtasks())
                .extracting("type")
                .containsExactly(SubtaskType.RETURN, SubtaskType.EXCHANGE);
    }

    @Test
    void ruleBasedPlannerSplitsReturnAndCouponIntoTwoSubtasks() {
        AgentPlanningContext context = planningContext("耳机有质量问题想退货，另外优惠券没用上能不能退？");

        AgentPlan plan = new RuleBasedAgentPlanner().plan(context);

        assertThat(plan.intent()).isEqualTo(IntentType.MULTI_INTENT);
        assertThat(plan.subtasks())
                .extracting("type")
                .containsExactly(SubtaskType.RETURN, SubtaskType.COUPON_CONSULTATION);
    }

    @Test
    void ruleBasedPlannerMarksHighRiskKeywordsAsApprovalRequired() {
        AgentPlanningContext context = planningContext("我要马上退款，金额较高，如果不给退我就投诉。");

        AgentPlan plan = new RuleBasedAgentPlanner().plan(context);

        assertThat(plan.riskLevel()).isEqualTo(ToolRiskLevel.HIGH);
        assertThat(plan.riskLevel().requiresApproval()).isTrue();
    }

    @Test
    void ruleBasedPlannerUsesMessageContentInsteadOfEvaluationCaseIds() {
        AgentPlanningContext refundContext = planningContextWithTicketId(
                "EVAL-NON-DATASET",
                "商品还没有发货，我只想仅退款，不需要退货。");
        AgentPlanningContext couponContext = planningContextWithTicketId(
                "EVAL-AS-EVAL-003",
                "下单时有一张优惠券没用上，还能退回或补给我吗？");

        AgentPlan refundPlan = new RuleBasedAgentPlanner().plan(refundContext);
        AgentPlan couponPlan = new RuleBasedAgentPlanner().plan(couponContext);

        assertThat(refundPlan.intent()).isEqualTo(IntentType.REFUND_ONLY);
        assertThat(couponPlan.intent()).isEqualTo(IntentType.GENERAL_CONSULTATION);
        assertThat(couponPlan.subtasks())
                .extracting("type")
                .containsExactly(SubtaskType.COUPON_CONSULTATION);
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
        return planningContextWithTicketId("T-PLANNER-1", message);
    }

    private static AgentPlanningContext planningContextWithTicketId(String ticketId, String message) {
        return new AgentPlanningContext(
                ticketId,
                "U-PLANNER-1",
                "O-PLANNER-1",
                message,
                TicketStatus.CREATED,
                List.of("get_order_by_id", "get_user_orders", "search_aftersale_policy", "add_ticket_note",
                        "create_aftersale_ticket", "update_ticket_status"),
                "High-risk actions require human approval.",
                Instant.parse("2026-05-14T00:00:00Z"));
    }
}
