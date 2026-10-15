package br.com.ticket.wizard.gateway.clientes;

import br.com.ticket.wizard.gateway.web.dto.CategoriaDto;
import br.com.ticket.wizard.gateway.web.dto.CategoriaRequest;
import br.com.ticket.wizard.gateway.web.dto.IngressoDto;
import br.com.ticket.wizard.grpc.ingressos.IngressoServiceGrpc;
import br.com.ticket.wizard.grpc.ingressos.ListarCategoriasRequest;
import br.com.ticket.wizard.grpc.ingressos.ListarIngressosRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class IngressosClient {

    private static final Logger LOG = LoggerFactory.getLogger(IngressosClient.class);

    private final IngressoServiceGrpc.IngressoServiceBlockingStub stub;

    public IngressosClient(IngressoServiceGrpc.IngressoServiceBlockingStub stub) {
        this.stub = stub;
    }

    public List<CategoriaDto> listarCategorias(String eventoId) {
        LOG.info("gRPC -> ingressos-service.ListarCategorias");
        var resposta = stub.listarCategorias(ListarCategoriasRequest.newBuilder()
                                                                    .setEventoId(eventoId == null ? "" : eventoId)
                                                                    .build());
        return resposta.getCategoriasList().stream().map(CategoriaDto::de).toList();
    }

    public CategoriaDto cadastrarCategoria(CategoriaRequest dados, UUID organizadorId) {
        LOG.info("gRPC -> ingressos-service.CadastrarCategoriaIngresso");
        var categoria = stub.cadastrarCategoriaIngresso(
                br.com.ticket.wizard.grpc.ingressos.CategoriaRequest.newBuilder()
                                                                     .setEventoId(dados.eventoId())
                                                                     .setNomeCategoria(dados.nome())
                                                                     .setPrecoCentavos(dados.precoCentavos())
                                                                     .setQuantidadeDisponivel(dados.quantidade())
                                                                     .setCriadoPor(organizadorId.toString())
                                                                     .build());
        return CategoriaDto.de(categoria);
    }

    public List<IngressoDto> listarIngressos(UUID usuarioId) {
        LOG.info("gRPC -> ingressos-service.ListarIngressos");
        var resposta = stub.listarIngressos(ListarIngressosRequest.newBuilder()
                                                                  .setUsuarioId(usuarioId.toString())
                                                                  .build());
        return resposta.getIngressosList().stream().map(IngressoDto::de).toList();
    }
}
