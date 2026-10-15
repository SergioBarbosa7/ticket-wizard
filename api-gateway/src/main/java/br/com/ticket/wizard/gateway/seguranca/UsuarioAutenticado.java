package br.com.ticket.wizard.gateway.seguranca;

import br.com.ticket.wizard.gateway.web.dto.PapelUsuario;

import java.util.UUID;

public record UsuarioAutenticado(UUID id, PapelUsuario papel) {
}
