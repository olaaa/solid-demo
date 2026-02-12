package com.company.orders.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email;
    private BigDecimal amount;
    private String status;

    protected Order() {}

    public Order(String email, BigDecimal amount) {
        this.email = email;
        this.amount = amount;
        this.status = "NEW";
    }

    public void markPaid() {
        this.status = "PAID";
    }

    public BigDecimal amount() { return amount; }
}