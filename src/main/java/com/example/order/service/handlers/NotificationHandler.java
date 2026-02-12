package com.example.order.service.handlers;

import com.example.order.domain.OrderCreatedEvent;
import org.springframework.stereotype.Component;

@Component
public class NotificationHandler implements OrderEventHandler {

    @Override
    public void handle(OrderCreatedEvent event) {
        System.out.println("Pulsar: Отправляю уведомление для клиента " + event.customerId() + " по заказу " + event.orderId());
    }
}
