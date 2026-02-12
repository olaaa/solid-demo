package com.example.order.service.handlers;

import com.example.order.domain.OrderCreatedEvent;
import org.springframework.stereotype.Component;

@Component
public class InventoryReservationHandler implements OrderEventHandler {

    @Override
    public void handle(OrderCreatedEvent event) {
        System.out.println("Pulsar: Резервирую товар " + event.productId() + " × " + event.quantity());
    }
}
