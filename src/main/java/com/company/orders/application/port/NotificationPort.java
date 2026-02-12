package com.company.orders.application.port;

import com.company.orders.domain.Order;

public interface NotificationPort {
    void notify(Order order);
}