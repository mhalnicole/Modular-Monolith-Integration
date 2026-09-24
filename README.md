# Modular Monolith Integration — Lab 3: LegacySupply Integration

**Course/Lab:** Lab 3: LegacySupply Integration  

---

## Overview & Architecture

Lab 3 extends our modular monolith by integrating an **Anti-Corruption Layer (ACL)** for external replenishment orders with **LegacySupply Distribution**. 

Key capabilities introduced in Lab 3:
1. **Anti-Corruption Layer (`edu.cit.patonog.supplier`):** Translates internal domain terms and requests into LegacySupply's legacy XML format without leaking external quirks (SKUs, pack sizes, XML schemas, or status codes) into core `Order` or `Inventory` modules.
2. **Encapsulation:** Only `SupplierGateway` and domain result types are public. All XML parsers, HTTP clients, session managers, and translators are strictly package-private.
3. **Automated Session Management:** `SessionManager` handles automated sign-in via `POST /auth/token` with Client ID `23-0065-102` and `$env:LS_API_KEY`, caching tokens in memory and automatically refreshing them upon expiration (measured 120s lifetime).
4. **Idempotency & Resilience:** All reorders generate and store a persistent `X-Request-Id` (UUID) and `BuyerRef` in `supplier_orders` before dispatch. Outbound calls timeout at 3 seconds and retry up to 3 times with exponential backoff.
5. **Outage Recovery & Scheduled Poller:** `@Scheduled` jobs automatically retry `PENDING` orders during outages and poll open purchase orders until delivery.
6. **Decoupled Delivery Restock:** Upon reaching status 40 (Delivered), the supplier scheduler fires `SupplierOrderDeliveredEvent`. The `Inventory` module listens via `@EventListener` and restocks the units without importing the supplier module.

---

## Project Structure

```
monolith/
├── src/
│   ├── main/
│   │   ├── java/edu/cit/patonog/
│   │   │   ├── ShopApplication.java            # @SpringBootApplication & @EnableScheduling
│   │   │   ├── config/
│   │   │   │   └── WebCorsConfig.java          # CORS configuration
│   │   │   ├── events/                         # In-Monolith Domain Events
│   │   │   │   ├── OrderPlacedEvent.java       # Customer order confirmed
│   │   │   │   ├── OrderRejectedEvent.java     # Customer order rejected
│   │   │   │   ├── LowStockEvent.java          # Stock <= 5 alert trigger
│   │   │   │   └── SupplierOrderDeliveredEvent.java # Supplier shipment delivered
│   │   │   ├── inventory/                      # Inventory Module
│   │   │   │   ├── InventoryItem.java          # JPA entity for 'inventory' table
│   │   │   │   ├── InventoryRepository.java    # Spring Data JPA repository
│   │   │   │   ├── InventoryService.java       # Public interface
│   │   │   │   └── InventoryServiceImpl.java   # Implementation (listens to delivery events)
│   │   │   ├── shop/                           # Order / Shop Module
│   │   │   │   ├── Order.java                  # JPA entity for 'orders' table
│   │   │   │   ├── OrderItem.java              # JPA entity for 'order_items' table
│   │   │   │   ├── OrderItemDto.java           # DTO for line item outcomes
│   │   │   │   ├── OrderRepository.java        # Spring Data JPA repository
│   │   │   │   ├── OrderRequest.java           # Multi-item request DTO
│   │   │   │   ├── OrderResponse.java          # Response DTO
│   │   │   │   ├── OrderService.java           # Multi-item validation & cancel
│   │   │   │   └── OrderController.java        # REST endpoints
│   │   │   ├── notification/                   # Decoupled Notification Module
│   │   │   │   ├── Notification.java           # JPA entity for 'notifications' table
│   │   │   │   ├── NotificationRepository.java # JPA repository
│   │   │   │   ├── NotificationEventListener.java # @EventListener for activity feed
│   │   │   │   └── NotificationController.java # Activity feed endpoint
│   │   │   └── supplier/                       # Anti-Corruption Layer (ACL)
│   │   │       ├── SupplierGateway.java        # PUBLIC gateway interface
│   │   │       ├── SupplierGatewayImpl.java    # Package-private implementation
│   │   │       ├── SupplierOrderStatus.java    # PUBLIC status enum
│   │   │       ├── SupplierReorderResult.java  # PUBLIC result record
│   │   │       ├── SupplierOrder.java          # JPA entity for 'supplier_orders'
│   │   │       ├── SupplierOrderRepository.java# JPA repository
│   │   │       ├── LegacySupplyClient.java     # Package-private XML/HTTP client
│   │   │       ├── SessionManager.java         # Package-private session manager
│   │   │       ├── SupplierSkuTranslator.java  # Package-private SKU & case math
│   │   │       ├── SupplierResilienceScheduler.java # Package-private retry & poller
│   │   │       └── AutoReorderEventListener.java # LowStock event listener
│   │   └── resources/
│   │       └── application.properties          # Environment-based configuration
│   └── test/
│       └── java/edu/cit/patonog/
│           └── ShopApplicationTests.java       # Unit tests
├── frontend/                                   # React (Vite) Frontend
├── supabase_schema.sql                         # Complete SQL schema & seed script
├── INTEGRATION.md                              # Lab 3 Contract discovery & SKU mapping
├── REFLECTION.md                               # Lab 3 Reflection question answers
├── pom.xml                                     # Maven dependencies
└── README.md
```

---

## Supabase Database Schema

Run [`supabase_schema.sql`](supabase_schema.sql) in your Supabase SQL Editor:

```sql
-- 1. Inventory Table
CREATE TABLE IF NOT EXISTS inventory (
    product_id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    stock INT NOT NULL CHECK (stock >= 0)
);

-- 2. Customer Orders Table
CREATE TABLE IF NOT EXISTS orders (
    order_id VARCHAR(100) PRIMARY KEY,
    status VARCHAR(50) NOT NULL,
    reason VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 3. Order Items Table
CREATE TABLE IF NOT EXISTS order_items (
    id BIGSERIAL PRIMARY KEY,
    order_id VARCHAR(100) NOT NULL REFERENCES orders(order_id) ON DELETE CASCADE,
    product_id VARCHAR(50) NOT NULL REFERENCES inventory(product_id),
    quantity INT NOT NULL CHECK (quantity > 0)
);

-- 4. Notifications Table
CREATE TABLE IF NOT EXISTS notifications (
    notification_id VARCHAR(100) PRIMARY KEY,
    message VARCHAR(500) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 5. Supplier Orders Table (Lab 3)
CREATE TABLE IF NOT EXISTS supplier_orders (
    id BIGSERIAL PRIMARY KEY,
    product_id VARCHAR(50) NOT NULL REFERENCES inventory(product_id),
    buyer_ref VARCHAR(100) NOT NULL UNIQUE,
    request_id VARCHAR(100) NOT NULL,
    po_number VARCHAR(100),
    cases INT NOT NULL,
    units INT NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
```

---

## Environment Variables Setup

Configure credentials in PowerShell before running:

```powershell
$env:SPRING_DATASOURCE_URL="jdbc:postgresql://aws-0-ap-northeast-2.pooler.supabase.com:6543/postgres?sslmode=require"
$env:SPRING_DATASOURCE_USERNAME="postgres.iqknbaihemnsfllbrmfh"
$env:SPRING_DATASOURCE_PASSWORD="<your-database-password>"
$env:LS_API_KEY="LSK-BB9C86DC2BC5D210014E"
```

---

## Running the Application

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
