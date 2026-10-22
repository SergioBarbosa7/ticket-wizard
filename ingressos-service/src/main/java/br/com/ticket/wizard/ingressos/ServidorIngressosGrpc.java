package br.com.ticket.wizard.ingressos;

import br.com.ticket.wizard.base.Ambiente;
import br.com.ticket.wizard.base.BancoDeDados;
import br.com.ticket.wizard.base.ServidorGrpc;

import javax.sql.DataSource;
import java.io.IOException;

public class ServidorIngressosGrpc {

    public static void main(String[] args) throws IOException, InterruptedException {
        DataSource dataSource = BancoDeDados.conectar("ingressos_db");
        var servico = new IngressoServiceImpl(dataSource, new CategoriaRepositorio(), new IngressoRepositorio());

        ServidorGrpc.executar("ingressos-service", Ambiente.inteiro("GRPC_PORT", 50051), servico);
    }
}
