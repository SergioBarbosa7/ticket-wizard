package br.com.ticket.wizard.gateway.web.dto;

import br.com.ticket.wizard.grpc.ingressos.Ingresso;

import java.time.Instant;

public record IngressoDto(String id,
                          String categoriaId,
                          String eventoId,
                          String nomeCategoria,
                          String pedidoId,
                          Instant emitidoEm) {

    public static IngressoDto de(Ingresso proto) {
        return new IngressoDto(proto.getId(), proto.getCategoriaId(), proto.getEventoId(), proto.getNomeCategoria(),
                               proto.getPedidoId().isEmpty() ? null : proto.getPedidoId(),
                               Instant.ofEpochSecond(proto.getEmitidoEm().getSeconds(),
                                                     proto.getEmitidoEm().getNanos()));
    }
}
