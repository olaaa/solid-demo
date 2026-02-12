package com.example.order.service;

import com.example.order.domain.OutboxEvent;
import com.example.order.repository.OutboxRepository;
import com.example.order.producer.OrderEventProducer;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class OutboxProcessor {

    private final OutboxRepository outboxRepository;
    private final OrderEventProducer eventProducer;

    public OutboxProcessor(OutboxRepository outboxRepository, OrderEventProducer eventProducer) {
        this.outboxRepository = outboxRepository;
        this.eventProducer = eventProducer;
    }

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void processOutbox() {
        List<OutboxEvent> events = outboxRepository.findByProcessedFalse();
        for (OutboxEvent event : events) {
            try {
                eventProducer.publish(event.getTopic(), event.getPayload());
                event.setProcessed(true);
                outboxRepository.save(event);
            } catch (Exception e) {
                System.err.println("Error sending event from outbox: " + e.getMessage());
            }
        }
    }
}
