package com.example.aftersale.ticket.api;

import com.example.aftersale.common.api.ApiResponse;
import com.example.aftersale.ticket.application.TicketApplicationService;
import com.example.aftersale.ticket.domain.IntentType;
import com.example.aftersale.ticket.domain.Ticket;
import com.example.aftersale.ticket.domain.TicketPage;
import com.example.aftersale.ticket.domain.TicketStatus;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    @GetMapping
    @Operation(
            summary = "List tickets with pagination",
            description = "Returns a bounded, read-only ticket page. Supported filters are status, userId, orderId, "
                    + "intentType, createdFrom, and createdTo. This endpoint does not create AgentRun records.")
    public ApiResponse<TicketPageResponse> listTickets(
            @Parameter(description = "Zero-based page index.", example = "0")
            @RequestParam(required = false) Integer page,
            @Parameter(description = "Page size from 1 to 100.", example = "20")
            @RequestParam(required = false) Integer size,
            @Parameter(description = "Sort format: createdAt,desc; updatedAt,asc; or ticketId,asc.",
                    example = "createdAt,desc")
            @RequestParam(required = false) String sort,
            @Parameter(description = "Optional ticket status filter.", example = "CREATED")
            @RequestParam(required = false) TicketStatus status,
            @Parameter(description = "Optional demo user id filter.", example = "U-DEMO-1001")
            @RequestParam(required = false) String userId,
            @Parameter(description = "Optional demo order id filter.", example = "O-DEMO-2001")
            @RequestParam(required = false) String orderId,
            @Parameter(description = "Optional after-sale intent filter.", example = "RETURN_AND_REFUND")
            @RequestParam(required = false) IntentType intentType,
            @Parameter(description = "Optional inclusive creation lower bound.", example = "2026-01-01T00:00:00Z")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdFrom,
            @Parameter(description = "Optional inclusive creation upper bound.", example = "2026-12-31T23:59:59Z")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdTo) {
        TicketPage ticketPage = ticketApplicationService.listTickets(
                page, size, sort, status, userId, orderId, intentType, createdFrom, createdTo);
        return ApiResponse.success(TicketPageResponse.from(ticketPage));
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
