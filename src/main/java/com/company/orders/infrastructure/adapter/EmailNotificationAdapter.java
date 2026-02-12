package com.company.orders.infrastructure.adapter;

import com.company.orders.application.port.NotificationPort;
import com.company.orders.domain.Order;
import org.springframework.stereotype.Component;

@Component
public class EmailNotificationAdapter implements NotificationPort {

    @Override
    public void notify(Order order) {
        // send email
    }
}