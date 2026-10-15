package br.com.ticket.wizard.ingressos;

import java.time.OffsetDateTime;
import java.util.UUID;

public record IngressoEmitido(UUID id,
                              UUID categoriaId,
                              String eventoId,
                              String nomeCategoria,
                              UUID usuarioId,
                              UUID pedidoId,
                              OffsetDateTime emitidoEm) {
}
