package br.com.ticket.wizard.base;

public final class Ambiente {

    private Ambiente() {
    }

    public static String obrigatoria(String nome) {
        String valor = System.getenv(nome);
        if (valor == null || valor.isBlank()) {
            throw new IllegalStateException("Variável de ambiente obrigatória não definida: " + nome);
        }
        return valor;
    }

    public static String ou(String nome, String padrao) {
        String valor = System.getenv(nome);
        return valor == null || valor.isBlank() ? padrao : valor;
    }

    public static int inteiro(String nome, int padrao) {
        String valor = System.getenv(nome);
        if (valor == null || valor.isBlank()) {
            return padrao;
        }
        try {
            return Integer.parseInt(valor.trim());
        } catch (NumberFormatException erro) {
            throw new IllegalStateException("Variável " + nome + " deve ser um número inteiro: " + valor, erro);
        }
    }
}
