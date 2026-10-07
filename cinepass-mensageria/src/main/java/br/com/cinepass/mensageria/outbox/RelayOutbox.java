package br.com.cinepass.mensageria.outbox;

import br.com.cinepass.mensageria.CabecalhosKafka;
import br.com.cinepass.mensageria.observabilidade.ContextoDeRastreamento;
import br.com.cinepass.mensageria.observabilidade.MdcDoEvento;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Lê o outbox em ordem de gravação, publica no Kafka, espera a confirmação do broker e só
 * então marca a linha como publicada. A ordem desses três passos é o padrão inteiro:
 * marcar antes de confirmar reintroduziria a perda que o outbox existe para evitar.
 *
 * <p>A garantia resultante é <em>pelo menos uma vez</em>: se o processo cair entre a
 * confirmação do Kafka e a marcação, a mensagem sai de novo, com o mesmo {@code eventId}.
 * Quem absorve essa repetição é a inbox dos consumidores.
 *
 * <p>Ao primeiro erro a rodada para. Pular a linha e seguir publicaria o evento seguinte da
 * mesma reserva antes do que falhou.
 *
 * <p>Roda numa thread própria, e não com {@code @Scheduled}: o agendador do Spring é
 * observado, e cada rodada vazia viraria um trace no Zipkin a cada meio segundo.
 *
 * <p>Limite assumido: com mais de uma instância do serviço, duas rodadas leriam as mesmas
 * linhas. A resposta usual é {@code select ... for update skip locked} com partição das
 * linhas por chave, ou eleição de líder. Aqui roda uma instância, e a duplicidade que
 * sobrar já é absorvida pelos consumidores idempotentes.
 */
public class RelayOutbox implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(RelayOutbox.class);
    private static final TypeReference<Map<String, String>> MAPA = new TypeReference<>() {
    };

    private final OutboxRepository repositorio;
    private final KafkaTemplate<String, String> kafka;
    private final TransactionTemplate transacao;
    private final ContextoDeRastreamento rastreamento;
    private final JsonMapper jsonMapper;
    private final Duration intervalo;

    private ScheduledExecutorService executor;
    private volatile boolean rodando;

    public RelayOutbox(OutboxRepository repositorio, KafkaTemplate<String, String> kafka, TransactionTemplate transacao,
                       ContextoDeRastreamento rastreamento, JsonMapper jsonMapper, Duration intervalo) {
        this.repositorio = repositorio;
        this.kafka = kafka;
        this.transacao = transacao;
        this.rastreamento = rastreamento;
        this.jsonMapper = jsonMapper;
        this.intervalo = intervalo;
    }

    /** Uma rodada: publica os pendentes em ordem e devolve quantos saíram. */
    public int publicarPendentes() {
        List<OutboxMensagem> pendentes = repositorio.findTop100ByPublicadoEmIsNullOrderByIdAsc();
        int publicadas = 0;
        for (OutboxMensagem mensagem : pendentes) {
            if (!publicar(mensagem)) {
                break;
            }
            publicadas++;
        }
        return publicadas;
    }

    private boolean publicar(OutboxMensagem m) {
        try (var mdc = MdcDoEvento.de(m.getReservaId(), m.getEventId(), m.getEventType(), m.getCorrelationId());
             var span = rastreamento.continuar("outbox relay " + m.getEventType(), lerCabecalhos(m))) {
            span.tag("cinepass.event.id", m.getEventId().toString());
            span.tag("cinepass.event.type", m.getEventType());
            span.tag("cinepass.reserva.id", m.getReservaId().toString());
            span.tag("cinepass.correlation.id", m.getCorrelationId());
            try {
                var registro = new ProducerRecord<String, String>(m.getTopico(), m.getChave(), m.getPayload());
                cabecalho(registro, CabecalhosKafka.EVENT_ID, m.getEventId().toString());
                cabecalho(registro, CabecalhosKafka.EVENT_TYPE, m.getEventType());
                cabecalho(registro, CabecalhosKafka.RESERVA_ID, m.getReservaId().toString());
                cabecalho(registro, CabecalhosKafka.CORRELATION_ID, m.getCorrelationId());

                RecordMetadata confirmacao = kafka.send(registro).get(10, TimeUnit.SECONDS).getRecordMetadata();
                transacao.executeWithoutResult(s -> repositorio.marcarPublicada(m.getId(), Instant.now()));
                log.info("Evento publicado no Kafka: tipo={} topico={} particao={} offset={} chave={}",
                        m.getEventType(), confirmacao.topic(), confirmacao.partition(), confirmacao.offset(), m.getChave());
                return true;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            } catch (Exception e) {
                span.erro(e);
                String motivo = causaRaiz(e);
                log.error("Falha ao publicar evento do outbox, nova tentativa na próxima rodada: {}", motivo);
                transacao.executeWithoutResult(s -> repositorio.registrarFalha(m.getId(), motivo));
                return false;
            }
        }
    }

    private Map<String, String> lerCabecalhos(OutboxMensagem m) {
        if (m.getCabecalhosTracing() == null || m.getCabecalhosTracing().isBlank()) {
            return Map.of();
        }
        return jsonMapper.readValue(m.getCabecalhosTracing(), MAPA);
    }

    private static void cabecalho(ProducerRecord<String, String> registro, String nome, String valor) {
        registro.headers().add(nome, valor.getBytes(StandardCharsets.UTF_8));
    }

    private static String causaRaiz(Throwable e) {
        Throwable causa = e;
        while (causa.getCause() != null && causa.getCause() != causa) {
            causa = causa.getCause();
        }
        String texto = causa.getClass().getSimpleName() + ": " + causa.getMessage();
        return texto.length() > 500 ? texto.substring(0, 500) : texto;
    }

    private void rodadaSegura() {
        try {
            publicarPendentes();
        } catch (RuntimeException e) {
            log.warn("Rodada do relay do outbox falhou: {}", causaRaiz(e));
        }
    }

    @Override
    public void start() {
        executor = Executors.newSingleThreadScheduledExecutor(Thread.ofPlatform().name("outbox-relay").daemon().factory());
        executor.scheduleWithFixedDelay(this::rodadaSegura, intervalo.toMillis(), intervalo.toMillis(), TimeUnit.MILLISECONDS);
        rodando = true;
        log.info("Relay do outbox iniciado, intervalo de {} ms", intervalo.toMillis());
    }

    @Override
    public void stop() {
        rodando = false;
        if (executor != null) {
            executor.shutdown();
        }
    }

    @Override
    public boolean isRunning() {
        return rodando;
    }
}
