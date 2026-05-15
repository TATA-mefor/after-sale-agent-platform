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
