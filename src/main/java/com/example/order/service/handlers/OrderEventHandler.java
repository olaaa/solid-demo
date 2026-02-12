package com.example.order.service.handlers;

import com.example.order.domain.OrderCreatedEvent;

public interface OrderEventHandler {
    void handle(OrderCreatedEvent event);
    
    default boolean supports(OrderCreatedEvent event) {
        return true;
    }
}
