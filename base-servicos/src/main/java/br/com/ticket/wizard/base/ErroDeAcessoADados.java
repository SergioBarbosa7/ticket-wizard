package br.com.ticket.wizard.base;

public class ErroDeAcessoADados extends RuntimeException {

    public ErroDeAcessoADados(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
