package com.example.aftersale.ticket.api;

import com.example.aftersale.common.api.ApiResponse;
import com.example.aftersale.ticket.application.TicketApplicationService;
import com.example.aftersale.ticket.domain.Ticket;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 处理工单相关的 HTTP 请求。
 *
 * <p>这个类只负责接收请求和组装响应；真正创建工单、保存工单的逻辑在 TicketApplicationService 后面。
 */
@RestController
@RequestMapping("/api/tickets")
@Tag(name = "Tickets", description = "Ticket intake and read APIs for after-sale issues.")
public class TicketController {

    private final TicketApplicationService ticketApplicationService;

    @SuppressFBWarnings(
            value = "EI_EXPOSE_REP2",
            justification = "Spring constructor injection intentionally stores the application service dependency.")
    public TicketController(TicketApplicationService ticketApplicationService) {
        this.ticketApplicationService = ticketApplicationService;
    }

    @PostMapping
    @Operation(
            summary = "Create an after-sale ticket",
            description = "Creates a local after-sale ticket from demo input. This does not execute refunds, "
                    + "exchanges, compensation, payment, logistics, or dispute closure.")
    public ResponseEntity<ApiResponse<TicketResponse>> createTicket(@RequestBody TicketCreateRequest request) {
        Ticket ticket = ticketApplicationService.createTicket(request.userId(), request.orderId(), request.message());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(TicketResponse.from(ticket)));
    }

    @GetMapping("/{ticketId}")
    @Operation(summary = "Get a ticket", description = "Returns an existing ticket by id without running the Agent.")
    public ApiResponse<TicketResponse> getTicket(
            @Parameter(description = "Ticket id returned by create ticket.", example = "T-DEMO-1001")
            @PathVariable String ticketId) {
        Ticket ticket = ticketApplicationService.getTicket(ticketId);
        return ApiResponse.success(TicketResponse.from(ticket));
    }
}
