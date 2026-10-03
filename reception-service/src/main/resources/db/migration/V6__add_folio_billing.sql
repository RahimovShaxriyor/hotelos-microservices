-- ==============================================================================
-- HotelOS Reception Service — V6 Migration
-- Schema: reception
-- Phase P3.4: Folio & Immutable Ledger Engine
-- ==============================================================================

-- 1. Fail-fast Pre-check: блокировка миграции при историческом несовпадении валют
DO $$
DECLARE
    mismatch_count INTEGER;
BEGIN
    SELECT COUNT(*)
    INTO mismatch_count
    FROM reception.room_service_charges rsc
    JOIN reception.guest_stays gs ON rsc.stay_id = gs.id
    WHERE rsc.currency <> gs.currency;

    IF mismatch_count > 0 THEN
        RAISE EXCEPTION 'MIGRATION ABORTED: Found % room_service_charges with currency mismatch against guest_stays', mismatch_count;
    END IF;
END $$;

-- 2. Таблица Folios (одно фолио на одно проживание)
CREATE TABLE reception.folios (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    stay_id UUID NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    closed_at TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_folios_stay FOREIGN KEY (stay_id)
        REFERENCES reception.guest_stays (id) ON DELETE RESTRICT,
    CONSTRAINT uq_folios_stay UNIQUE (stay_id),
    CONSTRAINT uq_folios_id_currency UNIQUE (id, currency),
    CONSTRAINT chk_folios_currency CHECK (length(currency) = 3 AND currency = upper(currency)),
    CONSTRAINT chk_folios_status CHECK (status IN ('OPEN', 'CLOSED')),
    CONSTRAINT chk_folios_closed_at CHECK (
        (status = 'OPEN' AND closed_at IS NULL) OR
        (status = 'CLOSED' AND closed_at IS NOT NULL)
    )
);

CREATE INDEX idx_folios_status ON reception.folios (status);

-- 3. Таблица Folio Entries (иммутабельный финансовый журнал)
CREATE TABLE reception.folio_entries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    folio_id UUID NOT NULL,
    currency VARCHAR(3) NOT NULL,
    entry_type VARCHAR(30) NOT NULL,
    source_type VARCHAR(30) NOT NULL,
    source_id VARCHAR(100) NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    description TEXT NOT NULL,
    idempotency_fingerprint VARCHAR(64) NULL,
    created_by VARCHAR(64) NOT NULL DEFAULT 'SYSTEM',
    occurred_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Композитный FK гарантирует совпадение валюты строки с валютой фолио
    CONSTRAINT fk_folio_entries_folio_currency FOREIGN KEY (folio_id, currency)
        REFERENCES reception.folios (id, currency) ON DELETE RESTRICT,

    -- Допустимые типы проводок
    CONSTRAINT chk_folio_entries_type CHECK (
        entry_type IN ('ROOM_CHARGE', 'ROOM_SERVICE', 'MINIBAR', 'LATE_CHECKOUT', 'DISCOUNT', 'ADJUSTMENT')
    ),

    -- Допустимые типы источников
    CONSTRAINT chk_folio_entries_source_type CHECK (
        source_type IN ('SYSTEM', 'ROOM_SERVICE_ORDER', 'MANUAL')
    ),

    -- Финансовые знаки на уровне СУБД (NUMERIC(19,2))
    CONSTRAINT chk_folio_entries_amount_sign CHECK (
        (entry_type IN ('ROOM_CHARGE', 'ROOM_SERVICE', 'MINIBAR', 'LATE_CHECKOUT') AND amount > 0) OR
        (entry_type = 'DISCOUNT' AND amount < 0) OR
        (entry_type = 'ADJUSTMENT' AND amount <> 0)
    ),

    -- Совместимость типа проводки и типа источника
    CONSTRAINT chk_folio_entries_source_compatibility CHECK (
        (entry_type = 'ROOM_CHARGE' AND source_type = 'SYSTEM') OR
        (entry_type = 'ROOM_SERVICE' AND source_type = 'ROOM_SERVICE_ORDER') OR
        (entry_type IN ('MINIBAR', 'LATE_CHECKOUT') AND source_type IN ('MANUAL', 'SYSTEM')) OR
        (entry_type IN ('DISCOUNT', 'ADJUSTMENT') AND source_type IN ('MANUAL', 'SYSTEM'))
    ),

    -- Инвариант целостности источника и фингерпринта:
    -- source_id никогда не NULL; для MANUAL обязателен idempotency_fingerprint
    CONSTRAINT chk_folio_entries_source_integrity CHECK (
        source_id IS NOT NULL AND
        (
            (source_type = 'MANUAL' AND idempotency_fingerprint IS NOT NULL) OR
            (source_type IN ('ROOM_SERVICE_ORDER', 'SYSTEM'))
        )
    )
);

CREATE INDEX idx_folio_entries_folio ON reception.folio_entries (folio_id);
CREATE INDEX idx_folio_entries_occurred_at ON reception.folio_entries (occurred_at);
CREATE INDEX idx_folio_entries_created_at ON reception.folio_entries (created_at);

-- 4. Скоупированные уникальные индексы дедупликации

-- А. Глобальная дедупликация заказов Room Service
CREATE UNIQUE INDEX uq_folio_entries_rs_order
ON reception.folio_entries (source_type, source_id)
WHERE source_type = 'ROOM_SERVICE_ORDER';

-- Б. Фолио-скоупированная дедупликация ручных проводок
CREATE UNIQUE INDEX uq_folio_entries_manual_idempotency
ON reception.folio_entries (folio_id, source_type, source_id)
WHERE source_type = 'MANUAL';

-- В. Глобальная дедупликация системных проводок
CREATE UNIQUE INDEX uq_folio_entries_system_source
ON reception.folio_entries (source_type, source_id)
WHERE source_type = 'SYSTEM';

-- 5. Детерминированный бэкфилл исторических данных
-- Используются точечные conflict targets: неожиданные сбои вызовут fail-fast прерывание миграции

-- 5.1. Создание фолио для всех GuestStays
INSERT INTO reception.folios (id, stay_id, currency, status, closed_at, created_at, updated_at)
SELECT
    gen_random_uuid(),
    gs.id,
    gs.currency,
    CASE WHEN gs.checked_out_at IS NOT NULL THEN 'CLOSED' ELSE 'OPEN' END,
    gs.checked_out_at,
    gs.created_at,
    COALESCE(gs.checked_out_at, gs.created_at)
FROM reception.guest_stays gs
ON CONFLICT (stay_id) DO NOTHING;

-- 5.2. Бэкфилл первичных начислений (ROOM_CHARGE) с семантикой BillingService: GREATEST(1, booked_nights)
INSERT INTO reception.folio_entries (
    folio_id, currency, entry_type, source_type, source_id, amount, description, idempotency_fingerprint, created_by, occurred_at, created_at
)
SELECT
    f.id,
    f.currency,
    'ROOM_CHARGE',
    'SYSTEM',
    'LEGACY_ROOM_CHARGE_' || gs.id::text,
    ROUND((gs.nightly_rate_at_checkin * GREATEST(1, gs.booked_nights))::numeric, 2),
    'Initial room charge for stay ' || gs.id::text,
    NULL,
    'SYSTEM',
    gs.checked_in_at,
    gs.created_at
FROM reception.guest_stays gs
JOIN reception.folios f ON f.stay_id = gs.id
WHERE (gs.nightly_rate_at_checkin * gs.booked_nights) > 0
ON CONFLICT (source_type, source_id) WHERE source_type = 'SYSTEM' DO NOTHING;

-- 5.3. Бэкфилл заказов Room Service
INSERT INTO reception.folio_entries (
    folio_id, currency, entry_type, source_type, source_id, amount, description, idempotency_fingerprint, created_by, occurred_at, created_at
)
SELECT
    f.id,
    rsc.currency,
    'ROOM_SERVICE',
    'ROOM_SERVICE_ORDER',
    rsc.order_id::text,
    ROUND(rsc.amount::numeric, 2),
    'Room service order ' || rsc.order_id::text,
    NULL,
    'SYSTEM',
    rsc.charged_at,
    rsc.created_at
FROM reception.room_service_charges rsc
JOIN reception.folios f ON f.stay_id = rsc.stay_id
ON CONFLICT (source_type, source_id) WHERE source_type = 'ROOM_SERVICE_ORDER' DO NOTHING;

-- 5.4. Бэкфилл минибара (при наличии)
INSERT INTO reception.folio_entries (
    folio_id, currency, entry_type, source_type, source_id, amount, description, idempotency_fingerprint, created_by, occurred_at, created_at
)
SELECT
    f.id,
    f.currency,
    'MINIBAR',
    'SYSTEM',
    'LEGACY_MINIBAR_' || gs.id::text,
    ROUND(gs.minibar_charge::numeric, 2),
    'Legacy minibar charge',
    NULL,
    'SYSTEM',
    COALESCE(gs.checked_out_at, gs.created_at),
    gs.created_at
FROM reception.guest_stays gs
JOIN reception.folios f ON f.stay_id = gs.id
WHERE gs.minibar_charge IS NOT NULL AND gs.minibar_charge > 0
ON CONFLICT (source_type, source_id) WHERE source_type = 'SYSTEM' DO NOTHING;

-- 5.5. Бэкфилл late checkout fee (при наличии)
INSERT INTO reception.folio_entries (
    folio_id, currency, entry_type, source_type, source_id, amount, description, idempotency_fingerprint, created_by, occurred_at, created_at
)
SELECT
    f.id,
    f.currency,
    'LATE_CHECKOUT',
    'SYSTEM',
    'LEGACY_LATE_FEE_' || gs.id::text,
    ROUND(gs.late_checkout_fee::numeric, 2),
    'Legacy late checkout fee',
    NULL,
    'SYSTEM',
    COALESCE(gs.checked_out_at, gs.created_at),
    gs.created_at
FROM reception.guest_stays gs
JOIN reception.folios f ON f.stay_id = gs.id
WHERE gs.late_checkout_fee IS NOT NULL AND gs.late_checkout_fee > 0
ON CONFLICT (source_type, source_id) WHERE source_type = 'SYSTEM' DO NOTHING;

-- 5.6. Бэкфилл скидки (сохраняется строго со знаком минус: amount < 0)
INSERT INTO reception.folio_entries (
    folio_id, currency, entry_type, source_type, source_id, amount, description, idempotency_fingerprint, created_by, occurred_at, created_at
)
SELECT
    f.id,
    f.currency,
    'DISCOUNT',
    'SYSTEM',
    'LEGACY_DISCOUNT_' || gs.id::text,
    -1 * ABS(ROUND(gs.discount::numeric, 2)),
    'Legacy stay discount',
    NULL,
    'SYSTEM',
    COALESCE(gs.checked_out_at, gs.created_at),
    gs.created_at
FROM reception.guest_stays gs
JOIN reception.folios f ON f.stay_id = gs.id
WHERE gs.discount IS NOT NULL AND gs.discount > 0
ON CONFLICT (source_type, source_id) WHERE source_type = 'SYSTEM' DO NOTHING;
