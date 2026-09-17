# Modular Monolith Integration with React Frontend & Supabase

**Course/Lab:** Modular Monolith Integration with a React Frontend  
**Stack:** Java Spring Boot (v4 / Java 17+) + React (Vite) + Supabase (PostgreSQL)  
**Package:** `edu.cit.patonog`

---

## Overview

This project demonstrates three distinct architectural integration styles within a modern full-stack web application:

1. **Module-to-Module In-Process Integration**: The Order module (`edu.cit.patonog.shop`) calls the Inventory module (`edu.cit.patonog.inventory`) directly in JVM memory via the public `InventoryService` interface, with implementation details hidden via package-private visibility.
2. **Service-to-Database Integration**: Spring Data JPA communicates with a shared Supabase PostgreSQL database using secure environment variables.
3. **External Client Integration**: A React (Vite) frontend communicates with the backend via HTTP REST (`POST /api/orders`) with CORS enabled.

---

## Project Structure

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

## Supabase Setup Guide

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

## Environment Variables Setup

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

## Running the Application

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

## Testing & API Documentation

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

## Network Tab Evidence

The screenshot below captures the browser DevTools Network tab showing both CONFIRMED and REJECTED HTTP responses from `POST /api/orders`.

The Preview panel on the right shows the full order history returned by `GET /api/orders`, including:
- `ORD-C3946608` — P100 → **CONFIRMED**
- `ORD-FBBC2465` — P100 → **CONFIRMED**
- `ORD-4D56D51D` — P300 → **REJECTED** (`"Insufficient stock: Requested 1, but only 0 available"`)
- `ORD-599DC053` — P200 → **CONFIRMED**
- `ORD-80953638` — P100 → **CONFIRMED**

![Network Tab Evidence — CONFIRMED and REJECTED orders visible in DevTools](evidence/Screenshot%202026-09-10%20201302.png)

---

## Reflection

### 1. In-Process vs. Microservices over a Network

In this project, the Order and Inventory modules communicate directly in-process, meaning they call each other through plain Java method calls inside the same JVM without any network involved. This is actually a big advantage because it removes a lot of overhead. Since there's no HTTP request happening between the two modules, there's no delay from TCP connections, no need to serialize and deserialize JSON, and no risk of a network timeout. Another benefit is that both modules can share the same database transaction managed by Spring's `@Transactional`, so if something goes wrong during an order, the stock change can be rolled back automatically without any inconsistency.

If we were to split these into separate microservices communicating over a network, things would get a lot more complicated. We'd have to handle network failures, timeouts, and partial responses. We'd also lose the ability to use a single shared transaction, so we'd need to implement something like the Saga Pattern where each service has its own compensating action in case of failure. On top of that, retrying failed network requests could accidentally duplicate reservations, so we'd need idempotency keys and probably distributed tracing tools just to debug what's happening. Basically, what was one simple method call becomes a much bigger engineering problem.

---

### 2. Importance of Package-Private Visibility on `InventoryServiceImpl`

Making `InventoryServiceImpl` package-private, meaning it has no `public` modifier, is what actually enforces the boundary between the two modules. In Java, a class without the `public` keyword can only be seen and used by other classes within the same package. So even though `OrderService` is in `edu.cit.patonog.shop`, it physically cannot import or directly use `InventoryServiceImpl` because it lives in `edu.cit.patonog.inventory`. The only thing it can access is the public `InventoryService` interface, which is exactly what we want.

This matters because if `InventoryServiceImpl` were made public, any developer could bypass the interface and inject the concrete class directly. Over time that leads to tight coupling where the shop module starts depending on internal implementation details of the inventory module. Once that happens, it becomes really hard to change one module without breaking the other, which defeats the whole purpose of having a modular architecture.

---

### 3. Microservice Extraction Strategy

If the Inventory module ever needed to become its own microservice, there are a few scenarios where that would make sense. One is when the inventory traffic grows much faster than order traffic, for example if millions of users are browsing products but only a fraction of them are actually placing orders. In that case, you'd want to scale the inventory lookup independently. Another reason would be if a separate team was responsible for inventory and needed their own deployment pipeline without touching the shop codebase.

As for what would change in the code, the good news is that the `InventoryService` interface would stay exactly the same. The only thing that needs to change is the implementation behind it. Instead of calling local methods, `InventoryServiceImpl` would be replaced with an HTTP client like Spring's `RestClient` or a Feign client that makes network calls to the external inventory service. The database would also need to be separated so each service owns its own tables. Eventually, the order flow would probably move toward an event-driven approach where placing an order publishes an event and the inventory service responds asynchronously.

