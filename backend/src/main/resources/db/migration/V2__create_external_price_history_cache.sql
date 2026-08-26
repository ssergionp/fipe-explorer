-- Cache do histórico mensal de preço vindo da API pública da FIPE
-- (fipe.parallelum.com.br/api/v2), consultada sob demanda. Guardamos tanto sucesso quanto
-- "não encontrado" para não bater na API de novo dentro do TTL; RATE_LIMITED e falha de rede não
-- são gravados aqui (são transitórios, ver CalendarHistoryService).
CREATE TABLE external_price_history (
    id            BIGSERIAL PRIMARY KEY,
    vehicle_type  VARCHAR(20)  NOT NULL,
    fipe_code     VARCHAR(20)  NOT NULL,
    year_code     VARCHAR(20)  NOT NULL,
    status        VARCHAR(20)  NOT NULL,
    payload       TEXT,
    fetched_at    TIMESTAMP    NOT NULL,
    CONSTRAINT uq_external_price_history UNIQUE (vehicle_type, fipe_code, year_code)
);
