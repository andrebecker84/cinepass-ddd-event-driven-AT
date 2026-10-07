package br.com.cinepass.mensageria;

/**
 * Cabeçalhos que acompanham cada registro no Kafka. Repetem campos do envelope para que
 * ferramentas (Kafka UI, roteadores, inspeção da DLT) leiam o essencial sem abrir o JSON.
 * O contexto de tracing ({@code traceparent}) é injetado pelo Micrometer, não por aqui.
 */
public final class CabecalhosKafka {

    public static final String EVENT_ID = "eventId";
    public static final String EVENT_TYPE = "eventType";
    public static final String RESERVA_ID = "reservaId";
    public static final String CORRELATION_ID = "correlationId";

    private CabecalhosKafka() {
    }
}
