package br.com.cinepass.mensageria.correlacao;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.restclient.RestClientCustomizer;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * Propagação do correlationId no lado servlet: entrada (filtro) e saída (RestClient).
 * O Gateway, que é reativo, tem o filtro equivalente no próprio módulo.
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class CorrelacaoAutoConfiguration {

    @Bean
    FilterRegistrationBean<CorrelacaoFilter> correlacaoFilter() {
        var registro = new FilterRegistrationBean<>(new CorrelacaoFilter());
        // Antes de tudo, para o MDC já estar pronto quando o primeiro log da requisição sair.
        registro.setOrder(Ordered.HIGHEST_PRECEDENCE);
        registro.addUrlPatterns("/*");
        return registro;
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(RestClientCustomizer.class)
    static class SaidaHttp {

        /** Toda chamada HTTP de saída leva adiante o correlationId da requisição corrente. */
        @Bean
        RestClientCustomizer correlacaoRestClientCustomizer() {
            return builder -> builder.requestInterceptor((request, body, execution) -> {
                String correlationId = Correlacao.atual();
                if (correlationId != null) {
                    request.getHeaders().set(Correlacao.HEADER, correlationId);
                }
                return execution.execute(request, body);
            });
        }
    }
}
