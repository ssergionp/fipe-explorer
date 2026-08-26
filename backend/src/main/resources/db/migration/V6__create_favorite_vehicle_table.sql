CREATE TABLE favorite_vehicle (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT     NOT NULL REFERENCES users (id),
    price_entry_id  BIGINT     NOT NULL REFERENCES price_entry (id),
    created_at      TIMESTAMP  NOT NULL DEFAULT now(),
    CONSTRAINT uq_favorite_vehicle_user_price_entry UNIQUE (user_id, price_entry_id)
);

CREATE INDEX ix_favorite_vehicle_user_id ON favorite_vehicle (user_id);
