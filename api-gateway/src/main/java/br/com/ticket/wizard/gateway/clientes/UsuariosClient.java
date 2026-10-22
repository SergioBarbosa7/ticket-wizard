package br.com.ticket.wizard.gateway.clientes;

import br.com.ticket.wizard.gateway.web.dto.LoginRequest;
import br.com.ticket.wizard.gateway.web.dto.RegistroRequest;
import br.com.ticket.wizard.gateway.web.dto.UsuarioDto;
import br.com.ticket.wizard.grpc.usuarios.AutenticarRequest;
import br.com.ticket.wizard.grpc.usuarios.CadastrarUsuarioRequest;
import br.com.ticket.wizard.grpc.usuarios.Papel;
import br.com.ticket.wizard.grpc.usuarios.UsuarioServiceGrpc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class UsuariosClient {

    private static final Logger LOG = LoggerFactory.getLogger(UsuariosClient.class);

    private final UsuarioServiceGrpc.UsuarioServiceBlockingStub stub;

    public UsuariosClient(UsuarioServiceGrpc.UsuarioServiceBlockingStub stub) {
        this.stub = stub;
    }

    public UsuarioDto cadastrar(RegistroRequest dados) {
        LOG.info("gRPC -> usuarios-service.Cadastrar");
        return UsuarioDto.de(stub.cadastrar(CadastrarUsuarioRequest.newBuilder()
                                                                   .setNome(dados.nome())
                                                                   .setEmail(dados.email())
                                                                   .setSenha(dados.senha())
                                                                   .setPapel(Papel.valueOf(dados.papel().name()))
                                                                   .build()));
    }

    public UsuarioDto autenticar(LoginRequest dados) {
        LOG.info("gRPC -> usuarios-service.Autenticar");
        return UsuarioDto.de(stub.autenticar(AutenticarRequest.newBuilder()
                                                              .setEmail(dados.email())
                                                              .setSenha(dados.senha())
                                                              .build()));
    }
}
