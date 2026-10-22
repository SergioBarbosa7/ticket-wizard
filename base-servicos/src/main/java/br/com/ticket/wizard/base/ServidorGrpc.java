package br.com.ticket.wizard.base;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public final class ServidorGrpc {

    private static final Logger LOG = LoggerFactory.getLogger(ServidorGrpc.class);

    private ServidorGrpc() {
    }

    public static void executar(String nome, int porta, BindableService... servicos)
            throws IOException, InterruptedException {
        ServerBuilder<?> builder = ServerBuilder.forPort(porta).intercept(new TratadorDeExcecoes());
        for (BindableService servico : servicos) {
            builder.addService(servico);
        }
        Server servidor = builder.build().start();
        LOG.info("{} gRPC iniciado na porta {}", nome, porta);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            servidor.shutdown();
            try {
                servidor.awaitTermination(10, TimeUnit.SECONDS);
            } catch (InterruptedException erro) {
                Thread.currentThread().interrupt();
            }
        }));

        servidor.awaitTermination();
    }
}
