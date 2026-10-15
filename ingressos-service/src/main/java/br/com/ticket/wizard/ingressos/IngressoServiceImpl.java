package br.com.ticket.wizard.ingressos;

import br.com.ticket.wizard.base.Erros;
import br.com.ticket.wizard.base.Tempo;
import br.com.ticket.wizard.base.Transacao;
import br.com.ticket.wizard.base.Validacao;
import br.com.ticket.wizard.grpc.ingressos.CategoriaRequest;
import br.com.ticket.wizard.grpc.ingressos.EmitirIngressosRequest;
import br.com.ticket.wizard.grpc.ingressos.EmitirIngressosResponse;
import br.com.ticket.wizard.grpc.ingressos.Ingresso;
import br.com.ticket.wizard.grpc.ingressos.IngressoServiceGrpc;
import br.com.ticket.wizard.grpc.ingressos.ListarCategoriasRequest;
import br.com.ticket.wizard.grpc.ingressos.ListarCategoriasResponse;
import br.com.ticket.wizard.grpc.ingressos.ListarIngressosRequest;
import br.com.ticket.wizard.grpc.ingressos.ListarIngressosResponse;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.util.List;
import java.util.UUID;

public class IngressoServiceImpl extends IngressoServiceGrpc.IngressoServiceImplBase {

    private static final Logger LOG = LoggerFactory.getLogger(IngressoServiceImpl.class);

    private final DataSource dataSource;
    private final CategoriaRepositorio categorias;
    private final IngressoRepositorio ingressos;

    public IngressoServiceImpl(DataSource dataSource, CategoriaRepositorio categorias, IngressoRepositorio ingressos) {
        this.dataSource = dataSource;
        this.categorias = categorias;
        this.ingressos = ingressos;
    }

    @Override
    public void cadastrarCategoriaIngresso(CategoriaRequest request,
                                           StreamObserver<br.com.ticket.wizard.grpc.ingressos.Categoria> responseObserver) {
        String eventoId = Validacao.texto("evento_id", request.getEventoId(), 100);
        String nome = Validacao.texto("nome_categoria", request.getNomeCategoria(), 100);
        if (request.getPrecoCentavos() <= 0) {
            throw Erros.argumentoInvalido("O preço deve ser maior que zero.");
        }
        if (request.getQuantidadeDisponivel() <= 0) {
            throw Erros.argumentoInvalido("A quantidade disponível deve ser maior que zero.");
        }
        UUID criadoPor = request.getCriadoPor().isBlank() ? null : Validacao.uuid("criado_por", request.getCriadoPor());

        Categoria nova = new Categoria(UUID.randomUUID(), eventoId, nome, request.getPrecoCentavos(),
                                       request.getQuantidadeDisponivel(), request.getQuantidadeDisponivel());
        Transacao.executar(dataSource, conexao -> categorias.inserir(conexao, nova, criadoPor));
        LOG.info("Categoria cadastrada: evento={}, nome={}, preco={}c, quantidade={}",
                 eventoId, nome, nova.precoCentavos(), nova.quantidadeTotal());

        responseObserver.onNext(paraProto(nova));
        responseObserver.onCompleted();
    }

    @Override
    public void listarCategorias(ListarCategoriasRequest request,
                                 StreamObserver<ListarCategoriasResponse> responseObserver) {
        List<Categoria> lista = Transacao.executar(dataSource,
                                                   conexao -> categorias.listar(conexao, request.getEventoId()));

        ListarCategoriasResponse.Builder resposta = ListarCategoriasResponse.newBuilder();
        lista.forEach(categoria -> resposta.addCategorias(paraProto(categoria)));
        responseObserver.onNext(resposta.build());
        responseObserver.onCompleted();
    }

    @Override
    public void emitirIngressos(EmitirIngressosRequest request,
                                StreamObserver<EmitirIngressosResponse> responseObserver) {
        UUID categoriaId = Validacao.uuid("categoria_id", request.getCategoriaId());
        UUID usuarioId = Validacao.uuid("usuario_id", request.getUsuarioId());
        UUID pedidoId = request.getPedidoId().isBlank() ? null : Validacao.uuid("pedido_id", request.getPedidoId());
        int quantidade = Validacao.quantidadeDoPedido(request.getQuantidade());

        EmitirIngressosResponse resposta = Transacao.executar(dataSource, conexao -> {
            var atualizada = categorias.baixarEstoque(conexao, categoriaId, quantidade);

            if (atualizada.isEmpty()) {
                var existente = categorias.buscar(conexao, categoriaId)
                                          .orElseThrow(() -> Erros.naoEncontrado("Categoria não encontrada."));
                throw Erros.precondicaoFalhou(existente.quantidadeDisponivel() == 0
                                              ? "Categoria esgotada."
                                              : "Estoque insuficiente: restam " + existente.quantidadeDisponivel()
                                                + " ingresso(s).");
            }

            Categoria categoria = atualizada.get();
            List<UUID> ids = ingressos.inserir(conexao, categoriaId, usuarioId, pedidoId, quantidade);
            return EmitirIngressosResponse.newBuilder()
                                          .addAllIngressoIds(ids.stream().map(UUID::toString).toList())
                                          .setCategoria(paraProto(categoria))
                                          .setValorTotalCentavos(categoria.precoCentavos() * quantidade)
                                          .build();
        });

        LOG.info("Emitidos {} ingresso(s): categoria={}, usuario={}, restam {}",
                 quantidade, categoriaId, usuarioId, resposta.getCategoria().getQuantidadeDisponivel());
        responseObserver.onNext(resposta);
        responseObserver.onCompleted();
    }

    @Override
    public void listarIngressos(ListarIngressosRequest request,
                                StreamObserver<ListarIngressosResponse> responseObserver) {
        UUID usuarioId = Validacao.uuid("usuario_id", request.getUsuarioId());
        List<IngressoEmitido> lista = Transacao.executar(dataSource,
                                                         conexao -> ingressos.listarPorUsuario(conexao, usuarioId));

        ListarIngressosResponse.Builder resposta = ListarIngressosResponse.newBuilder();
        for (IngressoEmitido emitido : lista) {
            resposta.addIngressos(Ingresso.newBuilder()
                                          .setId(emitido.id().toString())
                                          .setCategoriaId(emitido.categoriaId().toString())
                                          .setEventoId(emitido.eventoId())
                                          .setNomeCategoria(emitido.nomeCategoria())
                                          .setUsuarioId(emitido.usuarioId().toString())
                                          .setPedidoId(emitido.pedidoId() == null ? "" : emitido.pedidoId().toString())
                                          .setEmitidoEm(Tempo.paraTimestamp(emitido.emitidoEm())));
        }
        responseObserver.onNext(resposta.build());
        responseObserver.onCompleted();
    }

    private static br.com.ticket.wizard.grpc.ingressos.Categoria paraProto(Categoria categoria) {
        return br.com.ticket.wizard.grpc.ingressos.Categoria.newBuilder()
                                                            .setId(categoria.id().toString())
                                                            .setEventoId(categoria.eventoId())
                                                            .setNome(categoria.nome())
                                                            .setPrecoCentavos(categoria.precoCentavos())
                                                            .setQuantidadeTotal(categoria.quantidadeTotal())
                                                            .setQuantidadeDisponivel(categoria.quantidadeDisponivel())
                                                            .build();
    }
}
