package br.com.cinepass.reserva;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Lê o tópico como um consumidor de verdade, num grupo descartável, e devolve só os
 * registros de uma chave. O filtro por chave isola cada teste dos eventos dos outros,
 * já que os containers são compartilhados.
 */
public final class LeitorDoTopico {

    private LeitorDoTopico() {
    }

    public static List<ConsumerRecord<String, String>> registrosDaChave(String topico, String chave, int esperados,
                                                                        Duration prazo) {
        Map<String, Object> config = Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, InfraDeTeste.KAFKA.getBootstrapServers(),
                ConsumerConfig.GROUP_ID_CONFIG, "teste-" + UUID.randomUUID(),
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        List<ConsumerRecord<String, String>> encontrados = new ArrayList<>();
        Instant limite = Instant.now().plus(prazo);
        try (var consumidor = new KafkaConsumer<>(config, new StringDeserializer(), new StringDeserializer())) {
            consumidor.subscribe(List.of(topico));
            while (encontrados.size() < esperados && Instant.now().isBefore(limite)) {
                for (var registro : consumidor.poll(Duration.ofMillis(500))) {
                    if (chave.equals(registro.key())) {
                        encontrados.add(registro);
                    }
                }
            }
        }
        return encontrados;
    }
}
