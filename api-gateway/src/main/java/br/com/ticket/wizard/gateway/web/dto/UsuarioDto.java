package br.com.ticket.wizard.gateway.web.dto;

public record UsuarioDto(String id, String nome, String email, PapelUsuario papel) {

    public static UsuarioDto de(br.com.ticket.wizard.grpc.usuarios.Usuario proto) {
        return new UsuarioDto(proto.getId(), proto.getNome(), proto.getEmail(),
                              PapelUsuario.valueOf(proto.getPapel().name()));
    }
}
