package com.company.orders.infrastructure.adapter;

import com.company.orders.application.port.PaymentPort;
import com.company.orders.domain.Order;
import org.springframework.stereotype.Component;

@Component
public class RestPaymentAdapter implements PaymentPort {

    @Override
    public void pay(Order order) {
        order.markPaid();
    }
}