# Modular Monolith Integration - Lab 4: Tiangge Marketplace Integration

**Course/Lab:** Lab 4: Tiangge Marketplace: Run Your Shop Unattended  

---

## Overview & Architecture

Lab 4 integrates our modular monolith with the Tiangge Marketplace for unattended operations. The application discovers incoming orders, evaluates real inventory, pushes stock updates via domain events, and fulfills backorders once LegacySupply deliveries arrive.

- **Channel Module (`edu.cit.patonog.channel`):** Encapsulated adapter exposing only `MarketplaceGateway` and domain models.
- **Heartbeats & Go-Live:** Generates a runtime UUID (`X-Client-Instance`), publishes 3 catalog listings, and sends heartbeats every 30 seconds.
- **Event-Driven Stock Sync:** Pushes live stock changes to Tiangge using domain events without polling timers.
- **Feed Poller & Deduplication:** Reads orders and cancellations with persistent cursor tracking and deduplication in Supabase PostgreSQL.
- **Backorder Handling:** Holds unfillable orders as `BACKORDERED` while supplier orders are in transit and resolves them upon delivery.

---

## Project Structure

```
monolith/
├── src/
│   ├── main/
│   │   ├── java/edu/cit/patonog/
│   │   │   ├── ShopApplication.java
│   │   │   ├── config/
│   │   │   │   ├── AppInstanceHolder.java
│   │   │   │   └── WebCorsConfig.java
│   │   │   ├── events/
│   │   │   │   ├── LowStockEvent.java
│   │   │   │   ├── OrderPlacedEvent.java
│   │   │   │   ├── OrderRejectedEvent.java
│   │   │   │   ├── StockChangedEvent.java
│   │   │   │   └── SupplierOrderDeliveredEvent.java
│   │   │   ├── inventory/
│   │   │   │   ├── InventoryItem.java
│   │   │   │   ├── InventoryRepository.java
│   │   │   │   ├── InventoryService.java
│   │   │   │   └── InventoryServiceImpl.java
│   │   │   ├── shop/
│   │   │   │   ├── Order.java
│   │   │   │   ├── OrderItem.java
│   │   │   │   ├── OrderItemDto.java
│   │   │   │   ├── OrderRepository.java
│   │   │   │   ├── OrderRequest.java
│   │   │   │   ├── OrderResponse.java
│   │   │   │   └── OrderService.java
│   │   │   ├── notification/
│   │   │   │   ├── Notification.java
│   │   │   │   ├── NotificationRepository.java
│   │   │   │   ├── NotificationEventListener.java
│   │   │   │   └── NotificationController.java
│   │   │   ├── supplier/
│   │   │   │   ├── SupplierGateway.java
│   │   │   │   ├── SupplierGatewayImpl.java
│   │   │   │   ├── SupplierOrderStatus.java
│   │   │   │   ├── SupplierReorderResult.java
│   │   │   │   ├── SupplierOrder.java
│   │   │   │   ├── SupplierOrderRepository.java
│   │   │   │   ├── LegacySupplyClient.java
│   │   │   │   ├── SessionManager.java
│   │   │   │   ├── SupplierSkuTranslator.java
│   │   │   │   ├── SupplierResilienceScheduler.java
│   │   │   │   └── AutoReorderEventListener.java
│   │   │   └── channel/
│   │   │       ├── MarketplaceGateway.java
│   │   │       ├── MarketplaceGatewayImpl.java
│   │   │       ├── TianggeClient.java
│   │   │       ├── TianggeDto.java
│   │   │       ├── ChannelCursor.java
│   │   │       ├── ChannelCursorRepository.java
│   │   │       ├── ChannelProcessedEvent.java
│   │   │       ├── ChannelProcessedEventRepository.java
│   │   │       ├── ChannelOrder.java
│   │   │       ├── ChannelOrderRepository.java
│   │   │       ├── ChannelOrderProcessor.java
│   │   │       ├── ChannelBackorderResolver.java
│   │   │       ├── ChannelFeedPoller.java
│   │   │       ├── ChannelHeartbeatScheduler.java
│   │   │       ├── ChannelStartupRunner.java
│   │   │       └── ChannelStockSyncListener.java
│   │   └── resources/
│   │       └── application.properties
│   └── test/
│       └── java/edu/cit/patonog/
│           ├── ChannelModuleTests.java
│           ├── ShopApplicationTests.java
│           └── channel/
│               └── ChannelProcessorTests.java
├── frontend/
├── supabase_schema.sql
├── INTEGRATION.md
├── REFLECTION.md
├── pom.xml
└── README.md
```

---

## Supabase Database Schema

Run [`supabase_schema.sql`](supabase_schema.sql) in your Supabase SQL Editor:

```sql
CREATE TABLE IF NOT EXISTS inventory (
    product_id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    stock INT NOT NULL CHECK (stock >= 0)
);

CREATE TABLE IF NOT EXISTS orders (
    order_id VARCHAR(100) PRIMARY KEY,
    status VARCHAR(50) NOT NULL,
    reason VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS order_items (
    id BIGSERIAL PRIMARY KEY,
    order_id VARCHAR(100) NOT NULL REFERENCES orders(order_id) ON DELETE CASCADE,
    product_id VARCHAR(50) NOT NULL REFERENCES inventory(product_id),
    quantity INT NOT NULL CHECK (quantity > 0)
);

CREATE TABLE IF NOT EXISTS notifications (
    notification_id VARCHAR(100) PRIMARY KEY,
    message VARCHAR(500) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

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

CREATE TABLE IF NOT EXISTS channel_cursor (
    id INT PRIMARY KEY,
    next_cursor BIGINT NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS channel_processed_events (
    event_id VARCHAR(100) PRIMARY KEY,
    event_type VARCHAR(50) NOT NULL,
    processed_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS channel_orders (
    tiangge_order_id VARCHAR(100) PRIMARY KEY,
    shop_order_id VARCHAR(100),
    decision VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    lines_json TEXT NOT NULL,
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
.\mvnw test
.\mvnw spring-boot:run
```

### 2. React Frontend
```powershell
cd frontend
npm install
npm run dev
```
Open `http://localhost:5173` in your browser
