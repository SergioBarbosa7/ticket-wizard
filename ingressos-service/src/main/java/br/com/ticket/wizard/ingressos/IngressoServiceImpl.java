package br.com.ticket.wizard.ingressos;

import br.com.ticket.wizard.grpc.CategoriaRequest;
import br.com.ticket.wizard.grpc.CategoriaResponse;
import br.com.ticket.wizard.grpc.IngressoResponse;
import br.com.ticket.wizard.grpc.IngressoServiceGrpc;
import br.com.ticket.wizard.grpc.PedidoRequest;
import io.grpc.stub.StreamObserver;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// Implementa no servidor o contrato gerado a partir de IngressoService no .proto.
public class IngressoServiceImpl extends IngressoServiceGrpc.IngressoServiceImplBase {

    // Estado em memória desta fase: nenhum banco de dados ainda.
    private final Map<String, Categoria> categorias = new ConcurrentHashMap<>();
    private final Map<String, Ingresso> ingressos = new ConcurrentHashMap<>();

    // Categoria de ingresso de um evento. A quantidade cai a cada emissão.
    private static final class Categoria {
        private final String eventoId;
        private final String nome;
        private final double preco;
        private int quantidadeDisponivel;

        private Categoria(String eventoId, String nome, double preco, int quantidadeDisponivel) {
            this.eventoId = eventoId;
            this.nome = nome;
            this.preco = preco;
            this.quantidadeDisponivel = quantidadeDisponivel;
        }
    }

    // Ingresso já emitido para um comprador.
    private record Ingresso(String categoriaId, String userId) {
    }

    @Override
    // request é a única entrada declarada no .proto. O gRPC adiciona responseObserver
    // à API Java do servidor para enviar a resposta; por isso o método retorna void.
    public void cadastrarCategoriaIngresso(CategoriaRequest request,
                                           StreamObserver<CategoriaResponse> responseObserver) {
        // A requisição já chega desserializada como uma mensagem Protobuf.
        System.out.printf("Categoria recebida: evento=%s, nome=%s, preco=%.2f, quantidade=%d%n",
                          request.getEventoId(),
                          request.getNomeCategoria(),
                          request.getPreco(),
                          request.getQuantidadeDisponivel());

        String categoriaId = UUID.randomUUID().toString();
        categorias.put(categoriaId, new Categoria(request.getEventoId(),
                                                  request.getNomeCategoria(),
                                                  request.getPreco(),
                                                  request.getQuantidadeDisponivel()));

        // A resposta também é uma mensagem imutável gerada pelo Protobuf.
        CategoriaResponse response = CategoriaResponse.newBuilder()
                                                      .setCategoriaId(categoriaId)
                                                      .setStatus("OK")
                                                      .setMensagem("Categoria cadastrada com sucesso.")
                                                      .build();

        // Envia a resposta ao cliente e sinaliza que o RPC terminou com sucesso.
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void emitirIngresso(PedidoRequest request, StreamObserver<IngressoResponse> responseObserver) {
        System.out.printf("Emissão solicitada: evento=%s, usuario=%s, categoria=%s%n",
                          request.getEventoId(),
                          request.getUserId(),
                          request.getCategoriaId());

        Categoria categoria = categorias.get(request.getCategoriaId());

        // Erros de negócio voltam como status na própria resposta, sem lançar exceção.
        if (categoria == null) {
            responseObserver.onNext(erro("Categoria não encontrada."));
            responseObserver.onCompleted();
            return;
        }

        if (categoria.quantidadeDisponivel <= 0) {
            responseObserver.onNext(erro("Categoria esgotada."));
            responseObserver.onCompleted();
            return;
        }

        categoria.quantidadeDisponivel--;
        String ingressoId = UUID.randomUUID().toString();
        ingressos.put(ingressoId, new Ingresso(request.getCategoriaId(), request.getUserId()));

        IngressoResponse response = IngressoResponse.newBuilder()
                                                    .setIngressoId(ingressoId)
                                                    .setStatus("OK")
                                                    .setMensagem(String.format(
                                                            "Ingresso emitido: %s do evento %s por R$ %.2f. Restam %d.",
                                                            categoria.nome,
                                                            categoria.eventoId,
                                                            categoria.preco,
                                                            categoria.quantidadeDisponivel))
                                                    .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    private IngressoResponse erro(String mensagem) {
        return IngressoResponse.newBuilder().setIngressoId("").setStatus("ERRO").setMensagem(mensagem).build();
    }
}
