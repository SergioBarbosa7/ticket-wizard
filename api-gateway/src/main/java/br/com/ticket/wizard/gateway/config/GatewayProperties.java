package br.com.ticket.wizard.gateway.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Validated
@ConfigurationProperties("gateway")
public record GatewayProperties(@Valid @NotNull Jwt jwt,
                                @Valid @NotNull Servicos servicos,
                                @NotNull Cors cors) {

    public record Jwt(@NotBlank String secret, @Positive long expiracaoSegundos) {
    }

    public record Servicos(@NotBlank String usuarios, @NotBlank String ingressos, @NotBlank String pedidos) {
    }

    public record Cors(List<String> origens) {
    }
}
