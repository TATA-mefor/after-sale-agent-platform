package com.example.aftersale.agent.application.skill;

import static com.example.aftersale.agent.application.AgentExecutableToolPolicy.ADD_TICKET_NOTE_TOOL;
import static com.example.aftersale.agent.application.AgentExecutableToolPolicy.GET_ORDER_BY_ID_TOOL;
import static com.example.aftersale.agent.application.AgentExecutableToolPolicy.SEARCH_POLICY_TOOL;

import com.example.aftersale.agent.application.handler.CouponAgentHandler;
import com.example.aftersale.agent.application.handler.ExchangeAgentHandler;
import com.example.aftersale.agent.application.handler.GeneralConsultationHandler;
import com.example.aftersale.agent.application.handler.HumanEscalationHandler;
import com.example.aftersale.agent.application.handler.LogisticsAgentHandler;
import com.example.aftersale.agent.application.handler.ReturnAgentHandler;
import com.example.aftersale.agent.application.handler.SpecialistAgentHandler;
import com.example.aftersale.agent.application.planner.SubtaskType;
import com.example.aftersale.tool.domain.ToolRiskLevel;
import java.util.List;
import java.util.Set;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SpecialistSkillConfiguration {

    @Bean
    AgentSkill returnEligibilityAssessmentSkill(ReturnAgentHandler handler) {
        return specialistSkill(
                "ReturnEligibilityAssessmentSkill",
                "Assess return eligibility and draft item-level return guidance.",
                Set.of(SubtaskType.RETURN),
                List.of(GET_ORDER_BY_ID_TOOL, SEARCH_POLICY_TOOL, ADD_TICKET_NOTE_TOOL),
                handler,
                List.of("OrderFact", "PolicyEvidence"),
                List.of("OrderFact", "PolicyEvidence", "SubtaskMemory", "TicketNote"));
    }

    @Bean
    AgentSkill exchangeRecommendationSkill(ExchangeAgentHandler handler) {
        return specialistSkill(
                "ExchangeRecommendationSkill",
                "Assess exchange feasibility and draft item-level exchange guidance.",
                Set.of(SubtaskType.EXCHANGE),
                List.of(GET_ORDER_BY_ID_TOOL, SEARCH_POLICY_TOOL, ADD_TICKET_NOTE_TOOL),
                handler,
                List.of("OrderFact", "PolicyEvidence"),
                List.of("OrderFact", "PolicyEvidence", "SubtaskMemory", "TicketNote"));
    }

    @Bean
    AgentSkill couponConsultationSkill(CouponAgentHandler handler) {
        return specialistSkill(
                "CouponConsultationSkill",
                "Answer coupon after-sale questions from policy evidence.",
                Set.of(SubtaskType.COUPON_CONSULTATION),
                List.of(SEARCH_POLICY_TOOL, ADD_TICKET_NOTE_TOOL),
                handler,
                List.of("PolicyEvidence"),
                List.of("PolicyEvidence", "SubtaskMemory", "TicketNote"));
    }

    @Bean
    AgentSkill logisticsIssueAnalysisSkill(LogisticsAgentHandler handler) {
        return specialistSkill(
                "LogisticsIssueAnalysisSkill",
                "Analyze logistics-related after-sale issues without changing real logistics state.",
                Set.of(SubtaskType.LOGISTICS_ISSUE),
                List.of(GET_ORDER_BY_ID_TOOL, SEARCH_POLICY_TOOL, ADD_TICKET_NOTE_TOOL),
                handler,
                List.of("OrderFact", "PolicyEvidence"),
                List.of("OrderFact", "PolicyEvidence", "SubtaskMemory", "TicketNote"));
    }

    @Bean
    AgentSkill generalAfterSaleConsultationSkill(GeneralConsultationHandler handler) {
        return specialistSkill(
                "GeneralAfterSaleConsultationSkill",
                "Answer general after-sale questions from controlled policy evidence.",
                Set.of(SubtaskType.GENERAL_CONSULTATION),
                List.of(SEARCH_POLICY_TOOL, ADD_TICKET_NOTE_TOOL),
                handler,
                List.of("PolicyEvidence"),
                List.of("PolicyEvidence", "SubtaskMemory", "TicketNote"));
    }

    @Bean
    AgentSkill humanApprovalRoutingSkill(HumanEscalationHandler handler) {
        return specialistSkill(
                "HumanApprovalRoutingSkill",
                "Route high-risk or uncertain work to human review without executing business actions.",
                Set.of(SubtaskType.HUMAN_ESCALATION),
                List.of(ADD_TICKET_NOTE_TOOL),
                handler,
                List.of("RiskFlag"),
                List.of("SubtaskMemory", "TicketNote"),
                ToolRiskLevel.HIGH,
                "High-risk refund, compensation, payment, logistics mutation or dispute closure is requested.");
    }

    private static AgentSkill specialistSkill(
            String skillName,
            String description,
            Set<SubtaskType> supportedTypes,
            List<String> requiredTools,
            SpecialistAgentHandler handler,
            List<String> workspaceReads,
            List<String> workspaceWrites) {
        return specialistSkill(
                skillName,
                description,
                supportedTypes,
                requiredTools,
                handler,
                workspaceReads,
                workspaceWrites,
                ToolRiskLevel.LOW,
                "When the subtask risk level is HIGH or policy evidence is insufficient.");
    }

    private static AgentSkill specialistSkill(
            String skillName,
            String description,
            Set<SubtaskType> supportedTypes,
            List<String> requiredTools,
            SpecialistAgentHandler handler,
            List<String> workspaceReads,
            List<String> workspaceWrites,
            ToolRiskLevel riskLevel,
            String requiresApprovalWhen) {
        return new SpecialistHandlerSkillAdapter(new SkillDefinition(
                skillName,
                description,
                supportedTypes,
                requiredTools,
                List.of(),
                riskLevel,
                requiresApprovalWhen,
                List.of("Tool evidence must come from ToolRegistry outputs."),
                workspaceReads,
                workspaceWrites), handler);
    }
}
