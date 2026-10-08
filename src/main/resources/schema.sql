-- ============================================
-- FYNXT Order Booking & Portfolio API
-- Database Schema
-- ============================================

-- ============================================
-- 1. TRADERS
-- ============================================

CREATE TABLE IF NOT EXISTS traders (
   id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
    );


-- ============================================
-- 2. PORTFOLIO HOLDINGS
-- ============================================

CREATE TABLE IF NOT EXISTS portfolio_holdings (
     id BIGINT AUTO_INCREMENT PRIMARY KEY,

    trader_id VARCHAR(50) NOT NULL,
    stock VARCHAR(20) NOT NULL,
    sector VARCHAR(50) NOT NULL,

    quantity INT NOT NULL DEFAULT 0,
    reserved_quantity INT NOT NULL DEFAULT 0,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
    ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_portfolio_trader
    FOREIGN KEY (trader_id)
    REFERENCES traders(id),

    CONSTRAINT uk_portfolio_trader_stock
    UNIQUE (trader_id, stock),

    CONSTRAINT chk_portfolio_quantity
    CHECK (quantity >= 0),

    CONSTRAINT chk_reserved_quantity
    CHECK (reserved_quantity >= 0),

    CONSTRAINT chk_reserved_not_exceed_quantity
    CHECK (reserved_quantity <= quantity)
    );


-- ============================================
-- 3. ORDERS
-- ============================================

CREATE TABLE IF NOT EXISTS orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    trader_id VARCHAR(50) NOT NULL,
    stock VARCHAR(20) NOT NULL,
    sector VARCHAR(50) NOT NULL,

    quantity INT NOT NULL,

    side VARCHAR(10) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
    ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_order_trader
    FOREIGN KEY (trader_id)
    REFERENCES traders(id),

    CONSTRAINT chk_order_quantity
    CHECK (quantity > 0),

    CONSTRAINT chk_order_side
    CHECK (side IN ('BUY', 'SELL')),

    CONSTRAINT chk_order_status
    CHECK (status IN ('PENDING', 'FILLED', 'CANCELLED'))
    );





-- ============================================
-- 5. DEMO TRADER
-- ============================================

INSERT INTO traders (id, name)
SELECT 'T001', 'Demo Trader'
    WHERE NOT EXISTS (
    SELECT 1
    FROM traders
    WHERE id = 'T001'
);