-- threshold_percent como fração (0.05 = 5%), mesma convenção usada em VehicleCondition/VehicleExtra
-- (estimate/PriceEstimateService) - não um número inteiro de porcentagem.
CREATE TABLE watched_vehicle (
    id                BIGSERIAL      PRIMARY KEY,
    user_id           BIGINT         NOT NULL REFERENCES users (id),
    fipe_code         VARCHAR(20)    NOT NULL,
    threshold_percent NUMERIC(5,4)   NOT NULL DEFAULT 0.05,
    created_at        TIMESTAMP      NOT NULL DEFAULT now(),
    CONSTRAINT uq_watched_vehicle_user_fipe_code UNIQUE (user_id, fipe_code)
);

CREATE INDEX ix_watched_vehicle_fipe_code ON watched_vehicle (fipe_code);
