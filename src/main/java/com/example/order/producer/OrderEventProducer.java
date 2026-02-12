package com.example.order.producer;

import org.springframework.pulsar.core.PulsarTemplate;
import org.springframework.stereotype.Component;

@Component
public class OrderEventProducer {

    private final PulsarTemplate<String> pulsarTemplate;

    public OrderEventProducer(PulsarTemplate<String> pulsarTemplate) {
        this.pulsarTemplate = pulsarTemplate;
    }

    public void publish(String topic, String payload) {
        pulsarTemplate.send(topic, payload);
    }
}
