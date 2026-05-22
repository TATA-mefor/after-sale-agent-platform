package com.example.aftersale.ticket.api;

import com.example.aftersale.common.api.ApiResponse;
import com.example.aftersale.ticket.application.TicketApplicationService;
import com.example.aftersale.ticket.domain.Ticket;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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
public class TicketController {

    private final TicketApplicationService ticketApplicationService;

    @SuppressFBWarnings(
            value = "EI_EXPOSE_REP2",
            justification = "Spring constructor injection intentionally stores the application service dependency.")
    public TicketController(TicketApplicationService ticketApplicationService) {
        this.ticketApplicationService = ticketApplicationService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TicketResponse>> createTicket(@RequestBody TicketCreateRequest request) {
        Ticket ticket = ticketApplicationService.createTicket(request.userId(), request.orderId(), request.message());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(TicketResponse.from(ticket)));
    }

    @GetMapping("/{ticketId}")
    public ApiResponse<TicketResponse> getTicket(@PathVariable String ticketId) {
        Ticket ticket = ticketApplicationService.getTicket(ticketId);
        return ApiResponse.success(TicketResponse.from(ticket));
    }
}
