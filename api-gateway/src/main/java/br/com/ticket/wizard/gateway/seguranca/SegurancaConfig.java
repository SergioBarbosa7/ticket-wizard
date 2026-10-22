package br.com.ticket.wizard.gateway.seguranca;

import br.com.ticket.wizard.gateway.config.CorsConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SegurancaConfig {

    @Bean
    FilterRegistrationBean<FiltroJwt> filtroJwt(JwtService jwt, ObjectMapper json) {
        FilterRegistrationBean<FiltroJwt> registro = new FilterRegistrationBean<>(new FiltroJwt(jwt, json));
        registro.addUrlPatterns("/api/*");
        registro.setOrder(CorsConfig.ORDEM + 1);
        return registro;
    }
}
