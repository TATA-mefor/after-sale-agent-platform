package com.example.aftersale;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
                .andExpect(jsonPath("$.code").value("TICKET_NOT_FOUND"))
                .andExpect(jsonPath("$.message", containsString("T-MISSING-M4")));
    }
}
