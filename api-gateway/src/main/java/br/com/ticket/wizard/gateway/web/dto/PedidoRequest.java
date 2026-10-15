package br.com.ticket.wizard.gateway.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record PedidoRequest(
        @NotNull(message = "A categoria é obrigatória.")
        UUID categoriaId,

        @NotNull(message = "A quantidade é obrigatória.")
        @Min(value = 1, message = "A quantidade mínima é 1 ingresso.")
        @Max(value = 10, message = "A quantidade máxima é 10 ingressos por pedido.")
        Integer quantidade) {
}
