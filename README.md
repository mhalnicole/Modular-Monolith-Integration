# Modular Monolith Integration — Lab 2: Extending the Modular Monolith

**Course/Lab:** Lab 2: Extending the Modular Monolith  
**Stack:** Java Spring Boot (v4 / Java 17+) + React (Vite) + Supabase (PostgreSQL)  
**Package:** `edu.cit.patonog` (`.shop`, `.inventory`, `.notification`, `.events`)

---

## 📌 Overview & Architecture

Lab 2 extends our modular monolith from Lab 1 by introducing:
1. **Multi-Item Orders with Transactional Rollback:** All items are validated before reserving. If any item lacks stock, zero items are reserved (all-or-nothing).
2. **Order Cancellation & Restock:** Reverses module calls by updating status to `CANCELLED` and restocking inventory items via `InventoryService.restock()`.
3. **In-Monolith Domain Events & Notification Module:** `OrderService` and `InventoryService` publish domain events (`OrderPlacedEvent`, `OrderRejectedEvent`, `LowStockEvent`) through Spring's `ApplicationEventPublisher`. The new `Notification` module listens to these events via `@EventListener` with zero direct coupling to the other services.
4. **Low-Stock Auto-Reorder Alerts:** Automatically triggers a `LowStockEvent` whenever an item's stock drops to 5 or below after reservation.
5. **Live Dashboard & Cart:** React frontend featuring a multi-item cart, live inventory dashboard with low-stock badges, order history with cancel buttons, and an event activity feed.

```
+-----------------------------------------------------------------------------------+
|                                  React Frontend                                   |
|                              (http://localhost:5173)                              |
+-----------------------------------------+-----------------------------------------+
                                          |
                                          | HTTP REST (CORS enabled)
                                          v
+-----------------------------------------------------------------------------------+
|                      Spring Boot Monolith (In-Process JVM)                        |
|                                                                                   |
|   +--------------------------+           +------------------------------------+   |
|   |   edu.cit.patonog.shop   | --------> |     edu.cit.patonog.inventory      |   |
|   |      (Order Module)      | In-Process|         (Inventory Module)         |   |
|   |                          |           |                                    |   |
|   |   OrderService           |           |   InventoryService (Public)        |   |
|   |   OrderRepository        |           |   InventoryServiceImpl (Private)   |   |
|   |   OrderController        |           |   InventoryRepository              |   |
|   +------------+-------------+           +-----------------+------------------+   |
|                |                                           |                      |
|                | ApplicationEventPublisher                 | EventPublisher       |
|                v                                           v                      |
|      [OrderPlacedEvent / OrderRejectedEvent]        [LowStockEvent]               |
|                \                                           /                      |
|                 +-------------------+---------------------+                       |
|                                     | In-Process Event Bus                        |
|                                     v                                             |
|                     +-------------------------------+                             |
|                     | edu.cit.patonog.notification  |                             |
|                     |     (Notification Module)     |                             |
|                     |                               |                             |
|                     |  NotificationEventListener    |                             |
|                     |  NotificationRepository       |                             |
|                     |  NotificationController       |                             |
|                     +---------------+---------------+                             |
+-------------------------------------|---------------------------------------------+
                                      | Spring Data JPA (Shared Supabase Database)
                                      v
           +------------------------------------------------------+
           |                 Supabase PostgreSQL                  |
           |   inventory | orders | order_items | notifications   |
           +------------------------------------------------------+
```

---

## 📂 Project Structure

```
monolith/
├── src/
│   ├── main/
│   │   ├── java/edu/cit/patonog/
│   │   │   ├── ShopApplication.java            # @SpringBootApplication entry point
│   │   │   ├── config/
│   │   │   │   └── WebCorsConfig.java          # CORS config enabling frontend access
│   │   │   ├── events/                         # In-Monolith Domain Events
│   │   │   │   ├── OrderPlacedEvent.java       # Published when order is CONFIRMED
│   │   │   │   ├── OrderRejectedEvent.java     # Published when order is REJECTED
│   │   │   │   └── LowStockEvent.java          # Published when stock <= 5
│   │   │   ├── inventory/                      # Inventory Module
│   │   │   │   ├── InventoryItem.java          # JPA entity for 'inventory' table
│   │   │   │   ├── InventoryRepository.java    # Spring Data JPA Repository
│   │   │   │   ├── InventoryService.java       # Public interface (getItem, reserve, restock)
│   │   │   │   └── InventoryServiceImpl.java   # PACKAGE-PRIVATE service implementation
│   │   │   ├── shop/                           # Order / Shop Module
│   │   │   │   ├── Order.java                  # JPA entity for 'orders' table
│   │   │   │   ├── OrderItem.java              # JPA entity for 'order_items' table
│   │   │   │   ├── OrderItemDto.java           # DTO for line item requests and outcomes
│   │   │   │   ├── OrderRepository.java        # Spring Data JPA Repository
│   │   │   │   ├── OrderRequest.java           # Multi-item request DTO ({ items: [...] })
│   │   │   │   ├── OrderResponse.java          # Response DTO ({ orderId, status, items, inventory })
│   │   │   │   ├── OrderService.java           # Multi-item validation, rollback & cancellation
│   │   │   │   └── OrderController.java        # REST endpoints (/api/orders, /api/orders/{id}/cancel)
│   │   │   └── notification/                   # Decoupled Notification Module
│   │   │       ├── Notification.java           # JPA entity for 'notifications' table
│   │   │       ├── NotificationRepository.java # JPA Repository for notifications
│   │   │       ├── NotificationEventListener.java # @EventListener for domain events
│   │   │       └── NotificationController.java # REST endpoint (GET /api/notifications)
│   │   └── resources/
│   │       └── application.properties          # Environment variable-based configuration
│   └── test/
│       └── java/edu/cit/patonog/
│           └── ShopApplicationTests.java       # Unit tests for multi-item orders, cancel & events
├── frontend/                                   # React (Vite) Frontend
│   ├── src/
│   │   ├── App.jsx                             # Cart, live inventory, cancel action, activity feed
│   │   ├── index.css                           # Compact styling for 100% zoom
│   │   └── main.jsx                            # React root
│   ├── package.json
│   └── vite.config.js
├── supabase_schema.sql                         # Complete Lab 2 SQL schema & seed script
├── .env.example                                # Environment variable template
├── .gitignore                                  # Excludes credentials and build artifacts
├── pom.xml                                     # Maven dependencies
└── README.md
```

---

## 🗄️ Supabase Setup Guide

### 1. Database Schema Execution
Open the **SQL Editor** in your Supabase dashboard and run the entire script [`supabase_schema.sql`](supabase_schema.sql):

```sql
-- 1. Create Inventory Table
CREATE TABLE IF NOT EXISTS inventory (
    product_id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    stock INT NOT NULL CHECK (stock >= 0)
);

-- 2. Seed Initial Inventory Data
INSERT INTO inventory (product_id, name, stock) VALUES
    ('P100', 'Wireless Mouse', 25),
    ('P200', 'Mechanical Keyboard', 10),
    ('P300', 'USB-C Hub', 0)
ON CONFLICT (product_id) DO UPDATE 
SET name = EXCLUDED.name, stock = EXCLUDED.stock;

-- 3. Create Orders Table
CREATE TABLE IF NOT EXISTS orders (
    order_id VARCHAR(100) PRIMARY KEY,
    status VARCHAR(50) NOT NULL,
    reason VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 4. Create Order Items Table (Multi-Item Support)
CREATE TABLE IF NOT EXISTS order_items (
    id BIGSERIAL PRIMARY KEY,
    order_id VARCHAR(100) NOT NULL REFERENCES orders(order_id) ON DELETE CASCADE,
    product_id VARCHAR(50) NOT NULL REFERENCES inventory(product_id),
    quantity INT NOT NULL CHECK (quantity > 0)
);

-- 5. Create Notifications Table (Domain Event Log)
CREATE TABLE IF NOT EXISTS notifications (
    notification_id VARCHAR(100) PRIMARY KEY,
    message VARCHAR(500) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
```

---

## ⚙️ Environment Variables Setup

Configure your Supabase database credentials in PowerShell before launching:

```powershell
$env:SPRING_DATASOURCE_URL="jdbc:postgresql://aws-0-ap-northeast-2.pooler.supabase.com:6543/postgres?sslmode=require"
$env:SPRING_DATASOURCE_USERNAME="postgres.iqknbaihemnsfllbrmfh"
$env:SPRING_DATASOURCE_PASSWORD="<your-database-password>"
```

---

## 🚀 Running the Application

### 1. Spring Boot Backend
```powershell
# Run automated tests
.\mvnw test

# Start the server on port 8080
.\mvnw spring-boot:run
```

### 2. React Frontend
```powershell
cd frontend
npm install
npm run dev
```
Open `http://localhost:5173` in your browser.

---

## ⚡ Synchronous vs. Asynchronous Event Listeners (`@Async`)

In our implementation, the `NotificationEventListener` runs **synchronously** (Spring's default `@EventListener`).

### Why Synchronous was Chosen:
1. **Predictability & Simplicity:** Synchronous execution keeps the event handling on the same execution thread as the HTTP request. When an order is placed or rejected, the notification record is persisted immediately before the HTTP response is returned to the client, guaranteeing that the frontend's subsequent `GET /api/notifications` call will reliably find the new event.
2. **Transactional Integrity:** Both the order creation and notification insert participate within the request scope, eliminating race conditions.

### What Changes if `@Async` is Used:
If `@Async` were added to `@EventListener`:
- **Decoupled Latency:** The HTTP response wouldn't wait for notifications to be saved, which would be useful if the notification module sent emails, SMS, or external webhooks.
- **Requirements to Add:** We would need `@EnableAsync` in Spring Boot, an asynchronous thread pool task executor (`ThreadPoolTaskExecutor`), and `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)` to ensure events are only handled after the database transaction has safely committed. If the listener failed, error handling would need dedicated dead-letter or compensation logic since it wouldn't propagate back to the caller.

---

## 📸 Network Tab Evidence (Lab 2 Scenarios)

### Scenario 1: Multi-Item Order All Succeed (`CONFIRMED`)
- **Action:** Add `P100` (qty 2) and `P200` (qty 1) to cart, click Submit Order.
- **Request:** `POST /api/orders` with `{ "items": [{ "productId": "P100", "quantity": 2 }, { "productId": "P200", "quantity": 1 }] }`
- **Response:** HTTP 200 with `"status": "CONFIRMED"`, both line items marked `"outcome": "RESERVED"`.
- *(Insert screenshot here: `evidence/scenario1_confirmed_multi_item.png`)*

### Scenario 2: Multi-Item Order One Item Fails (`REJECTED` with All-or-Nothing Rollback)
- **Action:** Add `P100` (qty 1) and `P300` (qty 1, 0 stock) to cart, click Submit Order.
- **Request:** `POST /api/orders` with `{ "items": [{ "productId": "P100", "quantity": 1 }, { "productId": "P300", "quantity": 1 }] }`
- **Response:** HTTP 200 with `"status": "REJECTED"`, `"reason": "Insufficient stock for USB-C Hub (P300)..."`. P100 stock is completely untouched (0 units reserved).
- *(Insert screenshot here: `evidence/scenario2_rejected_all_or_nothing.png`)*

### Scenario 3: Order Cancellation with Restock
- **Action:** Click "Cancel Order" on a confirmed order in the order history.
- **Request:** `POST /api/orders/{orderId}/cancel`
- **Response:** HTTP 200 with `"status": "CANCELLED"`. Immediately followed by `GET /api/inventory` showing stock restored to its previous count.
- *(Insert screenshot here: `evidence/scenario3_order_cancellation_restock.png`)*

### Scenario 4: Notification Feed Showing Confirmed, Rejected, and Low-Stock Alert
- **Action:** View the activity feed after placing orders.
- **Request:** `GET /api/notifications`
- **Response:** HTTP 200 returning list containing order confirmation entries, order rejection entries, and `"Reorder needed: ... stock is down to X units"` entries.
- *(Insert screenshot here: `evidence/scenario4_notification_feed.png`)*

---

## 📝 Architectural Reflection (300–500 Words)

### 1. Multi-Item Orders & Transactional Atomicity (In-Process vs. Network)

In our modular monolith, multi-item orders touch `InventoryService` multiple times within a single request, but atomicity is guaranteed through a two-step approach and Spring's `@Transactional` boundary. First, `OrderService` performs a complete validation pass over all requested items before making any state changes. If even a single item lacks sufficient stock, the process stops immediately and nothing is reserved. Second, because both the `shop` and `inventory` modules run within the same JVM and share the same database connection, Spring manages the entire operation under one local ACID transaction. If an unexpected error or runtime exception occurs during reservation, the database automatically rolls back all changes, ensuring no partial fulfillment ever occurs.

If `Order` and `Inventory` were split across a network into separate microservices, we would lose shared ACID transactions completely. Each call to reserve stock would be an independent HTTP request to a separate database. To handle this, we would need to implement the Saga Pattern, either through orchestration or choreography. In an orchestration saga, an Order Saga Coordinator would send reserve requests to the Inventory service. If item one succeeded but item two failed due to insufficient stock, the coordinator would have to execute compensating transactions—such as sending explicit restock or release requests for the items that were previously reserved—to return the system to a consistent state.

---

### 2. Event-Driven Decoupling: In-Process vs. Microservices

Publishing an event via Spring's `ApplicationEventPublisher` instead of calling the `Notification` module directly completely breaks the compile-time coupling between `OrderService` and `Notification`. In our code, `OrderService` only knows about its own domain events (`OrderPlacedEvent` and `OrderRejectedEvent`). It has zero imports from `edu.cit.patonog.notification`, and the `Notification` module only knows about the event structures, never referencing `OrderService` or `InventoryService`. This means we can modify, refactor, or even completely disable the notification logic without touching a single line of order processing code.

If `Notification` became a separate microservice, Spring's in-process event bus would no longer work because the services would run in different JVM processes. We would need to introduce an external message broker like RabbitMQ, Apache Kafka, or AWS SQS. `OrderService` would publish events as JSON messages to an exchange or topic, and the Notification microservice would subscribe to that queue. To guarantee reliable delivery without losing messages during network hiccups or service restarts, we would also need the Transactional Outbox Pattern to save events into the database before publishing, alongside at-least-once delivery semantics and idempotent event consumers on the notification side.

---

### 3. Microservice Extraction Decision

If forced to extract exactly one module into its own microservice first, I would choose the **Notification** module. The reason is that Notification is already completely decoupled from the core business domain. It is an asynchronous consumer that only reads domain events and writes to its own `notifications` table; neither `OrderService` nor `InventoryService` ever needs a response back from it. Extracting `Inventory` first would be much riskier because Order and Inventory have tight transactional dependencies that would immediately force us into complex distributed sagas and two-phase commits.

To extract Notification into a standalone service, the changes to our codebase would be minimal. First, we would move `Notification`, `NotificationRepository`, `NotificationEventListener`, and `NotificationController` into their own separate Spring Boot project with their own database. In the monolith, we would replace the local `@EventListener` with a message publisher that sends `OrderPlacedEvent`, `OrderRejectedEvent`, and `LowStockEvent` to a message broker (like RabbitMQ or Kafka). The new Notification service would listen to that broker topic, save the incoming messages to its dedicated database, and serve the `GET /api/notifications` endpoint independently.
