package com.example.aftersale;

import static org.hamcrest.Matchers.blankOrNullString;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.aftersale.common.observability.ObservabilityConstants;
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
class TicketApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createTicketReturnsCreatedTicket() throws Exception {
        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": "U-2001",
                                  "orderId": "O-2001",
                                  "message": "Headphones arrived with one side silent."
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("ok"))
                .andExpect(jsonPath("$.data.ticketId", not(blankOrNullString())))
                .andExpect(jsonPath("$.data.userId").value("U-2001"))
                .andExpect(jsonPath("$.data.orderId").value("O-2001"))
                .andExpect(jsonPath("$.data.status").value("CREATED"))
                .andExpect(jsonPath("$.data.intentType").value("UNKNOWN"))
                .andExpect(jsonPath("$.data.priority").value("NORMAL"));
    }

    @Test
    void getTicketReturnsExistingTicket() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": "U-2002",
                                  "orderId": "O-2002",
                                  "message": "The package was delivered but the charger is missing."
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        String ticketId = JsonPath.read(createResult.getResponse().getContentAsString(), "$.data.ticketId");

        mockMvc.perform(get("/api/tickets/{ticketId}", ticketId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.ticketId").value(ticketId))
                .andExpect(jsonPath("$.data.userId").value("U-2002"))
                .andExpect(jsonPath("$.data.orderId").value("O-2002"))
                .andExpect(jsonPath("$.data.status").value("CREATED"));
    }

    @Test
    void getTicketReturnsClearErrorWhenTicketDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/tickets/{ticketId}", "T-MISSING-M4"))
                .andExpect(status().isNotFound())
                .andExpect(header().string(ObservabilityConstants.REQUEST_ID_HEADER, notNullValue()))
                .andExpect(jsonPath("$.code").value("TICKET_NOT_FOUND"))
                .andExpect(jsonPath("$.message", containsString("T-MISSING-M4")));
    }

    @Test
    void listTicketsReturnsPagedReadOnlyTicketResults() throws Exception {
        createTicket("U-PAGE-3201", "O-PAGE-3201", "First pagination ticket.");
        createTicket("U-PAGE-3201", "O-PAGE-3202", "Second pagination ticket.");
        createTicket("U-PAGE-3201", "O-PAGE-3203", "Third pagination ticket.");

        mockMvc.perform(get("/api/tickets")
                        .param("userId", "U-PAGE-3201")
                        .param("page", "0")
                        .param("size", "2")
                        .param("sort", "ticketId,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.items", hasSize(2)))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(2))
                .andExpect(jsonPath("$.data.totalElements").value(3))
                .andExpect(jsonPath("$.data.totalPages").value(2))
                .andExpect(jsonPath("$.data.hasNext").value(true))
                .andExpect(jsonPath("$.data.hasPrevious").value(false))
                .andExpect(jsonPath("$.data.sort").value("ticketId,asc"))
                .andExpect(jsonPath("$.data.items[0].userId").value("U-PAGE-3201"));
    }

    @Test
    void listTicketsSupportsQueryFiltersAndEmptyPages() throws Exception {
        createTicket("U-PAGE-3202", "O-PAGE-3204", "Filter by order id.");

        mockMvc.perform(get("/api/tickets")
                        .param("userId", "U-PAGE-3202")
                        .param("orderId", "O-PAGE-3204")
                        .param("status", "CREATED")
                        .param("intentType", "UNKNOWN")
                        .param("page", "1")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items", hasSize(0)))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.totalPages").value(1))
                .andExpect(jsonPath("$.data.hasNext").value(false))
                .andExpect(jsonPath("$.data.hasPrevious").value(true));
    }

    @Test
    void listTicketsRejectsInvalidPaginationAndSortParameters() throws Exception {
        mockMvc.perform(get("/api/tickets").param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message", containsString("page")));

        mockMvc.perform(get("/api/tickets").param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message", containsString("size")));

        mockMvc.perform(get("/api/tickets").param("sort", "status,desc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message", containsString("sort field")));

        mockMvc.perform(get("/api/tickets").param("sort", "createdAt,sideways"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message", containsString("sort direction")));
    }

    private String createTicket(String userId, String orderId, String message) throws Exception {
        MvcResult createResult = mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": "%s",
                                  "orderId": "%s",
                                  "message": "%s"
                                }
                                """.formatted(userId, orderId, message)))
                .andExpect(status().isCreated())
                .andReturn();
        return JsonPath.read(createResult.getResponse().getContentAsString(), "$.data.ticketId");
    }

    @Test
    void requestWithoutRequestIdReturnsGeneratedRequestIdHeader() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(header().string(ObservabilityConstants.REQUEST_ID_HEADER, notNullValue()));
    }

    @Test
    void requestWithRequestIdReturnsSameRequestIdHeader() throws Exception {
        mockMvc.perform(get("/api/health")
                        .header(ObservabilityConstants.REQUEST_ID_HEADER, "REQ-V3-OBS-1"))
                .andExpect(status().isOk())
                .andExpect(header().string(ObservabilityConstants.REQUEST_ID_HEADER, "REQ-V3-OBS-1"));
    }
}
