package com.example.order.service;

import com.example.order.domain.OrderCreatedEvent;
import com.example.order.service.handlers.OrderEventHandler;
import org.apache.pulsar.client.api.Consumer;
import org.apache.pulsar.client.api.Message;
import org.springframework.pulsar.annotation.PulsarListener;
import org.springframework.pulsar.listener.AckMode;
import org.springframework.pulsar.listener.Acknowledgement;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OrderEventConsumer {

    private final List<OrderEventHandler> handlers;

    public OrderEventConsumer(List<OrderEventHandler> handlers) {
        this.handlers = handlers;
    }

    /**
     * Режим MANUAL ack задаётся только в аннотации @PulsarListener(ackMode = AckMode.MANUAL) на методе-консьюмере.
     * Глобально такого нет (или не экспонировано)
     */
    @PulsarListener(subscriptionName = "order-group-sub", topics = "persistent://public/default/order-created", ackMode = AckMode.MANUAL)
    public void consume(OrderCreatedEvent event, Message<OrderCreatedEvent> message, Consumer<OrderCreatedEvent> consumer, Acknowledgement acknowledgement) {
        System.out.println("Pulsar: Получено событие: " + event);
        boolean success = true;

        for (OrderEventHandler handler : handlers) {
            if (handler.supports(event)) {
                try {
                    handler.handle(event);
                } catch (Exception e) {
                    System.err.println("Ошибка в обработчике " + handler.getClass().getSimpleName());
                    success = false;
                }
            }
        }

        if (success) {
            acknowledgement.acknowledge();
        } else {
            acknowledgement.nack();
        }
    }
}
