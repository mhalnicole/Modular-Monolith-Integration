# Modular Monolith Integration with React Frontend & Supabase

**Course/Lab:** Modular Monolith Integration with a React Frontend  
**Stack:** Java Spring Boot (v4 / Java 17+) + React (Vite) + Supabase (PostgreSQL)  
**Package:** `edu.cit.patonog`

---

## 📌 Overview

This project demonstrates three distinct architectural integration styles within a modern full-stack web application:

```
+-------------------------------------------------------------------------+
|                              React Frontend                             |
|                        (http://localhost:5173)                          |
+------------------------------------+------------------------------------+
                                     |
                                     | HTTP REST (POST /api/orders, CORS)
                                     v
+-------------------------------------------------------------------------+
|                  Spring Boot Application (In-Process JVM)               |
|                                                                         |
|   +--------------------------+       +------------------------------+   |
|   |   edu.cit.patonog.shop   | ----> |  edu.cit.patonog.inventory   |   |
|   |      (Order Module)      |       |      (Inventory Module)      |   |
|   |                          |       |                              |   |
|   |   OrderService           |       |   InventoryService (Public)  |   |
|   |   OrderRepository        |       |   InventoryServiceImpl       |   |
|   |   OrderController        |       |   (Package-Private)          |   |
|   +------------+-------------+       +--------------+---------------+   |
+----------------|------------------------------------|-------------------+
                 | Spring Data JPA                    | Spring Data JPA
                 +------------------+  +--------------+
                                    |  |
                                    v  v
                       +-----------------------------+
                       |     Supabase PostgreSQL     |
                       |    (inventory & orders)     |
                       +-----------------------------+
```

1. **Module-to-Module In-Process Integration**: The Order module (`edu.cit.patonog.shop`) calls the Inventory module (`edu.cit.patonog.inventory`) directly in JVM memory via the public `InventoryService` interface, with implementation details hidden via package-private visibility.
2. **Service-to-Database Integration**: Spring Data JPA communicates with a shared Supabase PostgreSQL database using secure environment variables.
3. **External Client Integration**: A React (Vite) frontend communicates with the backend via HTTP REST (`POST /api/orders`) with CORS enabled.

---

## 📂 Project Structure

```
monolith/
├── src/
│   ├── main/
│   │   ├── java/edu/cit/patonog/
│   │   │   ├── ShopApplication.java            # @SpringBootApplication entry point (scans parent & subpackages)
│   │   │   ├── config/
│   │   │   │   └── WebCorsConfig.java          # CORS config enabling http://localhost:5173
│   │   │   ├── inventory/                      # Inventory Module
│   │   │   │   ├── InventoryItem.java          # JPA entity for 'inventory' table
│   │   │   │   ├── InventoryRepository.java    # Spring Data JPA Repository
│   │   │   │   ├── InventoryService.java       # Public interface contract
│   │   │   │   └── InventoryServiceImpl.java   # PACKAGE-PRIVATE service implementation
│   │   │   └── shop/                           # Order / Shop Module
│   │   │       ├── Order.java                  # JPA entity for 'orders' table
│   │   │       ├── OrderRepository.java        # Spring Data JPA Repository
│   │   │       ├── OrderRequest.java           # Request DTO (productId, quantity)
│   │   │       ├── OrderResponse.java          # Response DTO (orderId, status, reason, inventory)
│   │   │       ├── OrderService.java           # Places orders via InventoryService interface
│   │   │       └── OrderController.java        # REST API endpoints (POST /api/orders)
│   │   └── resources/
│   │       └── application.properties          # Config reading env vars with safe fallbacks
│   └── test/
│       └── java/edu/cit/patonog/
│           └── ShopApplicationTests.java       # Unit & integration tests for Confirmed & Rejected paths
├── frontend/                                   # React (Vite) Frontend
│   ├── src/
│   │   ├── App.jsx                             # UI with dropdown, quantity input, response area, live stock table
│   │   ├── index.css                           # Modern developer UI styling
│   │   └── main.jsx                            # React root
│   ├── package.json
│   └── vite.config.js
├── supabase_schema.sql                         # SQL script for table creation and initial seed data
├── .env.example                                # Environment variable template (no credentials committed)
├── .gitignore                                  # Excludes .env, local properties, node_modules, target
├── pom.xml                                     # Maven dependencies (Spring Web, Data JPA, PostgreSQL)
└── README.md
```

---

## 🗄️ Supabase Setup Guide

### 1. Create a Supabase Project
1. Log in to [Supabase](https://supabase.com) and click **New Project**.
2. Set a project name (e.g. `modular-shop`) and set a secure database password. Choose your preferred region.

### 2. Execute SQL Schema & Seed Data
1. Go to the **SQL Editor** in your Supabase dashboard.
2. Open [`supabase_schema.sql`](supabase_schema.sql) and paste its contents into the SQL Editor:

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
    product_id VARCHAR(50) NOT NULL,
    quantity INT NOT NULL,
    status VARCHAR(50) NOT NULL,
    reason VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
```
3. Click **Run** to create and populate the tables.

### 3. Retrieve Supabase Connection Details
In Supabase, navigate to **Project Settings** &rarr; **Database**:
- Under **Connection parameters** or **Connection pooling**, locate the JDBC connection URL.
- Format: `jdbc:postgresql://<host>:5432/postgres?sslmode=require` or Session Pooler port `6543`.

---

## ⚙️ Environment Variables Setup

Per security requirements, credentials are **never committed to Git**. Configure them via environment variables before running the backend:

### Windows PowerShell:
```powershell
$env:SPRING_DATASOURCE_URL="jdbc:postgresql://<your-supabase-host>:5432/postgres?sslmode=require"
$env:SPRING_DATASOURCE_USERNAME="postgres"
$env:SPRING_DATASOURCE_PASSWORD="<your-database-password>"
```

### Linux / macOS:
```bash
export SPRING_DATASOURCE_URL="jdbc:postgresql://<your-supabase-host>:5432/postgres?sslmode=require"
export SPRING_DATASOURCE_USERNAME="postgres"
export SPRING_DATASOURCE_PASSWORD="<your-database-password>"
```

*(Alternatively, copy `.env.example` to `.env` or set environment variables in your IDE's Run Configuration).*

---

## 🚀 Running the Application

### 1. Run Spring Boot Backend
From the project root:
```powershell
# Run unit tests
.\mvnw test

# Start the Spring Boot application on port 8080
.\mvnw spring-boot:run
```
The backend starts at `http://localhost:8080`.

### 2. Run React Frontend
In a separate terminal:
```powershell
cd frontend
npm install
npm run dev
```
The frontend is accessible at `http://localhost:5173`.

---

## 🧪 Testing & API Documentation

### REST Endpoint: `POST /api/orders`

#### Confirmed Order Example:
- **Request:**
  ```json
  POST /api/orders
  Content-Type: application/json

  {
    "productId": "P100",
    "quantity": 2
  }
  ```
- **Response (`200 OK`):**
  ```json
  {
    "orderId": "ORD-E3A9F1B2",
    "status": "CONFIRMED",
    "reason": "Order placed successfully for 2 unit(s) of Wireless Mouse",
    "inventory": {
      "productId": "P100",
      "name": "Wireless Mouse",
      "stock": 23
    }
  }
  ```

#### Rejected Order Example (Insufficient Stock):
- **Request:**
  ```json
  POST /api/orders
  Content-Type: application/json

  {
    "productId": "P300",
    "quantity": 1
  }
  ```
- **Response (`200 OK`):**
  ```json
  {
    "orderId": "ORD-7B4D201A",
    "status": "REJECTED",
    "reason": "Insufficient stock: Requested 1, but only 0 available",
    "inventory": {
      "productId": "P300",
      "name": "USB-C Hub",
      "stock": 0
    }
  }
  ```

---

## 📸 Network Tab Evidence

The screenshot below captures the browser DevTools Network tab showing both CONFIRMED and REJECTED HTTP responses from `POST /api/orders`.

The Preview panel on the right shows the full order history returned by `GET /api/orders`, including:
- `ORD-C3946608` — P100 → **CONFIRMED**
- `ORD-FBBC2465` — P100 → **CONFIRMED**
- `ORD-4D56D51D` — P300 → **REJECTED** (`"Insufficient stock: Requested 1, but only 0 available"`)
- `ORD-599DC053` — P200 → **CONFIRMED**
- `ORD-80953638` — P100 → **CONFIRMED**

![Network Tab Evidence — CONFIRMED and REJECTED orders visible in DevTools](evidence/Screenshot%202026-09-10%20201302.png)

---

## 📝 Architectural Reflection (300–500 Words)

### 1. In-Process vs. Microservices over a Network
Integrating the `Order` and `Inventory` modules **in-process** within a modular monolith provides substantial architectural benefits "for free":
- **Zero Network Overhead & Sub-Millisecond Latency:** In-process communication consists of standard Java method invocations in JVM memory, avoiding TCP handshakes, DNS resolution, and JSON serialization/deserialization.
- **ACID Transaction Guarantees:** Both modules can participate in a single local database transaction managed by Spring's `@Transactional`. If an order fails to record, stock rollback is guaranteed atomically by the database without data inconsistency.
- **Compile-Time Type Safety:** Data transfer contracts are verified at compile time by the Java compiler.

If split into separate microservices over a network, several complex distributed systems concerns must be added back:
- **Resilience & Fault Tolerance:** Network calls can fail partially or timeout, requiring circuit breakers (e.g., Resilience4j), retries with exponential backoff, and timeouts.
- **Distributed Transactions & Eventual Consistency:** Without shared database transactions, systems must implement the **Saga Pattern** (orchestration or choreography) with compensating transactions to undo inventory reservations if order creation fails.
- **Idempotency & Distributed Tracing:** Network retries risk duplicate reservations, necessitating idempotency keys and distributed tracing tools (e.g., OpenTelemetry/Zipkin).

---

### 2. Importance of Package-Private Visibility on `InventoryServiceImpl`
Declaring `InventoryServiceImpl` as **package-private** (`class InventoryServiceImpl implements InventoryService` without the `public` access modifier) is a cornerstone of modular monolith architecture:
- **Enforced Boundary at Compile Time:** In Java, package-private classes are invisible to other packages. The `OrderService` (in `edu.cit.patonog.shop`) is physically prevented from importing or directly instantiating `InventoryServiceImpl`. It can only depend on the public contract (`InventoryService`).
- **Decoupling & Encapsulation:** It guarantees that internal implementation details—such as `InventoryRepository`, database queries, or private helper methods—remain strictly encapsulated within the `inventory` package.
- **What Breaks if Public?** If `InventoryServiceImpl` were made `public`, developers in the `shop` module could accidentally bypass the interface, inject the concrete class, or instantiate it directly. Over time, this leads to tight architectural coupling, circular dependencies, and a "spaghetti monolith" where modules cannot be independently refactored, tested, or extracted.

---

### 3. Microservice Extraction Strategy
**When to extract Inventory into its own microservice:**
- **Divergent Scaling Requirements:** When the inventory lookup/stock-checking traffic grows exponentially higher than order creation (e.g., millions of browsing users vs. thousands of purchasers), requiring independent horizontal auto-scaling.
- **Team Ownership & Organizational Boundaries:** When a dedicated inventory/warehouse team needs autonomous deployment pipelines and release cycles without risking the shop codebase.
- **Technology Specialization:** If the inventory domain requires specialized caching (e.g., high-throughput Redis clusters) or event streaming (e.g., Kafka).

**What would need to change in the codebase:**
1. **Interface Remains Untouched:** The `InventoryService` interface signature (`getItem`, `reserve`) remains identical, ensuring zero changes to `OrderService` business logic.
2. **Swap Implementation:** Replace the local `InventoryServiceImpl` with an HTTP/REST or gRPC client (e.g., Spring `RestClient`, `WebClient`, or Feign client) in the shop module that calls `https://inventory-service/api/...`.
3. **Database Segregation:** Separate the shared database so the Inventory service owns the `inventory` table exclusively, while the Shop service owns the `orders` table.
4. **Asynchronous / Saga Integration:** Transition order fulfillment to an event-driven model (e.g., publishing `OrderPlacedEvent` and consuming `InventoryReservedEvent`).
