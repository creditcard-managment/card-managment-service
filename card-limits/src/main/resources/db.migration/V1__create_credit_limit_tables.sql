-- =============================================
-- Credit Limits Service - Schema Creation
-- Module: card-limits
-- Version: 1.0.0
-- Description: Crea las tablas para gestión de límites de crédito
-- =============================================

-- =============================================
-- Table: credit_limits
-- Description: Tabla principal de límites de crédito
-- =============================================
CREATE TABLE IF NOT EXISTS credit_limits (
    -- Primary Key
    id VARCHAR(36) PRIMARY KEY,

    -- Card relationship (FK a cards.id)
    card_id VARCHAR(36) NOT NULL UNIQUE,

    -- Purchase limit
    purchase_limit_amount DECIMAL(15, 2) NOT NULL,
    purchase_limit_currency VARCHAR(3) NOT NULL DEFAULT 'ARS',

    -- Cash advance limit
    cash_advance_limit_amount DECIMAL(15, 2) NOT NULL,
    cash_advance_limit_currency VARCHAR(3) NOT NULL DEFAULT 'ARS',

    -- Consumed credit
    consumed_credit_amount DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    consumed_credit_currency VARCHAR(3) NOT NULL DEFAULT 'ARS',

    -- Available credit
    available_credit_amount DECIMAL(15, 2) NOT NULL,
    available_credit_currency VARCHAR(3) NOT NULL DEFAULT 'ARS',

    -- Daily limit
    daily_limit_amount DECIMAL(15, 2) NOT NULL,
    daily_limit_currency VARCHAR(3) NOT NULL DEFAULT 'ARS',
    daily_limit_consumed DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    daily_limit_reset_date DATE NOT NULL,

    -- Monthly limit
    monthly_limit_amount DECIMAL(15, 2) NOT NULL,
    monthly_limit_currency VARCHAR(3) NOT NULL DEFAULT 'ARS',
    monthly_limit_consumed DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    monthly_limit_reset_year INTEGER NOT NULL,
    monthly_limit_reset_month INTEGER NOT NULL,

    -- Audit fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Soft delete
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP,

    -- Constraints
    CONSTRAINT chk_purchase_limit_positive CHECK (purchase_limit_amount >= 0),
    CONSTRAINT chk_cash_advance_limit_positive CHECK (cash_advance_limit_amount >= 0),
    CONSTRAINT chk_consumed_credit_positive CHECK (consumed_credit_amount >= 0),
    CONSTRAINT chk_available_credit_positive CHECK (available_credit_amount >= 0),
    CONSTRAINT chk_daily_limit_positive CHECK (daily_limit_amount >= 0),
    CONSTRAINT chk_daily_consumed_positive CHECK (daily_limit_consumed >= 0),
    CONSTRAINT chk_monthly_limit_positive CHECK (monthly_limit_amount >= 0),
    CONSTRAINT chk_monthly_consumed_positive CHECK (monthly_limit_consumed >= 0),
    CONSTRAINT chk_reset_month_valid CHECK (monthly_limit_reset_month >= 1 AND monthly_limit_reset_month <= 12),
    CONSTRAINT chk_reset_year_valid CHECK (monthly_limit_reset_year >= 2025)
);

-- =============================================
-- Indexes for performance
-- =============================================

-- Index on card_id (most common query)
CREATE INDEX idx_credit_limit_card_id ON credit_limits(card_id) WHERE deleted = FALSE;

-- Index on deleted flag
CREATE INDEX idx_credit_limit_deleted ON credit_limits(deleted);

-- Compound index for temporal queries
CREATE INDEX idx_credit_limit_dates ON credit_limits(daily_limit_reset_date, monthly_limit_reset_year, monthly_limit_reset_month)
    WHERE deleted = FALSE;

-- Index on created_at for temporal queries
CREATE INDEX idx_credit_limit_created_at ON credit_limits(created_at DESC) WHERE deleted = FALSE;

-- =============================================
-- Comments
-- =============================================

COMMENT ON TABLE credit_limits IS 'Tabla de límites de crédito de tarjetas';
COMMENT ON COLUMN credit_limits.id IS 'Identificador único del límite (UUID)';
COMMENT ON COLUMN credit_limits.card_id IS 'Identificador de la tarjeta (FK a cards.id)';
COMMENT ON COLUMN credit_limits.purchase_limit_amount IS 'Límite total de compras';
COMMENT ON COLUMN credit_limits.cash_advance_limit_amount IS 'Límite de adelantos en efectivo';
COMMENT ON COLUMN credit_limits.consumed_credit_amount IS 'Crédito consumido actual';
COMMENT ON COLUMN credit_limits.available_credit_amount IS 'Crédito disponible (calculado)';
COMMENT ON COLUMN credit_limits.daily_limit_amount IS 'Límite diario de transacciones';
COMMENT ON COLUMN credit_limits.daily_limit_consumed IS 'Consumo del día actual';
COMMENT ON COLUMN credit_limits.daily_limit_reset_date IS 'Fecha del último reset diario';
COMMENT ON COLUMN credit_limits.monthly_limit_amount IS 'Límite mensual de transacciones';
COMMENT ON COLUMN credit_limits.monthly_limit_consumed IS 'Consumo del mes actual';
COMMENT ON COLUMN credit_limits.monthly_limit_reset_year IS 'Año del último reset mensual';
COMMENT ON COLUMN credit_limits.monthly_limit_reset_month IS 'Mes del último reset mensual (1-12)';
COMMENT ON COLUMN credit_limits.deleted IS 'Soft delete flag';

-- =============================================
-- Function: update_updated_at_column
-- Description: Actualiza automáticamente updated_at
-- =============================================

CREATE OR REPLACE FUNCTION update_credit_limit_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- =============================================
-- Trigger: update_credit_limits_updated_at
-- Description: Dispara update_updated_at en UPDATE
-- =============================================

CREATE TRIGGER update_credit_limits_updated_at
    BEFORE UPDATE ON credit_limits
    FOR EACH ROW
    EXECUTE FUNCTION update_credit_limit_updated_at();
