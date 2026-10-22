package br.com.ticket.wizard.gateway.web.dto;

public record TokenResponse(String token, String tipo, long expiraEmSegundos, UsuarioDto usuario) {

    public static TokenResponse bearer(String token, long expiraEmSegundos, UsuarioDto usuario) {
        return new TokenResponse(token, "Bearer", expiraEmSegundos, usuario);
    }
}
