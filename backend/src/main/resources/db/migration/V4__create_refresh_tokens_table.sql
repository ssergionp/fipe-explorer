CREATE TABLE refresh_tokens (
    id           BIGSERIAL PRIMARY KEY,
    token        VARCHAR(255)  NOT NULL,
    user_id      BIGINT        NOT NULL REFERENCES users (id),
    expiry_date  TIMESTAMP     NOT NULL,
    revoked      BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMP     NOT NULL DEFAULT now(),
    CONSTRAINT uq_refresh_tokens_token UNIQUE (token)
);

CREATE INDEX ix_refresh_tokens_user_id ON refresh_tokens (user_id);
