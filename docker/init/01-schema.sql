-- This is performed automatically ONLY on the FIRST startup,
-- when the data volume is still empty. On behalf of POSTGRES_USER.

CREATE TABLE IF NOT EXISTS users (
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(255) NOT NULL,
    email      VARCHAR(255) NOT NULL UNIQUE,
    age        INTEGER,
    created_at TIMESTAMP(6) NOT NULL
);
