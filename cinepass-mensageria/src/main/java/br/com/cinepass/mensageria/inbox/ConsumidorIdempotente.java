package br.com.cinepass.mensageria.inbox;

import br.com.cinepass.mensageria.EventoEnvelope;
import br.com.cinepass.mensageria.observabilidade.MdcDoEvento;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

/**
 * Consumo idempotente (Idempotent Consumer): o registro do evento na inbox e o efeito de
 * negócio acontecem na mesma transação local.
 *
 * <ul>
 *   <li>Evento novo: grava a inbox, aplica o efeito, commita os dois.</li>
 *   <li>Evento já visto: não aplica nada e registra no log que a duplicata foi ignorada.</li>
 *   <li>Efeito falhou: rollback dos dois, e a mensagem volta para nova tentativa.</li>
 * </ul>
 *
 * <p>Sem a transação compartilhada haveria duas janelas ruins: efeito aplicado sem registro
 * (repetido na reentrega) ou registro gravado sem efeito (perdido para sempre).
 */
public class ConsumidorIdempotente {

    private static final Logger log = LoggerFactory.getLogger(ConsumidorIdempotente.class);

    public enum Resultado { PROCESSADO, DUPLICADO }

    private final EventoProcessadoRepository inbox;
    private final TransactionTemplate transacao;
    private final JsonMapper jsonMapper;
    private final String consumidor;

    public ConsumidorIdempotente(EventoProcessadoRepository inbox, TransactionTemplate transacao,
                                 JsonMapper jsonMapper, String consumidor) {
        this.inbox = inbox;
        this.transacao = transacao;
        this.jsonMapper = jsonMapper;
        this.consumidor = consumidor;
    }

    public Resultado processar(ConsumerRecord<String, String> registro, ManipuladorDeEvento manipulador) {
        EventoEnvelope evento = ler(registro);
        try (var mdc = MdcDoEvento.de(evento)) {
            long inicio = System.nanoTime();
            log.info("Evento recebido: tipo={} sequence={} topico={} particao={} offset={}",
                    evento.eventType(), evento.sequence(), registro.topic(), registro.partition(), registro.offset());
            try {
                Resultado resultado = transacao.execute(status -> {
                    if (inbox.existsById(evento.eventId())) {
                        return Resultado.DUPLICADO;
                    }
                    inbox.save(new EventoProcessado(evento.eventId(), evento.eventType(), evento.reservaId(),
                            evento.correlationId(), consumidor, Instant.now()));
                    manipulador.tratar(evento);
                    return Resultado.PROCESSADO;
                });
                long ms = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - inicio);
                if (resultado == Resultado.DUPLICADO) {
                    log.warn("Evento duplicado ignorado: já processado anteriormente por {}, nenhum efeito reaplicado", consumidor);
                } else {
                    log.info("Processamento concluído: tipo={} resultado=PROCESSADO duracaoMs={}", evento.eventType(), ms);
                }
                return resultado;
            } catch (RuntimeException e) {
                log.error("Falha no processamento: tipo={} motivo={}", evento.eventType(), e.getMessage());
                throw e;
            }
        }
    }

    public EventoEnvelope ler(ConsumerRecord<String, String> registro) {
        try {
            return jsonMapper.readValue(registro.value(), EventoEnvelope.class);
        } catch (JacksonException | IllegalArgumentException | NullPointerException e) {
            log.error("Mensagem ilegível descartada para a DLT: topico={} particao={} offset={} motivo={}",
                    registro.topic(), registro.partition(), registro.offset(), e.getMessage());
            throw new EnvelopeInvalidoException("Mensagem não é um envelope de evento válido", e);
        }
    }

    public String consumidor() {
        return consumidor;
    }
}
