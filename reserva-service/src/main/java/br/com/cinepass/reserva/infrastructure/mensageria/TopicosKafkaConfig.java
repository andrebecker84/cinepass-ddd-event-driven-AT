package br.com.cinepass.reserva.infrastructure.mensageria;

import br.com.cinepass.mensageria.Topicos;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * O produtor é dono do tópico e o declara na subida: o KafkaAdmin cria se não existir.
 *
 * <p>Três partições é o teto de paralelismo de cada grupo consumidor: até três reservas
 * diferentes são processadas ao mesmo tempo, e os eventos de uma mesma reserva, que têm a
 * mesma chave, caem sempre na mesma partição e são lidos em ordem.
 */
@Configuration(proxyBeanMethods = false)
public class TopicosKafkaConfig {

    @Bean
    NewTopic topicoReservaEventos() {
        return TopicBuilder.name(Topicos.RESERVA_EVENTOS)
                .partitions(Topicos.PARTICOES)
                .replicas(1)
                .config(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(Topicos.RETENCAO_MS))
                .build();
    }
}
