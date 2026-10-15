package br.com.ticket.wizard.ingressos;

import java.util.UUID;

public record Categoria(UUID id,
                        String eventoId,
                        String nome,
                        long precoCentavos,
                        int quantidadeTotal,
                        int quantidadeDisponivel) {
}
