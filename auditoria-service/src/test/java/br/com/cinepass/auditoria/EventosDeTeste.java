package br.com.cinepass.auditoria;

import br.com.cinepass.mensageria.CabecalhosKafka;
import br.com.cinepass.mensageria.EventoEnvelope;
import br.com.cinepass.mensageria.Topicos;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.utils.Utils;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.awaitility.Awaitility.await;

/**
 * Fabrica eventos como o reserva-service os publica (mesmo envelope, mesma chave, mesmos
 * cabeçalhos) e os envia ao Kafka real. O consumidor sob teste não sabe que não foi o
 * produtor verdadeiro quem publicou.
 */
public final class EventosDeTeste {

    public static final UUID MARIA = UUID.fromString("33333333-3333-3333-3333-333333333333");
    public static final UUID SEM_CONTATO = UUID.fromString("44444444-4444-4444-4444-444444444444");

    private EventosDeTeste() {
    }

    public static EventoEnvelope evento(JsonMapper json, String tipo, UUID reservaId, long sequencia, UUID clienteId,
                                        Map<String, Object> dados) {
        ObjectNode payload = json.createObjectNode();
        payload.put("clienteId", clienteId.toString());
        dados.forEach((chave, valor) -> payload.set(chave, json.valueToTree(valor)));
        return new EventoEnvelope(UUID.randomUUID(), tipo, 1, reservaId, sequencia, Instant.now(),
                "corr-teste-" + reservaId.toString().substring(0, 8), "reserva-service", payload);
    }

    public static EventoEnvelope confirmada(JsonMapper json, UUID reservaId, long sequencia, UUID clienteId, String valor) {
        return evento(json, "ReservaConfirmada", reservaId, sequencia, clienteId,
                Map.of("valorTotal", new BigDecimal(valor), "assentos", List.of("A1", "A2")));
    }

    public static void publicar(KafkaTemplate<String, String> kafka, JsonMapper json, EventoEnvelope evento) {
        var registro = new ProducerRecord<String, String>(Topicos.RESERVA_EVENTOS, evento.chave(), json.writeValueAsString(evento));
        registro.headers().add(CabecalhosKafka.EVENT_ID, bytes(evento.eventId().toString()));
        registro.headers().add(CabecalhosKafka.EVENT_TYPE, bytes(evento.eventType()));
        registro.headers().add(CabecalhosKafka.RESERVA_ID, bytes(evento.reservaId().toString()));
        registro.headers().add(CabecalhosKafka.CORRELATION_ID, bytes(evento.correlationId()));
        kafka.send(registro).join();
    }

    /** Uma reserva cuja chave cai na partição pedida, pelo mesmo cálculo do particionador do Kafka. */
    public static UUID reservaNaParticao(int particao) {
        while (true) {
            UUID candidata = UUID.randomUUID();
            if (Utils.toPositive(Utils.murmur2(bytes(candidata.toString()))) % Topicos.PARTICOES == particao) {
                return candidata;
            }
        }
    }

    /** Espera cada thread do consumidor receber sua partição, para o teste não medir um rebalanceamento. */
    public static void aguardarParticoesAtribuidas(KafkaListenerEndpointRegistry registro) {
        await().atMost(Duration.ofSeconds(30)).until(() -> registro.getListenerContainers().stream()
                .filter(c -> c instanceof ConcurrentMessageListenerContainer<?, ?>)
                .flatMap(c -> ((ConcurrentMessageListenerContainer<?, ?>) c).getContainers().stream())
                .filter(c -> c.getAssignedPartitions() != null && !c.getAssignedPartitions().isEmpty())
                .count() == Topicos.PARTICOES);
    }

    private static byte[] bytes(String texto) {
        return texto.getBytes(StandardCharsets.UTF_8);
    }
}
