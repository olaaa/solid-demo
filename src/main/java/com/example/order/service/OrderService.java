package com.example.order.service;

import com.example.order.domain.OrderCreatedEvent;
import com.example.order.producer.OrderEventProducer;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
public class OrderService {

    private final OrderEventProducer eventProducer;

    public OrderService(OrderEventProducer eventProducer) {
        this.eventProducer = eventProducer;
    }

    public String createOrder(String customerId, String productId, int quantity, BigDecimal price) {
        String orderId = UUID.randomUUID().toString();
        BigDecimal total = price.multiply(BigDecimal.valueOf(quantity));

        OrderCreatedEvent event = new OrderCreatedEvent(
            orderId,
            customerId,
            productId,
            quantity,
            total,
            Instant.now()
        );

        eventProducer.publish(event);
        return orderId;
    }
}
