package br.com.ticket.wizard.usuarios;

import br.com.ticket.wizard.base.Ambiente;
import br.com.ticket.wizard.base.BancoDeDados;
import br.com.ticket.wizard.base.ServidorGrpc;

import javax.sql.DataSource;
import java.io.IOException;

public class ServidorUsuariosGrpc {

    public static void main(String[] args) throws IOException, InterruptedException {
        DataSource dataSource = BancoDeDados.conectar("usuarios_db");
        var servico = new UsuarioServiceImpl(dataSource, new UsuarioRepositorio());

        ServidorGrpc.executar("usuarios-service", Ambiente.inteiro("GRPC_PORT", 50053), servico);
    }
}
