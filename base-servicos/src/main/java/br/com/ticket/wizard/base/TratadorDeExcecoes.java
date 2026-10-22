package br.com.ticket.wizard.base;

import io.grpc.ForwardingServerCallListener.SimpleForwardingServerCallListener;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// O gRPC não converte uma StatusRuntimeException lançada no handler em resposta ao
// cliente (devolve UNKNOWN). Este interceptor faz a conversão: os serviços lançam
// Erros.xxx() e o status chega intacto; qualquer outra exceção vira INTERNAL, com o
// detalhe só no log do servidor.
public class TratadorDeExcecoes implements ServerInterceptor {

    private static final Logger LOG = LoggerFactory.getLogger(TratadorDeExcecoes.class);

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> chamada,
                                                                 Metadata cabecalhos,
                                                                 ServerCallHandler<ReqT, RespT> proximo) {
        ServerCall.Listener<ReqT> ouvinte = proximo.startCall(chamada, cabecalhos);

        return new SimpleForwardingServerCallListener<>(ouvinte) {
            @Override
            public void onHalfClose() {
                try {
                    super.onHalfClose();
                } catch (StatusRuntimeException erro) {
                    chamada.close(erro.getStatus(), new Metadata());
                } catch (RuntimeException erro) {
                    LOG.error("Erro inesperado em {}", chamada.getMethodDescriptor().getFullMethodName(), erro);
                    chamada.close(Status.INTERNAL.withDescription("Erro interno do serviço."), new Metadata());
                }
            }
        };
    }
}
