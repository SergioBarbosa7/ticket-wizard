CREATE TABLE usuarios (
    id         UUID PRIMARY KEY,
    nome       VARCHAR(120) NOT NULL,
    email      VARCHAR(254) NOT NULL,
    senha_hash VARCHAR(255) NOT NULL,
    papel      VARCHAR(20)  NOT NULL CHECK (papel IN ('COMPRADOR', 'ORGANIZADOR')),
    criado_em  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT email_unico UNIQUE (email)
);
