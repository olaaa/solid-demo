package com.example.order.service.handlers;

import com.example.order.domain.OrderCreatedEvent;
import org.springframework.stereotype.Component;

@Component
public class PaymentInitiationHandler implements OrderEventHandler {

    @Override
    public void handle(OrderCreatedEvent event) {
        System.out.println("Pulsar: Инициирую оплату на сумму " + event.totalAmount() + " для заказа " + event.orderId());
    }
}
