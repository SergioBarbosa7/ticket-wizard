package br.com.ticket.wizard.pedidos;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record Pedido(UUID id,
                     UUID usuarioId,
                     UUID categoriaId,
                     String eventoId,
                     String nomeCategoria,
                     int quantidade,
                     long valorTotalCentavos,
                     String status,
                     String motivoFalha,
                     List<UUID> ingressoIds,
                     OffsetDateTime criadoEm) {
}
