package com.example.order.service;

import com.example.order.domain.OrderCreatedEvent;
import com.example.order.domain.OutboxEvent;
import com.example.order.repository.OutboxRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
public class OrderService {

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public OrderService(OutboxRepository outboxRepository, ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
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

        try {
            String payload = objectMapper.writeValueAsString(event);
            OutboxEvent outboxEvent = new OutboxEvent("persistent://public/default/order-created", payload);
            outboxRepository.save(outboxEvent);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error serializing order event", e);
        }

        return orderId;
    }
}
