package br.com.cinepass.mensageria.observabilidade;

import io.micrometer.observation.ObservationPredicate;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.observation.ClientRequestObservationContext;

/**
 * Configuração comum de rastreamento.
 *
 * <p>Os filtros abaixo retiram do Zipkin o que é ruído de infraestrutura: heartbeats do
 * Eureka (um trace a cada 30 segundos por serviço), health checks e tarefas agendadas.
 * Sem eles, o trace de uma reserva fica soterrado entre centenas de traces vazios.
 */
@AutoConfiguration
@ConditionalOnClass(Tracer.class)
public class ObservabilidadeAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    ContextoDeRastreamento contextoDeRastreamento(ObjectProvider<Tracer> tracer, ObjectProvider<Propagator> propagator) {
        return new ContextoDeRastreamento(tracer.getIfAvailable(), propagator.getIfAvailable());
    }

    @Bean
    ObservationPredicate semTarefasAgendadas() {
        return (nome, contexto) -> !"tasks.scheduled.execution".equals(nome);
    }

    @Bean
    ObservationPredicate semChamadasAoEureka() {
        return (nome, contexto) -> {
            if (contexto instanceof ClientRequestObservationContext cliente && cliente.getCarrier() != null) {
                return !ehInfraestrutura(cliente.getCarrier().getURI().getPath());
            }
            return true;
        };
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    static class Servlet {

        @Bean
        ObservationPredicate semActuatorServlet() {
            return (nome, contexto) -> {
                if (contexto instanceof org.springframework.http.server.observation.ServerRequestObservationContext servidor
                        && servidor.getCarrier() != null) {
                    return !ehInfraestrutura(servidor.getCarrier().getRequestURI());
                }
                return true;
            };
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
    static class Reativo {

        @Bean
        ObservationPredicate semActuatorReativo() {
            return (nome, contexto) -> {
                if (contexto instanceof org.springframework.http.server.reactive.observation.ServerRequestObservationContext servidor
                        && servidor.getCarrier() != null) {
                    return !ehInfraestrutura(servidor.getCarrier().getPath().value());
                }
                if (contexto instanceof org.springframework.web.reactive.function.client.ClientRequestObservationContext cliente
                        && cliente.getRequest() != null) {
                    return !ehInfraestrutura(cliente.getRequest().url().getPath());
                }
                return true;
            };
        }
    }

    static boolean ehInfraestrutura(String caminho) {
        return caminho != null && (caminho.startsWith("/actuator") || caminho.contains("/eureka"));
    }
}
