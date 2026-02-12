package com.company.orders.application;

import com.company.orders.application.port.PaymentPort;
import com.company.orders.application.port.NotificationPort;
import com.company.orders.domain.Order;
import com.company.orders.infrastructure.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private final OrderRepository repository;
    private final PaymentPort paymentPort;
    private final NotificationPort notificationPort;

    public OrderService(OrderRepository repository,
                        PaymentPort paymentPort,
                        NotificationPort notificationPort) {
        this.repository = repository;
        this.paymentPort = paymentPort;
        this.notificationPort = notificationPort;
    }

    @Transactional
    public Order create(Order order) {
        repository.save(order);
        paymentPort.pay(order);
        notificationPort.notify(order);
        return order;
    }
}