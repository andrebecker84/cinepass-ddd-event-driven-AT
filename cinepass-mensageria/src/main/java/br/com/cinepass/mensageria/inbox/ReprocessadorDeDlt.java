package br.com.cinepass.mensageria.inbox;

import br.com.cinepass.mensageria.observabilidade.ContextoDeRastreamento;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.ConsumerFactory;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Reprocessa a DLT deste consumidor: lê as mensagens que esgotaram as tentativas e as
 * entrega de novo à mesma regra de negócio, pelo mesmo caminho idempotente.
 *
 * <p>As mensagens ficam na DLT até alguém decidir reprocessá-las, depois que a causa foi
 * corrigida. O progresso é guardado num grupo de consumo próprio: o que foi reprocessado
 * não volta, e o que falhou de novo continua na fila, na mesma ordem.
 *
 * <p>Ao primeiro erro o reprocessamento para. Seguir adiante processaria o evento seguinte
 * da mesma reserva antes do que falhou.
 */
public class ReprocessadorDeDlt {

    private static final Logger log = LoggerFactory.getLogger(ReprocessadorDeDlt.class);

    public record Resultado(String topicoDlt, int lidas, int reprocessadas, int duplicadas, String falha) {
    }

    private final ConsumerFactory<String, String> fabrica;
    private final ConsumidorIdempotente consumidor;
    private final ContextoDeRastreamento rastreamento;
    private final String topicoDlt;

    public ReprocessadorDeDlt(ConsumerFactory<String, String> fabrica, ConsumidorIdempotente consumidor,
                              ContextoDeRastreamento rastreamento, String topicoDlt) {
        this.fabrica = fabrica;
        this.consumidor = consumidor;
        this.rastreamento = rastreamento;
        this.topicoDlt = topicoDlt;
    }

    public synchronized Resultado reprocessar(ManipuladorDeEvento manipulador) {
        Properties ajustes = new Properties();
        ajustes.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        ajustes.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        String grupo = consumidor.consumidor() + "-dlt-reprocessador";

        int lidas = 0, reprocessadas = 0, duplicadas = 0;
        String falha = null;
        log.info("Reprocessamento da DLT iniciado: topico={}", topicoDlt);
        try (Consumer<String, String> leitor = fabrica.createConsumer(grupo, null, "-dlt", ajustes)) {
            leitor.subscribe(List.of(topicoDlt));
            int pollsVazios = 0;
            laco:
            while (pollsVazios < 3) {
                ConsumerRecords<String, String> registros = leitor.poll(Duration.ofSeconds(1));
                if (registros.isEmpty()) {
                    pollsVazios++;
                    continue;
                }
                pollsVazios = 0;
                for (ConsumerRecord<String, String> registro : registros) {
                    lidas++;
                    try (var span = rastreamento.continuar("dlt reprocessamento", CabecalhosDoRegistro.rastreamento(registro))) {
                        span.tag("cinepass.dlt", topicoDlt);
                        try {
                            var resultado = consumidor.processar(registro, manipulador);
                            if (resultado == ConsumidorIdempotente.Resultado.DUPLICADO) duplicadas++;
                            else reprocessadas++;
                            leitor.commitSync(Map.of(new TopicPartition(registro.topic(), registro.partition()),
                                    new OffsetAndMetadata(registro.offset() + 1)));
                        } catch (RuntimeException e) {
                            span.erro(e);
                            falha = e.getMessage();
                            break laco;
                        }
                    }
                }
            }
        }
        var resultado = new Resultado(topicoDlt, lidas, reprocessadas, duplicadas, falha);
        if (falha == null) {
            log.info("Reprocessamento da DLT concluído: {}", resultado);
        } else {
            log.error("Reprocessamento da DLT interrompido na primeira falha: {}", resultado);
        }
        return resultado;
    }
}
