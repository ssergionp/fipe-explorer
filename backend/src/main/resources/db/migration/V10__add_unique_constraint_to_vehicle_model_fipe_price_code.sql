-- Formaliza uma premissa que já era verdade nos dados (11.357 modelos, 11.357 códigos distintos,
-- verificado antes de aplicar esta migration) mas nunca foi garantida pelo schema. watched_vehicle
-- (fase 5) passa a depender disso: observar um fipe_code só faz sentido se ele mapear pra um único
-- vehicle_model.
ALTER TABLE vehicle_model ADD CONSTRAINT uq_vehicle_model_fipe_price_code UNIQUE (fipe_price_code);
