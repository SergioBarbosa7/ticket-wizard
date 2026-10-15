CREATE TABLE pedidos (
    id                   UUID PRIMARY KEY,
    usuario_id           UUID        NOT NULL,
    categoria_id         UUID        NOT NULL,
    -- Dados da categoria copiados no momento da compra (o dono deles é o ingressos-service).
    evento_id            VARCHAR(100),
    nome_categoria       VARCHAR(100),
    quantidade           INTEGER     NOT NULL CHECK (quantidade > 0),
    valor_total_centavos BIGINT      NOT NULL DEFAULT 0 CHECK (valor_total_centavos >= 0),
    status               VARCHAR(20) NOT NULL CHECK (status IN ('PENDENTE', 'CONFIRMADO', 'FALHOU')),
    motivo_falha         VARCHAR(255),
    criado_em            TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE pedido_ingressos (
    pedido_id   UUID NOT NULL REFERENCES pedidos (id),
    ingresso_id UUID NOT NULL,
    PRIMARY KEY (pedido_id, ingresso_id)
);

CREATE INDEX idx_pedidos_usuario ON pedidos (usuario_id, criado_em DESC);
