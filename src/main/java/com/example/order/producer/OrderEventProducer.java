package com.example.order.producer;

import com.example.order.domain.OrderCreatedEvent;
import org.springframework.pulsar.core.PulsarTemplate;
import org.springframework.stereotype.Component;

@Component
public class OrderEventProducer {

    private final PulsarTemplate<OrderCreatedEvent> pulsarTemplate;

    public OrderEventProducer(PulsarTemplate<OrderCreatedEvent> pulsarTemplate) {
        this.pulsarTemplate = pulsarTemplate;
    }

    public void publish(OrderCreatedEvent event) {
        pulsarTemplate.send("persistent://public/default/order-created", event);
    }
}
