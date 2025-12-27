-- =============================================
-- Card Audit Table
-- Description: Tabla de auditoría para cambios de estado
-- =============================================

CREATE TABLE IF NOT EXISTS card_audit (
    id BIGSERIAL PRIMARY KEY,
    card_id VARCHAR(36) NOT NULL,

    -- Cambio realizado
    previous_status VARCHAR(20),
    new_status VARCHAR(20) NOT NULL,
    action VARCHAR(50) NOT NULL,
    reason VARCHAR(500),

    -- Usuario que realizó el cambio (futuro)
    changed_by VARCHAR(100),

    -- Timestamp
    changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Metadata adicional (JSON)
    metadata JSONB,

    -- Foreign key
    CONSTRAINT fk_card_audit_card FOREIGN KEY (card_id) REFERENCES cards(id)
);

-- Índices
CREATE INDEX idx_card_audit_card_id ON card_audit(card_id);
CREATE INDEX idx_card_audit_changed_at ON card_audit(changed_at DESC);
CREATE INDEX idx_card_audit_action ON card_audit(action);

-- Comentarios
COMMENT ON TABLE card_audit IS 'Auditoría de cambios en tarjetas';
COMMENT ON COLUMN card_audit.action IS 'Acción realizada: CREATE, APPROVE, ISSUE, ACTIVATE, BLOCK, UNBLOCK, CANCEL';
COMMENT ON COLUMN card_audit.metadata IS 'Información adicional en formato JSON';

-- =============================================
-- Trigger Function: audit_card_changes
-- Description: Registra cambios de estado automáticamente
-- =============================================

CREATE OR REPLACE FUNCTION audit_card_changes()
RETURNS TRIGGER AS $$
BEGIN
    -- Solo auditar si cambió el status
    IF (TG_OP = 'UPDATE' AND OLD.status IS DISTINCT FROM NEW.status) THEN
        INSERT INTO card_audit (
            card_id,
            previous_status,
            new_status,
            action,
            reason,
            metadata
        ) VALUES (
            NEW.id,
            OLD.status,
            NEW.status,
            CASE
                WHEN NEW.status = 'APPROVED' THEN 'APPROVE'
                WHEN NEW.status = 'ISSUED' THEN 'ISSUE'
                WHEN NEW.status = 'ACTIVE' THEN 'ACTIVATE'
                WHEN NEW.status = 'BLOCKED' THEN 'BLOCK'
                WHEN NEW.status = 'BLOCKED_FRAUD' THEN 'BLOCK_FRAUD'
                WHEN NEW.status = 'CANCELLED' THEN 'CANCEL'
                ELSE 'UPDATE'
            END,
            NEW.block_reason,
            jsonb_build_object(
                'updated_at', NEW.updated_at,
                'client_id', NEW.client_id,
                'card_type', NEW.type
            )
        );
    END IF;

    -- Para INSERT
    IF (TG_OP = 'INSERT') THEN
        INSERT INTO card_audit (
            card_id,
            new_status,
            action,
            metadata
        ) VALUES (
            NEW.id,
            NEW.status,
            'CREATE',
            jsonb_build_object(
                'client_id', NEW.client_id,
                'card_type', NEW.type,
                'card_category', NEW.category
            )
        );
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- =============================================
-- Trigger: trigger_audit_card_changes
-- =============================================

CREATE TRIGGER trigger_audit_card_changes
    AFTER INSERT OR UPDATE ON cards
    FOR EACH ROW
    EXECUTE FUNCTION audit_card_changes();
