package com.example.aftersale;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.aftersale.approval.application.ApprovalApplicationService;
import com.example.aftersale.approval.domain.ApprovalRequest;
import com.example.aftersale.ticket.application.TicketApplicationService;
import com.example.aftersale.ticket.domain.Ticket;
import com.example.aftersale.tool.domain.ToolRiskLevel;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class ApprovalApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ApprovalApplicationService approvalApplicationService;

    @Autowired
    private TicketApplicationService ticketApplicationService;

    @Test
    void canQueryPendingAndSingleApprovalRequest() throws Exception {
        ApprovalRequest request = createApprovalRequest("U-APPROVAL-API-1", "subtask-api-1");

        mockMvc.perform(get("/api/approval-requests/pending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data[*].approvalRequestId", hasItem(request.getApprovalId())))
                .andExpect(jsonPath("$.data[*].subtaskId", hasItem("subtask-api-1")));

        mockMvc.perform(get("/api/approval-requests/{approvalRequestId}", request.getApprovalId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.approvalRequestId").value(request.getApprovalId()))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.riskLevel").value("HIGH"));
    }

    @Test
    void approveEndpointApprovesPendingRequestAndRejectsDuplicateDecision() throws Exception {
        ApprovalRequest request = createApprovalRequest("U-APPROVAL-API-2", "subtask-api-2");

        MvcResult result = mockMvc.perform(post(
                        "/api/approval-requests/{approvalRequestId}/approve",
                        request.getApprovalId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reviewerId": "reviewer-api-1",
                                  "reason": "Evidence is sufficient."
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"))
                .andExpect(jsonPath("$.data.decisionReason").value("Evidence is sufficient."))
                .andReturn();

        String approvalRequestId = JsonPath.read(
                result.getResponse().getContentAsString(),
                "$.data.approvalRequestId");

        mockMvc.perform(post("/api/approval-requests/{approvalRequestId}/reject", approvalRequestId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reviewerId": "reviewer-api-1",
                                  "reason": "Too late."
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"))
                .andExpect(jsonPath("$.message", containsString("not pending")));
    }

    @Test
    void rejectEndpointRejectsPendingRequestAndStoresReason() throws Exception {
        ApprovalRequest request = createApprovalRequest("U-APPROVAL-API-3", "subtask-api-3");

        mockMvc.perform(post("/api/approval-requests/{approvalRequestId}/reject", request.getApprovalId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reviewerId": "reviewer-api-2",
                                  "reason": "Policy evidence is insufficient."
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REJECTED"))
                .andExpect(jsonPath("$.data.decisionReason").value("Policy evidence is insufficient."));
    }

    private ApprovalRequest createApprovalRequest(String userId, String subtaskId) {
        Ticket ticket = ticketApplicationService.createTicket(
                userId,
                "O202605130001",
                "Approval API test.");
        return approvalApplicationService.createForHighRiskSubtask(
                ticket.getTicketId(),
                "RUN-" + subtaskId,
                subtaskId,
                "Manual review before high-risk action.",
                ToolRiskLevel.HIGH);
    }
}
