-- =============================================
-- Card Management Service - Schema Creation
-- Module: card-data
-- Version: 1.0.0
-- Description: Crea las tablas para gestión de tarjetas
-- =============================================

-- =============================================
-- Table: cards
-- Description: Tabla principal de tarjetas
-- =============================================
CREATE TABLE IF NOT EXISTS cards (
    -- Primary Key
    id VARCHAR(36) PRIMARY KEY,

    -- Client relationship
    client_id VARCHAR(20) NOT NULL,

    -- Card holder information (embedded)
    holder_first_name VARCHAR(100) NOT NULL,
    holder_last_name VARCHAR(100) NOT NULL,
    holder_document_number VARCHAR(20) NOT NULL,
    holder_email VARCHAR(255) NOT NULL,
    holder_phone_number VARCHAR(20) NOT NULL,
    holder_birth_date DATE NOT NULL,
    holder_address VARCHAR(500),

    -- Sensitive data (ENCRYPTED in application layer)
    card_number_encrypted TEXT,
    last_four_digits VARCHAR(4),
    cvv_encrypted TEXT,
    pin_hash TEXT,

    -- Card metadata
    type VARCHAR(20) NOT NULL,
    category VARCHAR(20) NOT NULL,
    expiration_month INTEGER,
    expiration_year INTEGER,
    status VARCHAR(20) NOT NULL DEFAULT 'REQUESTED',

    -- Audit fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    activated_at TIMESTAMP,
    blocked_at TIMESTAMP,
    block_reason VARCHAR(500),

    -- Soft delete
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP,

    -- Constraints
    CONSTRAINT chk_card_type CHECK (type IN ('VISA', 'MASTERCARD', 'AMEX', 'CABAL', 'NARANJA')),
    CONSTRAINT chk_card_category CHECK (category IN ('CLASSIC', 'GOLD', 'PLATINUM', 'BLACK', 'INFINITE')),
    CONSTRAINT chk_card_status CHECK (status IN ('REQUESTED', 'APPROVED', 'ISSUED', 'ACTIVE', 'BLOCKED', 'BLOCKED_FRAUD', 'EXPIRED', 'CANCELLED')),
    CONSTRAINT chk_expiration_month CHECK (expiration_month IS NULL OR (expiration_month >= 1 AND expiration_month <= 12)),
    CONSTRAINT chk_expiration_year CHECK (expiration_year IS NULL OR expiration_year >= 2025),
    CONSTRAINT chk_last_four_digits CHECK (last_four_digits IS NULL OR last_four_digits ~ '^[0-9]{4}$')
);

-- =============================================
-- Indexes for performance
-- =============================================

-- Index on client_id (most common query)
CREATE INDEX idx_cards_client_id ON cards(client_id) WHERE deleted = FALSE;

-- Index on card_number_encrypted (for lookups)
CREATE INDEX idx_cards_card_number_encrypted ON cards(card_number_encrypted) WHERE deleted = FALSE;

-- Index on last_four_digits + client_id (search by last 4)
CREATE INDEX idx_cards_last_four_client ON cards(last_four_digits, client_id) WHERE deleted = FALSE;

-- Index on status (for filtering by state)
CREATE INDEX idx_cards_status ON cards(status) WHERE deleted = FALSE;

-- Index on holder_document_number (for customer lookup)
CREATE INDEX idx_cards_holder_document ON cards(holder_document_number) WHERE deleted = FALSE;

-- Compound index for renewal queries
CREATE INDEX idx_cards_renewal ON cards(status, expiration_year, expiration_month)
    WHERE deleted = FALSE AND status = 'ACTIVE';

-- Index on created_at for temporal queries
CREATE INDEX idx_cards_created_at ON cards(created_at DESC) WHERE deleted = FALSE;

-- =============================================
-- Comments
-- =============================================

COMMENT ON TABLE cards IS 'Tabla principal de tarjetas de crédito';
COMMENT ON COLUMN cards.id IS 'Identificador único de la tarjeta (UUID)';
COMMENT ON COLUMN cards.client_id IS 'Identificador del cliente (DNI, CUIL, CUIT)';
COMMENT ON COLUMN cards.card_number_encrypted IS 'Número de tarjeta encriptado (AES-256)';
COMMENT ON COLUMN cards.last_four_digits IS 'Últimos 4 dígitos (para display y búsqueda)';
COMMENT ON COLUMN cards.cvv_encrypted IS 'CVV encriptado (AES-256)';
COMMENT ON COLUMN cards.pin_hash IS 'PIN hasheado (BCrypt)';
COMMENT ON COLUMN cards.type IS 'Tipo de tarjeta: VISA, MASTERCARD, AMEX, CABAL, NARANJA';
COMMENT ON COLUMN cards.category IS 'Categoría: CLASSIC, GOLD, PLATINUM, BLACK, INFINITE';
COMMENT ON COLUMN cards.status IS 'Estado: REQUESTED, APPROVED, ISSUED, ACTIVE, BLOCKED, BLOCKED_FRAUD, EXPIRED, CANCELLED';
COMMENT ON COLUMN cards.deleted IS 'Soft delete flag';

-- =============================================
-- Function: update_updated_at_column
-- Description: Actualiza automáticamente updated_at
-- =============================================

CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- =============================================
-- Trigger: update_cards_updated_at
-- Description: Dispara update_updated_at en UPDATE
-- =============================================

CREATE TRIGGER update_cards_updated_at
    BEFORE UPDATE ON cards
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- =============================================
-- Initial data (opcional - para testing)
-- =============================================

-- Puedes descomentar esto para crear una tarjeta de prueba
/*
INSERT INTO cards (
    id,
    client_id,
    holder_first_name,
    holder_last_name,
    holder_document_number,
    holder_email,
    holder_phone_number,
    holder_birth_date,
    type,
    category,
    status
) VALUES (
    'test-card-001',
    '12345678',
    'Juan',
    'Pérez',
    '12345678',
    'juan.perez@example.com',
    '+5491123456789',
    '1990-01-15',
    'VISA',
    'CLASSIC',
    'REQUESTED'
);
*/


