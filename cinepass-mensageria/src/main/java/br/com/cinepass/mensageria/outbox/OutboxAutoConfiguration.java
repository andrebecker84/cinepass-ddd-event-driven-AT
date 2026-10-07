package br.com.cinepass.mensageria.outbox;

import br.com.cinepass.mensageria.observabilidade.ContextoDeRastreamento;
import br.com.cinepass.mensageria.observabilidade.ObservabilidadeAutoConfiguration;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;

/**
 * Liga o outbox no serviço que o pede ({@code cinepass.mensageria.outbox.habilitado=true}).
 * Só o serviço que produz eventos precisa da tabela; os consumidores não a recebem.
 *
 * <p>{@link AutoConfigurationPackage} registra este pacote para a varredura de entidades e
 * repositórios do JPA, e por isso esta configuração precisa rodar antes das do JPA.
 */
@AutoConfiguration(
        after = ObservabilidadeAutoConfiguration.class,
        beforeName = {
                "org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration",
                "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration"})
@ConditionalOnClass({KafkaTemplate.class, EntityManager.class})
@ConditionalOnProperty(prefix = "cinepass.mensageria.outbox", name = "habilitado", havingValue = "true")
@AutoConfigurationPackage(basePackageClasses = OutboxMensagem.class)
public class OutboxAutoConfiguration {

    @Bean
    PublicadorOutbox publicadorOutbox(OutboxRepository repositorio, JsonMapper jsonMapper,
                                      ContextoDeRastreamento rastreamento) {
        return new PublicadorOutbox(repositorio, jsonMapper, rastreamento);
    }

    @Bean
    RelayOutbox relayOutbox(OutboxRepository repositorio, KafkaTemplate<String, String> kafka,
                            PlatformTransactionManager transacoes, ContextoDeRastreamento rastreamento,
                            JsonMapper jsonMapper,
                            @Value("${cinepass.mensageria.outbox.intervalo:500ms}") Duration intervalo) {
        return new RelayOutbox(repositorio, kafka, new TransactionTemplate(transacoes), rastreamento, jsonMapper, intervalo);
    }

    @Bean
    AdministracaoOutbox administracaoOutbox(OutboxRepository repositorio) {
        return new AdministracaoOutbox(repositorio);
    }
}
