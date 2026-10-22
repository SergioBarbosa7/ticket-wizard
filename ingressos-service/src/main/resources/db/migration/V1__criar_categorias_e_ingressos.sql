CREATE TABLE categorias (
    id                    UUID PRIMARY KEY,
    evento_id             VARCHAR(100) NOT NULL,
    nome                  VARCHAR(100) NOT NULL,
    preco_centavos        BIGINT       NOT NULL CHECK (preco_centavos > 0),
    quantidade_total      INTEGER      NOT NULL CHECK (quantidade_total > 0),
    quantidade_disponivel INTEGER      NOT NULL,
    criado_por            UUID,
    criado_em             TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT estoque_valido CHECK (quantidade_disponivel >= 0 AND quantidade_disponivel <= quantidade_total),
    CONSTRAINT categoria_unica_por_evento UNIQUE (evento_id, nome)
);

CREATE TABLE ingressos (
    id           UUID PRIMARY KEY,
    categoria_id UUID        NOT NULL REFERENCES categorias (id),
    usuario_id   UUID        NOT NULL,
    pedido_id    UUID,
    emitido_em   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_ingressos_usuario ON ingressos (usuario_id, emitido_em DESC);
