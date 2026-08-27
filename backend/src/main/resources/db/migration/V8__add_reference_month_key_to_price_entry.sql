-- reference_month é texto em português ("agosto de 2026") e não ordena cronologicamente como
-- string. reference_month_key é o dia 1 do mês em formato DATE, derivado do texto - preenchido
-- aqui pros dados já existentes; toda importação nova (FipeCsvImportService) passa a preencher
-- esta coluna diretamente no INSERT.
ALTER TABLE price_entry ADD COLUMN reference_month_key DATE;

UPDATE price_entry
SET reference_month_key = make_date(
    split_part(reference_month, ' de ', 2)::int,
    CASE split_part(reference_month, ' de ', 1)
        WHEN 'janeiro'   THEN 1
        WHEN 'fevereiro' THEN 2
        WHEN 'março'     THEN 3
        WHEN 'abril'     THEN 4
        WHEN 'maio'      THEN 5
        WHEN 'junho'     THEN 6
        WHEN 'julho'     THEN 7
        WHEN 'agosto'    THEN 8
        WHEN 'setembro'  THEN 9
        WHEN 'outubro'   THEN 10
        WHEN 'novembro'  THEN 11
        WHEN 'dezembro'  THEN 12
    END,
    1
);

ALTER TABLE price_entry ALTER COLUMN reference_month_key SET NOT NULL;

CREATE INDEX ix_price_entry_reference_month_key ON price_entry (reference_month_key);
