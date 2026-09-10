-- =============================================================================
-- Supabase PostgreSQL Schema & Seed Script
-- Lab: Modular Monolith Integration with a React Frontend
-- Student Package: edu.cit.patonog
-- =============================================================================

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

-- 4. Verify Seed Data
SELECT * FROM inventory;
SELECT * FROM orders;
