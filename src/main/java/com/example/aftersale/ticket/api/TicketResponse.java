package com.example.aftersale.ticket.api;

import com.example.aftersale.ticket.domain.IntentType;
import com.example.aftersale.ticket.domain.Ticket;
import com.example.aftersale.ticket.domain.TicketStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
/** 返回给前端的对象
 * API 返回对象，Controller 从领域模型 Ticket 转成它，再转成 JSON 返回给客户端。
 * 这里的 from() 方法是个简单的转换器，把领域模型的字段映射到 API 对象上。
 * 这样做的好处是：如果以后领域模型变了，或者我们不想暴露某些内部字段，我们只需要修改这个转换器，而不影响 Controller 或领域模型。
 */
@Schema(description = "Ticket response. Suggestions and notes are evidence summaries, not completed business actions.")
public record TicketResponse(
        @Schema(description = "Ticket id.", example = "T-DEMO-1001")
        String ticketId,
        @Schema(description = "Demo user id.", example = "U-DEMO-1001")
        String userId,
        @Schema(description = "Demo order id.", example = "O-DEMO-2001")
        String orderId,
        @Schema(description = "Original customer message, kept short in demo data.")
        String rawUserMessage,
        @Schema(description = "Detected after-sale intent.")
        IntentType intentType,
        @Schema(description = "Ticket priority.", example = "NORMAL")
        String priority,
        @Schema(description = "Ticket lifecycle status.")
        TicketStatus status,
        @Schema(description = "Internal note assembled by the platform.")
        String internalNote,
        @Schema(description = "Agent suggestion based on tool evidence; not an executed business action.")
        String agentSuggestion,
        @Schema(description = "Creation time.")
        Instant createdAt,
        @Schema(description = "Last update time.")
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
