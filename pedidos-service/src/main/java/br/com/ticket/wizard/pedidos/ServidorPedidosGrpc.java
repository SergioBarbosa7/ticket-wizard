package br.com.ticket.wizard.pedidos;

import br.com.ticket.wizard.base.Ambiente;
import br.com.ticket.wizard.base.BancoDeDados;
import br.com.ticket.wizard.base.ServidorGrpc;
import br.com.ticket.wizard.grpc.ingressos.IngressoServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import javax.sql.DataSource;
import java.io.IOException;

public class ServidorPedidosGrpc {

    private static final String VARIAVEL_INGRESSOS = "INGRESSOS_SERVICE_HOST";

    public static void main(String[] args) throws IOException, InterruptedException {
        DataSource dataSource = BancoDeDados.conectar("pedidos_db");

        // O canal conecta sob demanda e reconecta sozinho: o pedidos-service pode subir antes do ingressos.
        ManagedChannel canalIngressos = ManagedChannelBuilder
                .forTarget(Ambiente.ou(VARIAVEL_INGRESSOS, "localhost:50051"))
                .usePlaintext()
                .build();
        var servico = new PedidoServiceImpl(dataSource, new PedidoRepositorio(),
                                            IngressoServiceGrpc.newBlockingStub(canalIngressos));

        ServidorGrpc.executar("pedidos-service", Ambiente.inteiro("GRPC_PORT", 50052), servico);
    }
}
