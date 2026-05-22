package com.example.aftersale.ticket.api;

import com.example.aftersale.ticket.domain.IntentType;
import com.example.aftersale.ticket.domain.Ticket;
import com.example.aftersale.ticket.domain.TicketStatus;
import java.time.Instant;
/** 返回给前端的对象
 * API 返回对象，Controller 从领域模型 Ticket 转成它，再转成 JSON 返回给客户端。
 * 这里的 from() 方法是个简单的转换器，把领域模型的字段映射到 API 对象上。
 * 这样做的好处是：如果以后领域模型变了，或者我们不想暴露某些内部字段，我们只需要修改这个转换器，而不影响 Controller 或领域模型。
 */
public record TicketResponse(
        String ticketId,
        String userId,
        String orderId,
        String rawUserMessage,
        IntentType intentType,
        String priority,
        TicketStatus status,
        String internalNote,
        String agentSuggestion,
        Instant createdAt,
        Instant updatedAt) {

    public static TicketResponse from(Ticket ticket) {
        return new TicketResponse(
                ticket.getTicketId(),
                ticket.getUserId(),
                ticket.getOrderId(),
                ticket.getRawUserMessage(),
                ticket.getIntentType(),
                ticket.getPriority(),
                ticket.getStatus(),
                ticket.getInternalNote(),
                ticket.getAgentSuggestion(),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt());
    }
}
