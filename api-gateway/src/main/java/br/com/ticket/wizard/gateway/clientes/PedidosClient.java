package br.com.ticket.wizard.gateway.clientes;

import br.com.ticket.wizard.gateway.web.dto.PedidoDto;
import br.com.ticket.wizard.gateway.web.dto.PedidoRequest;
import br.com.ticket.wizard.grpc.pedidos.ListarPedidosRequest;
import br.com.ticket.wizard.grpc.pedidos.PedidoServiceGrpc;
import br.com.ticket.wizard.grpc.pedidos.RealizarPedidoRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class PedidosClient {

    private static final Logger LOG = LoggerFactory.getLogger(PedidosClient.class);

    private final PedidoServiceGrpc.PedidoServiceBlockingStub stub;

    public PedidosClient(PedidoServiceGrpc.PedidoServiceBlockingStub stub) {
        this.stub = stub;
    }

    public PedidoDto realizar(PedidoRequest dados, UUID usuarioId) {
        LOG.info("gRPC -> pedidos-service.RealizarPedido");
        return PedidoDto.de(stub.realizarPedido(RealizarPedidoRequest.newBuilder()
                                                                     .setUsuarioId(usuarioId.toString())
                                                                     .setCategoriaId(dados.categoriaId().toString())
                                                                     .setQuantidade(dados.quantidade())
                                                                     .build()));
    }

    public List<PedidoDto> listar(UUID usuarioId) {
        LOG.info("gRPC -> pedidos-service.ListarPedidos");
        var resposta = stub.listarPedidos(ListarPedidosRequest.newBuilder()
                                                              .setUsuarioId(usuarioId.toString())
                                                              .build());
        return resposta.getPedidosList().stream().map(PedidoDto::de).toList();
    }
}
