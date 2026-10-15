package br.com.ticket.wizard.usuarios;

import java.util.UUID;

public record Usuario(UUID id, String nome, String email, String senhaHash, String papel) {
}
