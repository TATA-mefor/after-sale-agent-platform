package com.example.aftersale;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.aftersale.tool.application.ToolRegistry;
import com.example.aftersale.tool.domain.ToolExecutionStatus;
import com.example.aftersale.tool.domain.ToolInput;
import com.example.aftersale.tool.domain.ToolOutput;
import com.example.aftersale.tool.domain.ToolRiskLevel;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class OrderToolTest {

    @Autowired
    private ToolRegistry toolRegistry;

    @Test
    void getOrderByIdReturnsDemoOrderFacts() {
        ToolOutput output = toolRegistry.execute("get_order_by_id", ToolInput.of(Map.of(
                "orderId", "O202605130001")));

        assertThat(output.status()).isEqualTo(ToolExecutionStatus.SUCCEEDED);
        assertThat(output.data())
                .containsEntry("orderId", "O202605130001")
                .containsEntry("userId", "U-DEMO-1")
                .containsEntry("productId", "P-HEADPHONE-001")
                .containsEntry("productName", "Wireless Headphones")
                .containsEntry("orderStatus", "DELIVERED")
                .containsEntry("whetherInAftersaleWindow", true);
        assertThat(output.data()).containsKeys("paidAmount", "paidAt", "deliveredAt", "aftersaleDeadline");
        assertThat(output.data().get("orderItems")).isInstanceOf(List.class);
        List<?> orderItems = (List<?>) output.data().get("orderItems");
        assertThat(orderItems).isNotEmpty();
        Map<?, ?> orderItem = (Map<?, ?>) orderItems.get(0);
        assertThat(orderItem.get("orderItemId")).isEqualTo("OI-O202605130001-1");
        assertThat(orderItem.get("productId")).isEqualTo("P-HEADPHONE-001");
        assertThat(orderItem.get("productName")).isEqualTo("Wireless Headphones");
        assertThat(orderItem.get("category")).isEqualTo("电子数码");
        assertThat(orderItem.get("quantity")).isEqualTo(1);
        assertThat(orderItem.get("itemStatus")).isEqualTo("DELIVERED");
        assertThat(orderItem.get("supportReturn")).isEqualTo(true);
        assertThat(orderItem.get("supportExchange")).isEqualTo(true);
        assertThat(orderItem.get("isSpecialItem")).isEqualTo(false);
    }

    @Test
    void getOrderByIdToolOutputContainsStructuredOrderItems() {
        ToolOutput output = toolRegistry.execute("get_order_by_id", ToolInput.of(Map.of(
                "orderId", "O-SPECIAL-GOODS")));

        assertThat(output.status()).isEqualTo(ToolExecutionStatus.SUCCEEDED);
        assertThat(output.data()).containsKey("orderItems");
        List<?> orderItems = (List<?>) output.data().get("orderItems");
        assertThat(orderItems).hasSize(1);
        Map<?, ?> orderItem = (Map<?, ?>) orderItems.get(0);
        assertThat(orderItem.get("productId")).isEqualTo("P-CUSTOM-001");
        assertThat(orderItem.get("category")).isEqualTo("特殊商品");
        assertThat(orderItem.get("supportReturn")).isEqualTo(false);
        assertThat(orderItem.get("supportExchange")).isEqualTo(false);
        assertThat(orderItem.get("isSpecialItem")).isEqualTo(true);
    }

    @Test
    void getOrderByIdReturnsClearFailureForMissingOrder() {
        ToolOutput output = toolRegistry.execute("get_order_by_id", ToolInput.of(Map.of(
                "orderId", "O-MISSING-V2-2")));

        assertThat(output.status()).isEqualTo(ToolExecutionStatus.FAILED);
        assertThat(output.errorCode()).isEqualTo("TOOL_EXECUTION_FAILED");
        assertThat(output.message()).contains("Order not found: O-MISSING-V2-2");
    }

    @Test
    void getUserOrdersReturnsStructuredOrderList() {
        ToolOutput output = toolRegistry.execute("get_user_orders", ToolInput.of(Map.of(
                "userId", "U-DEMO-ORDER")));

        assertThat(output.status()).isEqualTo(ToolExecutionStatus.SUCCEEDED);
        assertThat(output.data()).containsKey("orders");
        Object orders = output.data().get("orders");
        assertThat(orders).isInstanceOf(List.class);
        List<String> orderIds = ((List<?>) orders).stream()
                .map(order -> (String) ((Map<?, ?>) order).get("orderId"))
                .toList();
        assertThat(orderIds)
                .contains("O-PAID-NOT-SHIPPED", "O-EXPIRED-AFTERSALE", "O-SPECIAL-GOODS");
    }

    @Test
    void orderToolsAreLowRiskAndAvailableThroughToolRegistry() {
        assertThat(toolRegistry.findDefinition("get_order_by_id"))
                .hasValueSatisfying(definition -> {
                    assertThat(definition.riskLevel()).isEqualTo(ToolRiskLevel.LOW);
                    assertThat(definition.requiresApproval()).isFalse();
                });
        assertThat(toolRegistry.findDefinition("get_user_orders"))
                .hasValueSatisfying(definition -> {
                    assertThat(definition.riskLevel()).isEqualTo(ToolRiskLevel.LOW);
                    assertThat(definition.requiresApproval()).isFalse();
                });
    }
}
