package br.com.ticket.wizard.gateway.config;

import br.com.ticket.wizard.grpc.ingressos.IngressoServiceGrpc;
import br.com.ticket.wizard.grpc.pedidos.PedidoServiceGrpc;
import br.com.ticket.wizard.grpc.usuarios.UsuarioServiceGrpc;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.MethodDescriptor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class GrpcConfig {

    private static final long PRAZO_SEGUNDOS = 15;

    private static ManagedChannel canal(String alvo) {
        return ManagedChannelBuilder.forTarget(alvo)
                                    .usePlaintext()
                                    .intercept(new PrazoPorChamada())
                                    .build();
    }

    @Bean(destroyMethod = "shutdown")
    ManagedChannel canalUsuarios(GatewayProperties propriedades) {
        return canal(propriedades.servicos().usuarios());
    }

    @Bean(destroyMethod = "shutdown")
    ManagedChannel canalIngressos(GatewayProperties propriedades) {
        return canal(propriedades.servicos().ingressos());
    }

    @Bean(destroyMethod = "shutdown")
    ManagedChannel canalPedidos(GatewayProperties propriedades) {
        return canal(propriedades.servicos().pedidos());
    }

    @Bean
    UsuarioServiceGrpc.UsuarioServiceBlockingStub usuarioStub(@Qualifier("canalUsuarios") ManagedChannel canal) {
        return UsuarioServiceGrpc.newBlockingStub(canal);
    }

    @Bean
    IngressoServiceGrpc.IngressoServiceBlockingStub ingressoStub(@Qualifier("canalIngressos") ManagedChannel canal) {
        return IngressoServiceGrpc.newBlockingStub(canal);
    }

    @Bean
    PedidoServiceGrpc.PedidoServiceBlockingStub pedidoStub(@Qualifier("canalPedidos") ManagedChannel canal) {
        return PedidoServiceGrpc.newBlockingStub(canal);
    }

    // O prazo precisa ser calculado a cada chamada: um deadline fixado no stub contaria a
    // partir da criação do bean e expiraria depois de alguns segundos.
    private static final class PrazoPorChamada implements ClientInterceptor {
        @Override
        public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> metodo,
                                                                   CallOptions opcoes,
                                                                   Channel proximo) {
            return proximo.newCall(metodo, opcoes.getDeadline() == null
                                           ? opcoes.withDeadlineAfter(PRAZO_SEGUNDOS, TimeUnit.SECONDS)
                                           : opcoes);
        }
    }
}
