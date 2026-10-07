package br.com.cinepass.auditoria;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * PostgreSQL e Kafka reais, os mesmos do docker-compose, iniciados uma vez por JVM.
 * Ficam em campos estáticos para que as classes de teste, cada uma com seu contexto
 * Spring, não subam containers novos.
 */
@TestConfiguration(proxyBeanMethods = false)
public class InfraDeTeste {

    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine");
    static final KafkaContainer KAFKA = new KafkaContainer("apache/kafka:4.3.1");

    static {
        POSTGRES.start();
        KAFKA.start();
    }

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgres() {
        return POSTGRES;
    }

    @Bean
    @ServiceConnection
    KafkaContainer kafka() {
        return KAFKA;
    }
}
