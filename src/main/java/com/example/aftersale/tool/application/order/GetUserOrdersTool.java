package com.example.aftersale.tool.application.order;

import com.example.aftersale.order.application.OrderApplicationService;
import com.example.aftersale.tool.application.ToolExecutor;
import com.example.aftersale.tool.domain.ToolDefinition;
import com.example.aftersale.tool.domain.ToolInput;
import com.example.aftersale.tool.domain.ToolOutput;
import com.example.aftersale.tool.domain.ToolRiskLevel;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class GetUserOrdersTool implements ToolExecutor {

    public static final String TOOL_NAME = "get_user_orders";

    private static final ToolDefinition DEFINITION = ToolDefinition.of(
            TOOL_NAME,
            "List demo orders for one user.",
            "{\"userId\":\"string\"}",
            "{\"orders\":[{\"orderId\":\"string\",\"userId\":\"string\",\"productId\":\"string\","
                    + "\"productName\":\"string\",\"orderStatus\":\"string\",\"paidAmount\":\"number\","
                    + "\"paidAt\":\"string\",\"deliveredAt\":\"string|null\",\"aftersaleDeadline\":\"string\","
                    + "\"whetherInAftersaleWindow\":\"boolean\"}]}",
            ToolRiskLevel.LOW);

    private final OrderApplicationService orderApplicationService;

    @SuppressFBWarnings(
            value = "EI_EXPOSE_REP2",
            justification = "Spring constructor injection intentionally stores the application service dependency.")
    public GetUserOrdersTool(OrderApplicationService orderApplicationService) {
        this.orderApplicationService = orderApplicationService;
    }

    @Override
    public ToolDefinition definition() {
        return DEFINITION;
    }

    @Override
    public ToolOutput execute(ToolInput input) {
        Instant checkedAt = Instant.now();
        return ToolOutput.succeeded(TOOL_NAME, Map.of(
                "orders", orderApplicationService.getUserOrders(input.requireString("userId")).stream()
                        .map(order -> OrderToolOutput.toMap(order, checkedAt))
                        .toList()));
    }
}
