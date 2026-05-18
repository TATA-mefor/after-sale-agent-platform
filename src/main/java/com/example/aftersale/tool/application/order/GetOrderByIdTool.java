package com.example.aftersale.tool.application.order;

import com.example.aftersale.order.application.OrderApplicationService;
import com.example.aftersale.order.domain.Order;
import com.example.aftersale.tool.application.ToolExecutor;
import com.example.aftersale.tool.domain.ToolDefinition;
import com.example.aftersale.tool.domain.ToolInput;
import com.example.aftersale.tool.domain.ToolOutput;
import com.example.aftersale.tool.domain.ToolRiskLevel;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class GetOrderByIdTool implements ToolExecutor {

    public static final String TOOL_NAME = "get_order_by_id";

    private static final ToolDefinition DEFINITION = ToolDefinition.of(
            TOOL_NAME,
            "Get one demo order by orderId.",
            "{\"orderId\":\"string\"}",
            "{\"orderId\":\"string\",\"userId\":\"string\",\"productId\":\"string\",\"productName\":\"string\","
                    + "\"orderStatus\":\"string\",\"paidAmount\":\"number\",\"paidAt\":\"string\","
                    + "\"deliveredAt\":\"string|null\",\"aftersaleDeadline\":\"string\","
                    + "\"whetherInAftersaleWindow\":\"boolean\",\"orderItems\":[{\"orderItemId\":\"string\","
                    + "\"productId\":\"string\",\"productName\":\"string\",\"category\":\"string\","
                    + "\"quantity\":\"number\",\"unitPrice\":\"number\",\"itemStatus\":\"string\","
                    + "\"supportReturn\":\"boolean\",\"supportExchange\":\"boolean\",\"isSpecialItem\":\"boolean\"}]}",
            ToolRiskLevel.LOW);

    private final OrderApplicationService orderApplicationService;

    @SuppressFBWarnings(
            value = "EI_EXPOSE_REP2",
            justification = "Spring constructor injection intentionally stores the application service dependency.")
    public GetOrderByIdTool(OrderApplicationService orderApplicationService) {
        this.orderApplicationService = orderApplicationService;
    }

    @Override
    public ToolDefinition definition() {
        return DEFINITION;
    }

    @Override
    public ToolOutput execute(ToolInput input) {
        Order order = orderApplicationService.getOrderById(input.requireString("orderId"));
        return ToolOutput.succeeded(TOOL_NAME, OrderToolOutput.toMap(order, Instant.now()));
    }
}
