CREATE TABLE brand (
    id         BIGSERIAL PRIMARY KEY,
    fipe_code  VARCHAR(20)  NOT NULL,
    name       VARCHAR(150) NOT NULL,
    CONSTRAINT uq_brand_fipe_code UNIQUE (fipe_code)
);

CREATE TABLE fuel_type (
    id    BIGSERIAL PRIMARY KEY,
    code  VARCHAR(1)   NOT NULL,
    name  VARCHAR(50)  NOT NULL,
    CONSTRAINT uq_fuel_type_name UNIQUE (name)
);

CREATE TABLE vehicle_model (
    id               BIGSERIAL PRIMARY KEY,
    brand_id         BIGINT       NOT NULL REFERENCES brand (id),
    fipe_model_code  VARCHAR(20)  NOT NULL,
    name             VARCHAR(200) NOT NULL,
    vehicle_type     VARCHAR(20)  NOT NULL,
    fipe_price_code  VARCHAR(20)  NOT NULL,
    CONSTRAINT uq_vehicle_model_brand_code UNIQUE (brand_id, fipe_model_code)
);

CREATE INDEX ix_vehicle_model_brand_id ON vehicle_model (brand_id);

CREATE TABLE price_entry (
    id                BIGSERIAL PRIMARY KEY,
    vehicle_model_id  BIGINT        NOT NULL REFERENCES vehicle_model (id),
    fuel_type_id      BIGINT        NOT NULL REFERENCES fuel_type (id),
    year_code         VARCHAR(20)   NOT NULL,
    year_value        VARCHAR(50)   NOT NULL,
    price             NUMERIC(12,2) NOT NULL,
    reference_month   VARCHAR(30)   NOT NULL,
    CONSTRAINT uq_price_entry_model_year_month UNIQUE (vehicle_model_id, year_code, reference_month)
);

CREATE INDEX ix_price_entry_vehicle_model_id ON price_entry (vehicle_model_id);
