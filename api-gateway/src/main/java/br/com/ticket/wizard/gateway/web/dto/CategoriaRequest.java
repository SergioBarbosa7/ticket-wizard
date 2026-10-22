package br.com.ticket.wizard.gateway.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CategoriaRequest(
        @NotBlank(message = "O evento é obrigatório.")
        @Size(max = 100, message = "O evento aceita no máximo 100 caracteres.")
        String eventoId,

        @NotBlank(message = "O nome da categoria é obrigatório.")
        @Size(max = 100, message = "O nome aceita no máximo 100 caracteres.")
        String nome,

        @NotNull(message = "O preço (em centavos) é obrigatório.")
        @Min(value = 1, message = "O preço deve ser maior que zero.")
        Long precoCentavos,

        @NotNull(message = "A quantidade é obrigatória.")
        @Min(value = 1, message = "A quantidade deve ser maior que zero.")
        @Max(value = 1_000_000, message = "A quantidade máxima é 1.000.000.")
        Integer quantidade) {
}
