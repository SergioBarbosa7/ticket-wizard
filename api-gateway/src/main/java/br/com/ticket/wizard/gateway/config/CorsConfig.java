package br.com.ticket.wizard.gateway.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

// Roda antes do filtro JWT, para que até a resposta 401 leve os cabeçalhos CORS.
@Configuration
public class CorsConfig {

    public static final int ORDEM = 0;

    @Bean
    FilterRegistrationBean<CorsFilter> corsFilter(GatewayProperties propriedades) {
        CorsConfiguration config = new CorsConfiguration();
        List<String> origens = propriedades.cors().origens() == null ? List.of() : propriedades.cors().origens();
        config.setAllowedOriginPatterns(origens);
        config.setAllowedMethods(List.of("GET", "POST", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource fonte = new UrlBasedCorsConfigurationSource();
        fonte.registerCorsConfiguration("/api/**", config);

        FilterRegistrationBean<CorsFilter> registro = new FilterRegistrationBean<>(new CorsFilter(fonte));
        registro.setOrder(ORDEM);
        return registro;
    }
}
