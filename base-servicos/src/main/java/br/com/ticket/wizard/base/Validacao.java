package br.com.ticket.wizard.base;

import java.util.UUID;

public final class Validacao {

    public static final int MAX_INGRESSOS_POR_PEDIDO = 10;

    private Validacao() {
    }

    public static String texto(String campo, String valor, int tamanhoMaximo) {
        if (valor == null || valor.isBlank()) {
            throw Erros.argumentoInvalido("O campo '" + campo + "' é obrigatório.");
        }
        String aparado = valor.trim();
        if (aparado.length() > tamanhoMaximo) {
            throw Erros.argumentoInvalido("O campo '" + campo + "' aceita no máximo " + tamanhoMaximo + " caracteres.");
        }
        return aparado;
    }

    public static UUID uuid(String campo, String valor) {
        if (valor == null || valor.isBlank()) {
            throw Erros.argumentoInvalido("O campo '" + campo + "' é obrigatório.");
        }
        try {
            return UUID.fromString(valor.trim());
        } catch (IllegalArgumentException erro) {
            throw Erros.argumentoInvalido("O campo '" + campo + "' não é um identificador válido.");
        }
    }

    public static int quantidadeDoPedido(int quantidade) {
        if (quantidade < 1 || quantidade > MAX_INGRESSOS_POR_PEDIDO) {
            throw Erros.argumentoInvalido(
                    "A quantidade deve estar entre 1 e " + MAX_INGRESSOS_POR_PEDIDO + " ingressos por pedido.");
        }
        return quantidade;
    }
}
