package io.github.tatame.aftersale.tool.application.order;

import io.github.tatame.aftersale.order.application.OrderApplicationService;
import io.github.tatame.aftersale.order.domain.Order;
import io.github.tatame.aftersale.tool.application.ToolExecutor;
import io.github.tatame.aftersale.tool.domain.ToolDefinition;
import io.github.tatame.aftersale.tool.domain.ToolInput;
import io.github.tatame.aftersale.tool.domain.ToolOutput;
import io.github.tatame.aftersale.tool.domain.ToolRiskLevel;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import org.springframework.stereotype.Component;

/**
 * 提供低风险订单事实查询工具，供 Agent 规划和 Handler 获取售后判断依据。
 *
 * <p>边界：工具只通过 OrderApplicationService 读取订单事实，不修改订单，也不直接访问订单 Repository。
 */
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
