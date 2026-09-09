package br.com.ticket.wizard.ingressos;

import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.IOException;

// Inicializa o servidor gRPC e publica a implementação do serviço de ingressos.
public class ServidorIngressosGrpc {

    private static final int PORTA = 50051;

    public static void main(String[] args) throws IOException, InterruptedException {
        // Registra o serviço que atenderá os métodos definidos no arquivo .proto.
        Server servidor = ServerBuilder.forPort(PORTA)
                                       .addService(new IngressoServiceImpl())
                                       .build()
                                       .start();

        System.out.println("Servidor gRPC iniciado na porta " + PORTA);

        // Mantém o processo ativo enquanto o servidor espera novas chamadas.
        servidor.awaitTermination();
    }
}
