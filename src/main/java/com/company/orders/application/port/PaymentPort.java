package com.company.orders.application.port;

import com.company.orders.domain.Order;

public interface PaymentPort {
    void pay(Order order);
}