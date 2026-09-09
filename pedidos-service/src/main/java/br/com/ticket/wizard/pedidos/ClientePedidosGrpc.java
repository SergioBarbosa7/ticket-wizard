package br.com.ticket.wizard.pedidos;

import br.com.ticket.wizard.grpc.CategoriaRequest;
import br.com.ticket.wizard.grpc.CategoriaResponse;
import br.com.ticket.wizard.grpc.IngressoResponse;
import br.com.ticket.wizard.grpc.IngressoServiceGrpc;
import br.com.ticket.wizard.grpc.PedidoRequest;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

// Cliente que abre um canal, chama os métodos remotos e exibe as respostas.
public class ClientePedidosGrpc {

    // Endereço do ingressos-service. Na nuvem, basta exportar a variável de ambiente.
    private static final String ALVO_PADRAO = "localhost:50051";
    private static final String VARIAVEL_DE_AMBIENTE = "INGRESSOS_SERVICE_HOST";

    // Dados da demonstração.
    private static final String EVENTO_ID = "evento-001";
    private static final String USER_ID = "user-42";
    private static final String NOME_CATEGORIA = "Pista";
    private static final double PRECO = 100.0;

    // Valores usados quando os argumentos não são informados na execução.
    private static final int INGRESSOS_A_EMITIR_PADRAO = 1;
    private static final int QUANTIDADE_DISPONIVEL_PADRAO = 500;

    private final IngressoServiceGrpc.IngressoServiceBlockingStub stub;

    public ClientePedidosGrpc(ManagedChannel canal) {
        // O stub (ponta/canal/cliente) bloqueante aguarda a resposta antes de continuar a execução.
        this.stub = IngressoServiceGrpc.newBlockingStub(canal);
    }

    // Argumentos da execução: [ingressos a emitir] [quantidade disponível da categoria].
    public static void main(String[] args) {
        int ingressosAEmitir = lerInteiro(args, 0, INGRESSOS_A_EMITIR_PADRAO, "Quantidade de ingressos");
        int quantidadeDisponivel = lerInteiro(args, 1, QUANTIDADE_DISPONIVEL_PADRAO, "Quantidade disponível");

        String alvo = System.getenv().getOrDefault(VARIAVEL_DE_AMBIENTE, ALVO_PADRAO);
        System.out.println("Chamando o ingressos-service em " + alvo);
        System.out.printf("Categoria com %d ingressos, emitindo %d.%n", quantidadeDisponivel, ingressosAEmitir);

        // O canal mantém a conexão usada pelo cliente para se comunicar com o servidor.
        ManagedChannel canal = ManagedChannelBuilder.forTarget(alvo).usePlaintext().build();
        ClientePedidosGrpc cliente = new ClientePedidosGrpc(canal);

        // Fluxo da demonstração: cadastra a categoria e emite os ingressos dela.
        String categoriaId = cliente.cadastrarCategoria(EVENTO_ID, NOME_CATEGORIA, PRECO, quantidadeDisponivel);

        // Cada volta do laço é um RPC independente. O servidor decrementa a quantidade
        // a cada emissão, então dá para acompanhar o saldo caindo nas respostas.
        for (int numero = 1; numero <= ingressosAEmitir; numero++) {
            cliente.emitirIngresso(EVENTO_ID, USER_ID, categoriaId, numero);
        }

        // Libera os recursos de rede mantidos pelo canal.
        canal.shutdown();
    }

    // Lê um número da posição indicada dos argumentos. Sem argumento ou com valor
    // inválido, avisa no console e segue com o padrão em vez de derrubar a demonstração.
    private static int lerInteiro(String[] args, int indice, int padrao, String descricao) {
        if (args.length <= indice) {
            return padrao;
        }

        try {
            int valor = Integer.parseInt(args[indice].trim());
            if (valor >= 0) {
                return valor;
            }
        } catch (NumberFormatException erro) {
            // Valor não numérico: cai no aviso abaixo.
        }

        System.out.printf("%s inválida: \"%s\". Usando %d.%n", descricao, args[indice], padrao);
        return padrao;
    }

    public String cadastrarCategoria(String eventoId, String nome, double preco, int quantidade) {
        // A classe e seu builder foram gerados a partir de CategoriaRequest no .proto.
        CategoriaRequest request = CategoriaRequest.newBuilder()
                                                   .setEventoId(eventoId)
                                                   .setNomeCategoria(nome)
                                                   .setPreco(preco)
                                                   .setQuantidadeDisponivel(quantidade)
                                                   .build();

        // O stub serializa a requisição, realiza o RPC e desserializa a resposta.
        CategoriaResponse response = stub.cadastrarCategoriaIngresso(request);

        System.out.println();
        System.out.println("== Cadastro de categoria ==");
        System.out.println("Categoria ID: " + response.getCategoriaId());
        System.out.println("Status: " + response.getStatus());
        System.out.println("Mensagem: " + response.getMensagem());

        return response.getCategoriaId();
    }

    public void emitirIngresso(String eventoId, String userId, String categoriaId, int numero) {
        PedidoRequest request = PedidoRequest.newBuilder()
                                             .setEventoId(eventoId)
                                             .setUserId(userId)
                                             .setCategoriaId(categoriaId)
                                             .build();

        IngressoResponse response = stub.emitirIngresso(request);

        System.out.println();
        System.out.println("== Emissão de ingresso " + numero + " ==");
        System.out.println("Ingresso ID: " + response.getIngressoId());
        System.out.println("Status: " + response.getStatus());
        System.out.println("Mensagem: " + response.getMensagem());
    }
}
