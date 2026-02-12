# Orders Service – SOLID Production Example (Spring Boot 4.0.2)

This project demonstrates all SOLID principles in a production-like Spring Boot microservice.

---

# 1️⃣ SRP — Single Responsibility Principle

A class should have only one reason to change.

❌ Violation (GodService):
- Handles validation
- Payment call
- Persistence
- Email
- Transaction management

If payment logic changes → service changes.
If notification changes → service changes.

✅ Fix:
- OrderService (orchestration)
- PaymentPort
- NotificationPort
- Repository
Each class changes for one reason only.

---

# 2️⃣ OCP — Open/Closed Principle

Open for extension, closed for modification.

❌ Violation:
if(paymentType.equals("CARD")) {...}
else if("PAYPAL") {...}

Adding new payment → modify existing class.

✅ Fix:
PaymentStrategy interface
Spring injects Map<String, PaymentStrategy>
New payment = new bean. No modification.

---

# 3️⃣ LSP — Liskov Substitution Principle

Subtypes must preserve behavioral contracts.

❌ Violation:
SpecialOrder overrides calculateTotal() breaking discount contract.

Clients relying on base behavior break.

✅ Fix:
Separate abstractions.
Use composition over inheritance.
Keep invariants inside aggregate root.

---

# 4️⃣ ISP — Interface Segregation Principle

Clients must not depend on methods they don’t use.

❌ Violation:
FatExternalClient with 12 methods.
Services forced to implement unused ones.

✅ Fix:
Split into PaymentClient, ShippingClient, NotificationClient.

---

# 5️⃣ DIP — Dependency Inversion Principle

High-level modules must depend on abstractions.

❌ Violation:
OrderService directly creates RestClient.

Hard to test.
Hard to replace transport.

✅ Fix:
OrderService depends on PaymentPort.
Adapter implements PaymentPort using RestClient.
Test uses FakePaymentAdapter.

---

# Architecture

Hexagonal Architecture:
- domain
- application
- ports
- adapters
- infrastructure

OrderService = application layer
PaymentPort = output port
RestPaymentAdapter = adapter
JpaOrderRepository = adapter

---

# What Interviewers Expect at Senior Level

- Ability to detect God Services
- Explain transaction boundaries
- Explain why DIP improves testability
- Explain why inheritance breaks LSP
- Know when NOT to apply SOLID blindly

---

Run:
mvn spring-boot:run


2. Оба типа модулей должны зависеть от абстракций (интерфейсов/абстрактных классов).
3. Абстракции не должны зависеть от деталей. Детали (конкретные реализации) должны зависеть от абстракций.
   Классика — Dependency Injection (через конструктор, setter, интерфейс). Вместо new MySQLRepository() → Repository repo (интерфейс), который приходит извне. Это даёт гибкость, тестируемость и возможность менять реализации без изменения бизнес-логики.