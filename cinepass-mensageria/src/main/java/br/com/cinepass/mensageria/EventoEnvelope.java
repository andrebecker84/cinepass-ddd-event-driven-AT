package br.com.cinepass.mensageria;

import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Envelope de todo evento de integração publicado no Kafka: o contrato de comunicação
 * entre os serviços, especificado em {@code docs/EVENTOS.md}.
 *
 * <p>Os campos de controle ficam fora do {@code payload} para que qualquer consumidor
 * saiba deduplicar, ordenar e correlacionar sem conhecer o evento: {@code eventId}
 * identifica a mensagem, {@code reservaId} é a chave de partição, {@code sequence}
 * numera os eventos de uma mesma reserva e {@code correlationId} liga a mensagem à
 * operação que a originou no API Gateway.
 *
 * <p>Versionamento: campo novo e opcional no payload não muda {@code eventVersion};
 * remover ou ressignificar campo exige nova versão, publicada em paralelo.
 */
public record EventoEnvelope(
        UUID eventId,
        String eventType,
        int eventVersion,
        UUID reservaId,
        long sequence,
        Instant occurredAt,
        String correlationId,
        String producer,
        JsonNode payload) {

    public EventoEnvelope {
        Objects.requireNonNull(eventId, "eventId é obrigatório");
        Objects.requireNonNull(eventType, "eventType é obrigatório");
        Objects.requireNonNull(reservaId, "reservaId é obrigatório");
        Objects.requireNonNull(occurredAt, "occurredAt é obrigatório");
        Objects.requireNonNull(correlationId, "correlationId é obrigatório");
        Objects.requireNonNull(producer, "producer é obrigatório");
        Objects.requireNonNull(payload, "payload é obrigatório");
        if (eventVersion < 1) throw new IllegalArgumentException("eventVersion começa em 1");
        if (sequence < 1) throw new IllegalArgumentException("sequence começa em 1");
    }

    /** A chave de publicação: todos os eventos da mesma reserva caem na mesma partição. */
    public String chave() {
        return reservaId.toString();
    }
}
