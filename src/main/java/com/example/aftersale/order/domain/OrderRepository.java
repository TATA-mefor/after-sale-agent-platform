package com.example.aftersale.order.domain;

import java.util.List;
import java.util.Optional;

public interface OrderRepository {

    Optional<Order> findById(String orderId);

    List<Order> findByUserId(String userId);
}
