
Принципы остаются теми же:

- **OCP** — открытость для расширения: новый обработчик события добавляется без изменения основного кода
- **DIP** — зависимость от абстракций (интерфейсов)
- **Event-driven** (E из IDEALS) — асинхронное общение через события (теперь через Pulsar topics и subscriptions)

### Сценарий остаётся тем же

Микросервис **Order Service** создаёт заказ → публикует событие `OrderCreatedEvent`.  
Несколько независимых обработчиков реагируют:

- Резервирование товара
- Инициирование оплаты
- Отправка уведомления

Новый обработчик (например, начисление бонусов) добавляется просто новым классом.

### Структура проекта (упрощённо)

```
src/main/java/com/example/order
├── config
│   └── PulsarConfig.java          (если нужно кастомизировать)
├── domain
│   └── OrderCreatedEvent.java
├── service
│   ├── OrderService.java
│   └── handlers
│       ├── OrderEventHandler.java       // интерфейс (DIP + OCP)
│       ├── InventoryReservationHandler.java
│       ├── PaymentInitiationHandler.java
│       └── NotificationHandler.java
└── producer
    └── OrderEventProducer.java
```

### 1. Событие (DTO)

```java
// OrderCreatedEvent.java
package com.example.order.domain;

import java.math.BigDecimal;
import java.time.Instant;

public record OrderCreatedEvent(
    String orderId,
    String customerId,
    String productId,
    int quantity,
    BigDecimal totalAmount,
    Instant createdAt
) {}
```

### 2. Интерфейс обработчика (DIP + OCP)

```java
// OrderEventHandler.java
package com.example.order.service.handlers;

import com.example.order.domain.OrderCreatedEvent;

public interface OrderEventHandler {
    void handle(OrderCreatedEvent event);
    
    default boolean supports(OrderCreatedEvent event) {
        return true;
    }
}
```

### 3. Производитель события (PulsarTemplate)

```java
// OrderEventProducer.java
package com.example.order.producer;

import com.example.order.domain.OrderCreatedEvent;
import org.springframework.pulsar.core.PulsarTemplate;
import org.springframework.stereotype.Component;

@Component
public class OrderEventProducer {

    private final PulsarTemplate<OrderCreatedEvent> pulsarTemplate;

    public OrderEventProducer(PulsarTemplate<OrderCreatedEvent> pulsarTemplate) {
        this.pulsarTemplate = pulsarTemplate;
    }

    public void publish(OrderCreatedEvent event) {
        // persistent://public/default/order-created — типичный формат persistent topic в Pulsar
        pulsarTemplate.send("persistent://public/default/order-created", event);
    }
}
```

### 4. Основной сервис заказа

```java
// OrderService.java
package com.example.order.service;

import com.example.order.domain.OrderCreatedEvent;
import com.example.order.producer.OrderEventProducer;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
public class OrderService {

    private final OrderEventProducer eventProducer;

    public OrderService(OrderEventProducer eventProducer) {
        this.eventProducer = eventProducer;
    }

    public String createOrder(String customerId, String productId, int quantity, BigDecimal price) {
        String orderId = UUID.randomUUID().toString();
        BigDecimal total = price.multiply(BigDecimal.valueOf(quantity));

        OrderCreatedEvent event = new OrderCreatedEvent(
            orderId,
            customerId,
            productId,
            quantity,
            total,
            Instant.now()
        );

        eventProducer.publish(event);
        return orderId;
    }
}
```

### 5. Обработчики событий (каждый — отдельный @Component)

```java
// InventoryReservationHandler.java
@Component
public class InventoryReservationHandler implements OrderEventHandler {

    @Override
    public void handle(OrderCreatedEvent event) {
        System.out.println("Pulsar: Резервирую товар " + event.productId() + " × " + event.quantity());
    }
}
```

```java
// PaymentInitiationHandler.java
@Component
public class PaymentInitiationHandler implements OrderEventHandler {

    @Override
    public void handle(OrderCreatedEvent event) {
        System.out.println("Pulsar: Инициирую оплату на сумму " + event.totalAmount() + " для заказа " + event.orderId());
    }
}
```

### 6. Потребитель (PulsarListener + распределение по обработчикам)

```java
// OrderEventConsumer.java
package com.example.order.service;

import com.example.order.domain.OrderCreatedEvent;
import com.example.order.service.handlers.OrderEventHandler;
import org.springframework.pulsar.annotation.PulsarListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OrderEventConsumer {

    private final List<OrderEventHandler> handlers;

    public OrderEventConsumer(List<OrderEventHandler> handlers) {
        this.handlers = handlers;
    }

    @PulsarListener(subscriptionName = "order-group-sub", topics = "persistent://public/default/order-created")
    public void consume(OrderCreatedEvent event) {
        System.out.println("Pulsar: Получено событие: " + event);

        for (OrderEventHandler handler : handlers) {
            if (handler.supports(event)) {
                try {
                    handler.handle(event);
                } catch (Exception e) {
                    System.err.println("Ошибка в обработчике " + handler.getClass().getSimpleName());
                    // В продакшене: nack, отрицательное подтверждение, retry, dead-letter и т.д.
                }
            }
        }
    }
}
```

### 7. Зависимости в pom.xml (основные)

```xml
<dependencies>
    <!-- Spring Boot Pulsar Starter -->
    <dependency>
        <groupId>org.springframework.pulsar</groupId>
        <artifactId>spring-boot-starter-pulsar</artifactId>
    </dependency>
    
    <!-- Для JSON сериализации событий (рекомендуется) -->
    <dependency>
        <groupId>org.springframework.pulsar</groupId>
        <artifactId>spring-pulsar-spring-json</artifactId>
    </dependency>
</dependencies>
```

### 8. Конфигурация в application.yml (минимальная)

```yaml
spring:
  pulsar:
    client:
      service-url: pulsar://localhost:6650          # или pulsar+ssl:// для продакшена
      # authentication: ... (token, tls и т.д. при необходимости)
    defaults:
      type-mappings:
        - message-type: com.example.order.domain.OrderCreatedEvent
          schema-info:
            schema-type: JSON
```

### Как это реализует принципы (то же самое, но с Pulsar)

- **DIP**: `OrderService` зависит от абстракции `OrderEventProducer`.  
  `OrderEventConsumer` получает список интерфейсов `OrderEventHandler` через конструктор (Spring их собирает автоматически).

- **OCP**: Новый обработчик — просто новый `@Component`, реализующий `OrderEventHandler`.  
  Spring Boot + PulsarListener автоматически подхватывают все bean'ы. Ничего в `OrderService` или `OrderEventConsumer` менять не нужно.

- **Event-driven**: Полностью асинхронно через Pulsar.  
  Преимущества Pulsar по сравнению с Kafka в этом сценарии:
    - встроенная поддержка multi-tenancy и namespaces
    - гибкие подписки (exclusive, shared, failover)
    - tiered storage (можно хранить старые события дешево)
    - Kafka-совместимый API (если нужно мигрировать позже)

В production рекомендуется:

- Использовать **transactional outbox** (чтобы событие гарантированно ушло после сохранения в БД)
- Настроить **acknowledgment** (MANUAL или отрицательное подтверждение при ошибках)
- Dead-letter topics
- Schema evolution (Pulsar отлично поддерживает)
- Tracing (Micrometer + OpenTelemetry)

Если нужно — могу добавить пример с **@PulsarListener** на отдельные обработчики (без центрального consumer-класса) или с **Pulsar Functions** / **Spring Cloud Stream** binder для Pulsar. Удачи на собеседовании!