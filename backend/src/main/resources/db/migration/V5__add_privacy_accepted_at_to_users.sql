-- Registro mínimo de consentimento: só data/hora do aceite, sem versionamento de política.
-- Default now() só existe para retrocompatibilidade com linhas já existentes; todo cadastro novo
-- sempre define este valor explicitamente (ver User.onCreate) e nunca chega a usar o default.
ALTER TABLE users ADD COLUMN privacy_accepted_at TIMESTAMP NOT NULL DEFAULT now();
