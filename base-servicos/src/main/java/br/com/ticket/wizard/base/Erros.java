package br.com.ticket.wizard.base;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;

public final class Erros {

    private Erros() {
    }

    public static StatusRuntimeException argumentoInvalido(String mensagem) {
        return Status.INVALID_ARGUMENT.withDescription(mensagem).asRuntimeException();
    }

    public static StatusRuntimeException naoEncontrado(String mensagem) {
        return Status.NOT_FOUND.withDescription(mensagem).asRuntimeException();
    }

    public static StatusRuntimeException jaExiste(String mensagem) {
        return Status.ALREADY_EXISTS.withDescription(mensagem).asRuntimeException();
    }

    public static StatusRuntimeException precondicaoFalhou(String mensagem) {
        return Status.FAILED_PRECONDITION.withDescription(mensagem).asRuntimeException();
    }

    public static StatusRuntimeException naoAutenticado(String mensagem) {
        return Status.UNAUTHENTICATED.withDescription(mensagem).asRuntimeException();
    }
}
