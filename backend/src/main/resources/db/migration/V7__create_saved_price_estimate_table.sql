-- extras/components guardados como JSON em TEXT (mesmo padrão de external_price_history.payload),
-- não JSONB nem colunas separadas - sem precisar consultar por dentro desses campos até agora.
CREATE TABLE saved_price_estimate (
    id              BIGSERIAL      PRIMARY KEY,
    user_id         BIGINT         NOT NULL REFERENCES users (id),
    price_entry_id  BIGINT         NOT NULL REFERENCES price_entry (id),
    km              BIGINT         NOT NULL,
    condition       VARCHAR(20)    NOT NULL,
    extras          TEXT           NOT NULL,
    adjusted_price  NUMERIC(12,2)  NOT NULL,
    components      TEXT           NOT NULL,
    created_at      TIMESTAMP      NOT NULL DEFAULT now()
);

CREATE INDEX ix_saved_price_estimate_user_id ON saved_price_estimate (user_id);
