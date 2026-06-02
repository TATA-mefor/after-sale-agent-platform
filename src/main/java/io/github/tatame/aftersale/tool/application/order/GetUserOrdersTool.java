package io.github.tatame.aftersale.tool.application.order;

import io.github.tatame.aftersale.order.application.OrderApplicationService;
import io.github.tatame.aftersale.tool.application.ToolExecutor;
import io.github.tatame.aftersale.tool.domain.ToolDefinition;
import io.github.tatame.aftersale.tool.domain.ToolInput;
import io.github.tatame.aftersale.tool.domain.ToolOutput;
import io.github.tatame.aftersale.tool.domain.ToolRiskLevel;
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
                    + "\"whetherInAftersaleWindow\":\"boolean\",\"orderItems\":\"array\"}]}",
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
