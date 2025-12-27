-- =============================================
-- Credit Limit History Table
-- Description: Tabla de auditoría para cambios de límites
-- =============================================

CREATE TABLE IF NOT EXISTS credit_limit_history (
    -- Primary Key
    id BIGSERIAL PRIMARY KEY,

    -- Credit limit relationship
    credit_limit_id VARCHAR(36) NOT NULL,

    -- Type of limit changed
    limit_type VARCHAR(20) NOT NULL,

    -- Action performed
    action VARCHAR(20) NOT NULL,

    -- Values - Previous
    previous_value_amount DECIMAL(15, 2),
    previous_value_currency VARCHAR(3),

    -- Values - New
    new_value_amount DECIMAL(15, 2) NOT NULL,
    new_value_currency VARCHAR(3) NOT NULL,

    -- Difference
    difference_amount DECIMAL(15, 2) NOT NULL,
    difference_currency VARCHAR(3) NOT NULL,

    -- Context
    reason VARCHAR(500),
    changed_by VARCHAR(100),

    -- Timestamp
    changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Metadata (JSON)
    metadata TEXT,

    -- Constraints
    CONSTRAINT fk_history_credit_limit FOREIGN KEY (credit_limit_id)
        REFERENCES credit_limits(id) ON DELETE CASCADE,
    CONSTRAINT chk_limit_type CHECK (limit_type IN ('PURCHASE', 'CASH_ADVANCE', 'DAILY', 'MONTHLY')),
    CONSTRAINT chk_action CHECK (action IN ('ASSIGNED', 'INCREASED', 'DECREASED', 'UPDATED'))
);

-- =============================================
-- Indexes
-- =============================================

CREATE INDEX idx_history_credit_limit_id ON credit_limit_history(credit_limit_id);
CREATE INDEX idx_history_changed_at ON credit_limit_history(changed_at DESC);
CREATE INDEX idx_history_action ON credit_limit_history(action);
CREATE INDEX idx_history_limit_type ON credit_limit_history(limit_type);

-- Compound index for common queries
CREATE INDEX idx_history_limit_type_date ON credit_limit_history(credit_limit_id, limit_type, changed_at DESC);

-- =============================================
-- Comments
-- =============================================

COMMENT ON TABLE credit_limit_history IS 'Auditoría de cambios en límites de crédito';
COMMENT ON COLUMN credit_limit_history.limit_type IS 'Tipo de límite: PURCHASE, CASH_ADVANCE, DAILY, MONTHLY';
COMMENT ON COLUMN credit_limit_history.action IS 'Acción: ASSIGNED, INCREASED, DECREASED, UPDATED';
COMMENT ON COLUMN credit_limit_history.metadata IS 'Información adicional en formato JSON';
COMMENT ON COLUMN credit_limit_history.changed_by IS 'Usuario o sistema que realizó el cambio';

-- =============================================
-- Trigger Function: audit_credit_limit_changes
-- Description: Registra cambios automáticamente
-- =============================================

CREATE OR REPLACE FUNCTION audit_credit_limit_changes()
RETURNS TRIGGER AS $$
BEGIN
    -- Auditar cambios en purchase limit
    IF (TG_OP = 'UPDATE' AND OLD.purchase_limit_amount IS DISTINCT FROM NEW.purchase_limit_amount) THEN
        INSERT INTO credit_limit_history (
            credit_limit_id,
            limit_type,
            action,
            previous_value_amount,
            previous_value_currency,
            new_value_amount,
            new_value_currency,
            difference_amount,
            difference_currency,
            reason,
            changed_by,
            metadata
        ) VALUES (
            NEW.id,
            'PURCHASE',
            CASE
                WHEN NEW.purchase_limit_amount > OLD.purchase_limit_amount THEN 'INCREASED'
                ELSE 'DECREASED'
            END,
            OLD.purchase_limit_amount,
            OLD.purchase_limit_currency,
            NEW.purchase_limit_amount,
            NEW.purchase_limit_currency,
            ABS(NEW.purchase_limit_amount - OLD.purchase_limit_amount),
            NEW.purchase_limit_currency,
            'Auto-registered change',
            'SYSTEM',
            jsonb_build_object(
                'updated_at', NEW.updated_at,
                'card_id', NEW.card_id
            )
        );
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- =============================================
-- Trigger: trigger_audit_credit_limit_changes
-- =============================================

CREATE TRIGGER trigger_audit_credit_limit_changes
    AFTER UPDATE ON credit_limits
    FOR EACH ROW
    EXECUTE FUNCTION audit_credit_limit_changes();
