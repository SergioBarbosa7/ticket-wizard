package br.com.ticket.wizard.gateway.web.dto;

import br.com.ticket.wizard.grpc.pedidos.Pedido;
import com.google.protobuf.Timestamp;

import java.time.Instant;
import java.util.List;

public record PedidoDto(String id,
                        String categoriaId,
                        String eventoId,
                        String nomeCategoria,
                        int quantidade,
                        long valorTotalCentavos,
                        String status,
                        String motivoFalha,
                        List<String> ingressoIds,
                        Instant criadoEm) {

    public static PedidoDto de(Pedido proto) {
        return new PedidoDto(proto.getId(), proto.getCategoriaId(), vazioParaNulo(proto.getEventoId()),
                             vazioParaNulo(proto.getNomeCategoria()), proto.getQuantidade(),
                             proto.getValorTotalCentavos(), proto.getStatus().name(),
                             vazioParaNulo(proto.getMotivoFalha()), proto.getIngressoIdsList(),
                             instante(proto.getCriadoEm()));
    }

    // Em Protobuf 3, string ausente chega como "". No JSON, ausente é null.
    private static String vazioParaNulo(String valor) {
        return valor.isEmpty() ? null : valor;
    }

    static Instant instante(Timestamp timestamp) {
        return Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos());
    }
}
