package br.com.cinepass.mensageria.inbox;

import br.com.cinepass.mensageria.Topicos;
import br.com.cinepass.mensageria.observabilidade.ContextoDeRastreamento;
import br.com.cinepass.mensageria.observabilidade.ObservabilidadeAutoConfiguration;
import jakarta.persistence.EntityManager;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.config.TopicConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

/**
 * Liga o lado consumidor no serviço que se declara consumidor
 * ({@code cinepass.mensageria.consumidor.nome}): inbox, tratamento de falhas com DLT e
 * reprocessamento.
 *
 * <p><b>Tratamento de falhas.</b> Uma mensagem que falha é tentada de novo no lugar, com
 * espera crescente (1 s, 2 s). Esgotadas as tentativas, vai para a DLT do consumidor,
 * <em>na mesma partição</em> de origem, e o consumo da partição segue. A falha fica isolada
 * na mensagem que falhou: as reservas seguintes não ficam presas atrás dela.
 *
 * <p>A retentativa é bloqueante de propósito. A alternativa não bloqueante (tópicos de
 * retry) libera a partição, mas deixa o evento seguinte da mesma reserva passar à frente
 * do que está esperando, e a ordem por reserva é requisito.
 */
@AutoConfiguration(
        after = ObservabilidadeAutoConfiguration.class,
        beforeName = {
                "org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration",
                "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration"})
@ConditionalOnClass({KafkaTemplate.class, EntityManager.class})
@ConditionalOnProperty(prefix = "cinepass.mensageria.consumidor", name = "nome")
@AutoConfigurationPackage(basePackageClasses = EventoProcessado.class)
public class InboxAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(InboxAutoConfiguration.class);

    @Bean
    ConsumidorIdempotente consumidorIdempotente(EventoProcessadoRepository inbox, PlatformTransactionManager transacoes,
                                                JsonMapper jsonMapper,
                                                @Value("${cinepass.mensageria.consumidor.nome}") String consumidor) {
        return new ConsumidorIdempotente(inbox, new TransactionTemplate(transacoes), jsonMapper, consumidor);
    }

    @Bean
    CommonErrorHandler tratamentoDeFalhas(KafkaTemplate<String, String> kafka,
                                          @Value("${cinepass.mensageria.consumidor.nome}") String consumidor,
                                          @Value("${cinepass.mensageria.consumidor.tentativas:3}") int tentativas,
                                          @Value("${cinepass.mensageria.consumidor.espera-inicial-ms:1000}") long esperaInicial) {
        var dlt = new DeadLetterPublishingRecoverer(kafka,
                (registro, erro) -> new TopicPartition(Topicos.dltDe(registro.topic(), consumidor), registro.partition()));

        var espera = new ExponentialBackOffWithMaxRetries(tentativas - 1);
        espera.setInitialInterval(esperaInicial);
        espera.setMultiplier(2.0);

        var tratamento = new DefaultErrorHandler((registro, erro) -> {
            try (var mdc = CabecalhosDoRegistro.mdc(registro)) {
                log.error("Tentativas esgotadas, mensagem enviada para a DLT: destino={} particao={} offsetOriginal={} motivo={}",
                        Topicos.dltDe(registro.topic(), consumidor), registro.partition(), registro.offset(),
                        causaRaiz(erro));
            }
            dlt.accept(registro, erro);
        }, espera);
        tratamento.addNotRetryableExceptions(EnvelopeInvalidoException.class);
        tratamento.setRetryListeners((registro, erro, tentativa) -> {
            try (var mdc = CabecalhosDoRegistro.mdc(registro)) {
                log.warn("Tentativa {} de {} falhou: particao={} offset={} motivo={}",
                        tentativa, tentativas, registro.partition(), registro.offset(), causaRaiz(erro));
            }
        });
        return tratamento;
    }

    /**
     * Declara o tópico de origem com a mesma configuração do produtor (quem subir primeiro o
     * cria; se já existe, nada muda) e a DLT deste consumidor.
     */
    @Bean
    NewTopic topicoReservaEventos() {
        return TopicBuilder.name(Topicos.RESERVA_EVENTOS).partitions(Topicos.PARTICOES).replicas(1)
                .config(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(Topicos.RETENCAO_MS)).build();
    }

    @Bean
    NewTopic topicoDlt(@Value("${cinepass.mensageria.consumidor.nome}") String consumidor) {
        // Mesmo número de partições da origem: a DLT recebe cada mensagem na partição em que ela estava.
        return TopicBuilder.name(Topicos.dltDe(Topicos.RESERVA_EVENTOS, consumidor))
                .partitions(Topicos.PARTICOES).replicas(1).build();
    }

    @Bean
    ReprocessadorDeDlt reprocessadorDeDlt(ConsumerFactory<String, String> fabrica, ConsumidorIdempotente consumidor,
                                          ContextoDeRastreamento rastreamento) {
        return new ReprocessadorDeDlt(fabrica, consumidor, rastreamento,
                Topicos.dltDe(Topicos.RESERVA_EVENTOS, consumidor.consumidor()));
    }

    private static String causaRaiz(Throwable erro) {
        Throwable causa = erro;
        while (causa.getCause() != null && causa.getCause() != causa) {
            causa = causa.getCause();
        }
        return causa.getClass().getSimpleName() + ": " + causa.getMessage();
    }
}
