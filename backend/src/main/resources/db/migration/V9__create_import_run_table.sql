-- Um registro por mês de referência já importado - é o que evita reimportar o mesmo mês duas
-- vezes, mesmo que o arquivo seja baixado de novo com outro nome (dedup é por
-- reference_month_key, não por filename).
CREATE TABLE import_run (
    id                    BIGSERIAL      PRIMARY KEY,
    filename              VARCHAR(255)   NOT NULL,
    reference_month       VARCHAR(30)    NOT NULL,
    reference_month_key   DATE           NOT NULL,
    row_count             INTEGER        NOT NULL,
    imported_at           TIMESTAMP      NOT NULL DEFAULT now(),
    CONSTRAINT uq_import_run_reference_month_key UNIQUE (reference_month_key)
);

-- Backfill retroativo: sem isso, um banco que já tem o CSV semente importado (praticamente
-- qualquer ambiente existente) tentaria reimportá-lo inteiro na próxima subida, batendo na
-- constraint UNIQUE de price_entry. Registra qualquer mês já presente nos dados, não só o atual.
INSERT INTO import_run (filename, reference_month, reference_month_key, row_count, imported_at)
SELECT 'tabela-fipe-336.csv', reference_month, reference_month_key, count(*), now()
FROM price_entry
GROUP BY reference_month, reference_month_key;
