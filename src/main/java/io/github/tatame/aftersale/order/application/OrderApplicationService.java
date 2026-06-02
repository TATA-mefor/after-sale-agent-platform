package io.github.tatame.aftersale.order.application;

import io.github.tatame.aftersale.common.exception.ResourceNotFoundException;
import io.github.tatame.aftersale.order.domain.Order;
import io.github.tatame.aftersale.order.domain.OrderRepository;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class OrderApplicationService {

    private final OrderRepository orderRepository;

    @SuppressFBWarnings(
            value = "EI_EXPOSE_REP2",
            justification = "Spring constructor injection intentionally stores the repository interface dependency.")
    public OrderApplicationService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public Order getOrderById(String orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "ORDER_NOT_FOUND",
                        "Order not found: " + orderId));
    }

    public List<Order> getUserOrders(String userId) {
        return orderRepository.findByUserId(userId);
    }
}
