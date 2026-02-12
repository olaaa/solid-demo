package com.example.order.service;

import com.example.order.domain.OrderCreatedEvent;
import com.example.order.service.handlers.OrderEventHandler;
import org.springframework.pulsar.annotation.PulsarListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OrderEventConsumer {

    private final List<OrderEventHandler> handlers;

    public OrderEventConsumer(List<OrderEventHandler> handlers) {
        this.handlers = handlers;
    }

    @PulsarListener(subscriptionName = "order-group-sub", topics = "persistent://public/default/order-created")
    public void consume(OrderCreatedEvent event) {
        System.out.println("Pulsar: Получено событие: " + event);

        for (OrderEventHandler handler : handlers) {
            if (handler.supports(event)) {
                try {
                    handler.handle(event);
                } catch (Exception e) {
                    System.err.println("Ошибка в обработчике " + handler.getClass().getSimpleName());
                }
            }
        }
    }
}
