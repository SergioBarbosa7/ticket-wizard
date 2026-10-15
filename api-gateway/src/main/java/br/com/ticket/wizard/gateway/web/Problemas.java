package br.com.ticket.wizard.gateway.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

public final class Problemas {

    private Problemas() {
    }

    public static ProblemDetail criar(HttpStatus status, String detalhe) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, detalhe);
        problema.setTitle(status.getReasonPhrase());
        return problema;
    }
}
