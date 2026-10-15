package br.com.ticket.wizard.pedidos;

import br.com.ticket.wizard.base.Tempo;
import br.com.ticket.wizard.base.Transacao;
import br.com.ticket.wizard.base.Validacao;
import br.com.ticket.wizard.grpc.ingressos.EmitirIngressosRequest;
import br.com.ticket.wizard.grpc.ingressos.EmitirIngressosResponse;
import br.com.ticket.wizard.grpc.ingressos.IngressoServiceGrpc;
import br.com.ticket.wizard.grpc.pedidos.ListarPedidosRequest;
import br.com.ticket.wizard.grpc.pedidos.ListarPedidosResponse;
import br.com.ticket.wizard.grpc.pedidos.PedidoServiceGrpc;
import br.com.ticket.wizard.grpc.pedidos.RealizarPedidoRequest;
import br.com.ticket.wizard.grpc.pedidos.StatusPedido;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class PedidoServiceImpl extends PedidoServiceGrpc.PedidoServiceImplBase {

    private static final Logger LOG = LoggerFactory.getLogger(PedidoServiceImpl.class);

    private static final long PRAZO_INGRESSOS_SEGUNDOS = 10;

    // Erros em que o ingressos-service recusou o pedido de forma definitiva: nada foi emitido.
    private static final Set<Status.Code> RECUSAS_DE_NEGOCIO = Set.of(
            Status.Code.INVALID_ARGUMENT, Status.Code.NOT_FOUND, Status.Code.FAILED_PRECONDITION);

    private final DataSource dataSource;
    private final PedidoRepositorio pedidos;
    private final IngressoServiceGrpc.IngressoServiceBlockingStub ingressos;

    public PedidoServiceImpl(DataSource dataSource, PedidoRepositorio pedidos,
                             IngressoServiceGrpc.IngressoServiceBlockingStub ingressos) {
        this.dataSource = dataSource;
        this.pedidos = pedidos;
        this.ingressos = ingressos;
    }

    @Override
    public void realizarPedido(RealizarPedidoRequest request,
                               StreamObserver<br.com.ticket.wizard.grpc.pedidos.Pedido> responseObserver) {
        UUID usuarioId = Validacao.uuid("usuario_id", request.getUsuarioId());
        UUID categoriaId = Validacao.uuid("categoria_id", request.getCategoriaId());
        int quantidade = Validacao.quantidadeDoPedido(request.getQuantidade());
        UUID pedidoId = UUID.randomUUID();

        // 1. Registra o pedido como PENDENTE, em transação própria já confirmada. Não há
        //    transação distribuída entre os dois bancos; se o processo cair daqui em diante,
        //    o pedido continua visível como PENDENTE em vez de sumir.
        Transacao.executar(dataSource, conexao -> {
            pedidos.inserirPendente(conexao, pedidoId, usuarioId, categoriaId, quantidade);
            return null;
        });

        EmitirIngressosResponse emissao;
        try {
            emissao = ingressos.withDeadlineAfter(PRAZO_INGRESSOS_SEGUNDOS, TimeUnit.SECONDS)
                               .emitirIngressos(EmitirIngressosRequest.newBuilder()
                                                                      .setCategoriaId(categoriaId.toString())
                                                                      .setUsuarioId(usuarioId.toString())
                                                                      .setQuantidade(quantidade)
                                                                      .setPedidoId(pedidoId.toString())
                                                                      .build());
        } catch (StatusRuntimeException erro) {
            Status status = erro.getStatus();
            if (RECUSAS_DE_NEGOCIO.contains(status.getCode())) {
                String motivo = status.getDescription() == null ? status.getCode().name() : status.getDescription();
                Transacao.executar(dataSource, conexao -> {
                    pedidos.falhar(conexao, pedidoId, motivo);
                    return null;
                });
                LOG.info("Pedido {} recusado pelo ingressos-service: {}", pedidoId, motivo);
            } else {
                // Timeout ou serviço fora do ar: não dá para saber se emitiu. Fica PENDENTE.
                LOG.warn("Pedido {} sem resposta definitiva do ingressos-service: {}", pedidoId, status);
            }
            throw status.getCode() == Status.Code.UNAVAILABLE || status.getCode() == Status.Code.DEADLINE_EXCEEDED
                    ? Status.UNAVAILABLE.withDescription("Serviço de ingressos indisponível. Tente novamente.")
                                        .asRuntimeException()
                    : status.asRuntimeException();
        }

        List<UUID> ingressoIds = emissao.getIngressoIdsList().stream().map(UUID::fromString).toList();
        Pedido confirmado = Transacao.executar(dataSource, conexao -> {
            pedidos.confirmar(conexao, pedidoId, emissao.getCategoria().getEventoId(),
                              emissao.getCategoria().getNome(), emissao.getValorTotalCentavos(), ingressoIds);
            return pedidos.buscar(conexao, pedidoId).orElseThrow();
        });
        LOG.info("Pedido {} confirmado: {} ingresso(s), total {}c", pedidoId, quantidade,
                 confirmado.valorTotalCentavos());

        responseObserver.onNext(paraProto(confirmado));
        responseObserver.onCompleted();
    }

    @Override
    public void listarPedidos(ListarPedidosRequest request, StreamObserver<ListarPedidosResponse> responseObserver) {
        UUID usuarioId = Validacao.uuid("usuario_id", request.getUsuarioId());
        List<Pedido> lista = Transacao.executar(dataSource, conexao -> pedidos.listarPorUsuario(conexao, usuarioId));

        ListarPedidosResponse.Builder resposta = ListarPedidosResponse.newBuilder();
        lista.forEach(pedido -> resposta.addPedidos(paraProto(pedido)));
        responseObserver.onNext(resposta.build());
        responseObserver.onCompleted();
    }

    private static br.com.ticket.wizard.grpc.pedidos.Pedido paraProto(Pedido pedido) {
        return br.com.ticket.wizard.grpc.pedidos.Pedido.newBuilder()
                .setId(pedido.id().toString())
                .setUsuarioId(pedido.usuarioId().toString())
                .setCategoriaId(pedido.categoriaId().toString())
                .setEventoId(pedido.eventoId() == null ? "" : pedido.eventoId())
                .setNomeCategoria(pedido.nomeCategoria() == null ? "" : pedido.nomeCategoria())
                .setQuantidade(pedido.quantidade())
                .setValorTotalCentavos(pedido.valorTotalCentavos())
                .setStatus(StatusPedido.valueOf(pedido.status()))
                .setMotivoFalha(pedido.motivoFalha() == null ? "" : pedido.motivoFalha())
                .addAllIngressoIds(pedido.ingressoIds().stream().map(UUID::toString).toList())
                .setCriadoEm(Tempo.paraTimestamp(pedido.criadoEm()))
                .build();
    }
}
