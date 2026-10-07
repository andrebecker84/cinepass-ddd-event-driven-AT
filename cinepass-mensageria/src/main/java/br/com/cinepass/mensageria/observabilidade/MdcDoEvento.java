package br.com.cinepass.mensageria.observabilidade;

import br.com.cinepass.mensageria.EventoEnvelope;
import br.com.cinepass.mensageria.correlacao.Correlacao;
import org.slf4j.MDC;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Coloca no MDC as chaves de contexto de um evento e as remove ao fechar, devolvendo o que
 * havia antes. Toda linha de log escrita dentro do bloco sai com reservaId, eventId,
 * eventType e correlationId, no console e no Elasticsearch.
 */
public final class MdcDoEvento implements AutoCloseable {

    private final Map<String, String> anteriores = new HashMap<>();

    private MdcDoEvento() {
    }

    public static MdcDoEvento de(EventoEnvelope envelope) {
        return de(envelope.reservaId(), envelope.eventId(), envelope.eventType(), envelope.correlationId());
    }

    public static MdcDoEvento de(UUID reservaId, UUID eventId, String eventType, String correlationId) {
        var mdc = new MdcDoEvento();
        mdc.colocar(Correlacao.MDC_RESERVA_ID, reservaId == null ? null : reservaId.toString());
        mdc.colocar(Correlacao.MDC_EVENT_ID, eventId == null ? null : eventId.toString());
        mdc.colocar(Correlacao.MDC_EVENT_TYPE, eventType);
        mdc.colocar(Correlacao.MDC_CORRELATION_ID, correlationId);
        return mdc;
    }

    private void colocar(String chave, String valor) {
        if (valor == null) return;
        anteriores.put(chave, MDC.get(chave));
        MDC.put(chave, valor);
    }

    @Override
    public void close() {
        anteriores.forEach((chave, valor) -> {
            if (valor == null) MDC.remove(chave);
            else MDC.put(chave, valor);
        });
    }
}
