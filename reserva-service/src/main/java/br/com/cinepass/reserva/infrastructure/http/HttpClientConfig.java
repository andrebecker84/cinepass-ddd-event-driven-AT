package br.com.cinepass.reserva.infrastructure.http;

import org.springframework.boot.restclient.autoconfigure.RestClientBuilderConfigurer;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * Os dois builders passam pelo {@link RestClientBuilderConfigurer} do Spring Boot. Na base
 * eles eram criados com {@code RestClient.builder()} puro, e por isso as chamadas ao
 * pagamento e ao ingresso não apareciam no trace: a observação do Micrometer e o repasse do
 * correlationId são customizações que o Boot aplica no builder que ele configura.
 */
@Configuration
public class HttpClientConfig {
    @Bean
    @Primary
    RestClient.Builder restClientBuilder(RestClientBuilderConfigurer configurer) {
        return configurer.configure(RestClient.builder());
    }

    @Bean
    @LoadBalanced
    RestClient.Builder loadBalancedRestClientBuilder(RestClientBuilderConfigurer configurer) {
        var fabrica = new SimpleClientHttpRequestFactory();
        // Sem prazo, um participante travado prenderia a Saga e a thread da requisição indefinidamente.
        fabrica.setConnectTimeout(Duration.ofSeconds(2));
        fabrica.setReadTimeout(Duration.ofSeconds(5));
        return configurer.configure(RestClient.builder()).requestFactory(fabrica);
    }
}
