package br.com.ticket.wizard.gateway.web.dto;

import br.com.ticket.wizard.grpc.ingressos.Categoria;

public record CategoriaDto(String id,
                           String eventoId,
                           String nome,
                           long precoCentavos,
                           int quantidadeTotal,
                           int quantidadeDisponivel) {

    public static CategoriaDto de(Categoria proto) {
        return new CategoriaDto(proto.getId(), proto.getEventoId(), proto.getNome(), proto.getPrecoCentavos(),
                                proto.getQuantidadeTotal(), proto.getQuantidadeDisponivel());
    }
}
