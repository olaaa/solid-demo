
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

### Как это реализует принципы

- **DIP**: `OutboxProcessor` зависит от абстракции `OrderEventProducer`.  
  `OrderEventConsumer` получает список интерфейсов `OrderEventHandler` через конструктор (Spring их собирает автоматически).

- **OCP**: Новый обработчик — просто новый `@Component`, реализующий `OrderEventHandler`.  
  Spring Boot + PulsarListener автоматически подхватывают все bean'ы. Ничего в `OrderService` или `OrderEventConsumer` менять не нужно.

- **Event-driven**: Полностью асинхронно через Pulsar.  
  Преимущества Pulsar по сравнению с Kafka в этом сценарии:
    - встроенная поддержка multi-tenancy и namespaces
    - гибкие подписки (exclusive, shared, failover)
    - tiered storage (можно хранить старые события дешево)
    - Kafka-совместимый API (если нужно мигрировать позже)

В production реализовано:

- **Transactional outbox**: события сначала сохраняются в БД (таблица `outbox_events`) в одной транзакции с бизнес-логикой, а затем отдельный процесс `OutboxProcessor` отправляет их в Pulsar.
- **Acknowledgment**: настроен `MANUAL` режим подтверждения. Если хотя бы один обработчик завершается с ошибкой, вызывается `nack()`, что инициирует повторную доставку.
- **Dead-letter topics**: после 3 неудачных попыток событие отправляется в `order-created-dlq`.
- **Schema evolution**: настроена стратегия `ALWAYS_COMPATIBLE` для обеспечения совместимости схем при изменениях.
- **Tracing**: интегрированы Micrometer Tracing и OpenTelemetry (настроено через `application.yml` и зависимости в `pom.xml`).

В проект внесены следующие изменения для обеспечения надежности и наблюдаемости:

### 1. Transactional Outbox
- Добавлена сущность `OutboxEvent` и репозиторий `OutboxRepository` (Spring Data JPA).
- `OrderService` теперь сохраняет события в базу данных (H2) в рамках транзакции, вместо прямой отправки в Pulsar.
- Реализован `OutboxProcessor`, который периодически (`@Scheduled`) считывает необработанные события из БД и отправляет их в Pulsar, гарантируя доставку "at-least-once".

### 2. Acknowledgment & Dead-letter Topics
- В `application.yml` настроен режим подтверждения `manual`.
- В `OrderEventConsumer` добавлена логика обработки подтверждений:
  - `acknowledgement.acknowledge()` вызывается при успешной обработке всеми хендлерами.
  - `acknowledgement.nack()` вызывается при возникновении ошибок, что приводит к повторной доставке.
- Настроена `dead-letter-policy`: после 3 неудачных попыток событие перемещается в топик `order-created-dlq`.

### 3. Schema Evolution
- В конфигурацию Pulsar добавлена стратегия `ALWAYS_COMPATIBLE` для управления эволюцией схем.

### 4. Tracing (Micrometer + OpenTelemetry)
- В `pom.xml` добавлены зависимости для Micrometer Tracing и OpenTelemetry.
- В `application.yml` включено сэмплирование трасс (100%) и настроен экспорт данных в формате OTLP.

### 5. Обновление документации
- `README.md` обновлен: раздел "рекомендуется" заменен на описание реализованной функциональности.

Все изменения соответствуют принципам SOLID и лучшим практикам построения событийно-ориентированных систем.